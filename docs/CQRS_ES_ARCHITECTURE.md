# Arquitetura CQRS/ES - Documentação de Aprendizado

## 1. Visão Geral da Arquitetura

Este documento consolida o aprendizado obtido da análise da implementação CQRS/ES existente no projeto `/home/julio/Codes/YC/Experiments/comigo/`, especificamente os módulos:
- **core-cqrs**: Framework genérico CQRS/ES
- **assistencia.atendimento**: Implementação concreta do agregado Atendimento

### 1.1 Princípios Fundamentais

**CQRS (Command Query Responsibility Segregation)**
- Separação entre operações de escrita (Commands) e leitura (Queries)
- Commands modificam estado → geram Events
- Queries lêem de Projections (modelos de leitura otimizados)

**Event Sourcing**
- Estado do agregado NÃO é persistido diretamente
- Apenas EVENTOS são persistidos (Event Store)
- Estado atual é reconstruído replay de eventos
- Possibilita auditoria completa e time-travel

---

## 2. Módulo Core-CQRS: Framework Genérico

Localização: `/home/julio/Codes/YC/Experiments/comigo/core-cqrs/`

### 2.1 Componentes Principais do Domain Layer

#### 2.1.1 Aggregate (classe base abstrata)
**Arquivo**: `br/com/comigo/core/domain/Aggregate.java`

**Responsabilidades**:
- Classe base para todos os agregados
- Gerencia versão do agregado (otimistic locking)
- Mantém lista de eventos ocorridos (`occurredEvents`)
- Aplica eventos para mudar estado

**Métodos principais**:
```java
// Processa comando (invoca método process(Command) específico via reflexão)
public void process(Command command)

// Aplica evento e adiciona à lista de eventos ocorridos
protected void applyChange(Event event)

// Reconstrói estado a partir do histórico de eventos
public void loadFromHistory(List<Event> events)

// Versão atual + 1
protected int getNextVersion()
```

**Atributos importantes**:
- `aggregateId`: UUID único do agregado
- `version`: versão atual do agregado
- `baseVersion`: versão base (antes de novos eventos)
- `occurredEvents`: lista de eventos pendentes de persistência

#### 2.1.2 Command (classe base)
**Arquivo**: `br/com/comigo/core/domain/command/Command.java`

**Responsabilidades**:
- Representa intenção de mudar estado
- Contém dados necessários para a operação

**Atributos**:
```java
protected final String aggregateType;
protected final UUID aggregateId;
protected final Class<? extends Command> commandType;
```

#### 2.1.3 Event (classe base abstrata)
**Arquivo**: `br/com/comigo/core/domain/event/Event.java`

**Responsabilidades**:
- Representa fato que ocorreu (imutável)
- Contém dados do que mudou

**Atributos**:
```java
protected final UUID aggregateId;
protected final int version;
protected final Timestamp createdDate;

public abstract String getEventType();
```

### 2.2 Camada de Aplicação

#### 2.2.1 AggregateStore
**Arquivo**: `br/com/comigo/core/application/service/AggregateStore.java`

**Responsabilidades**:
- Persistir agregados e eventos
- Recuperar agregados (via eventos ou snapshots)
- Gerenciar snapshots

**Métodos principais**:
```java
// Salva agregado e retorna eventos persistidos
List<EventWithId<Event>> saveAggregate(String schemaName, Aggregate aggregate)

// Lê agregado reconstruindo via eventos
Aggregate readAggregate(String schemaName, String aggregateType, UUID aggregateId)
```

**Fluxo de saveAggregate**:
1. Cria registro em `ES_AGGREGATE` (se não existir)
2. Verifica versão (optimistic locking) → lança `OptimisticConcurrencyControlException` se divergir
3. Persiste eventos em `ES_EVENT`
4. Cria snapshot se configurado

**Fluxo de readAggregate**:
1. Tenta ler snapshot (se habilitado)
2. Se snapshot existe: carrega eventos APÓS o snapshot
3. Se não: carrega TODOS os eventos
4. Cria instância do agregado via `AggregateFactory`
5. Reconstrói estado via `loadFromHistory(events)`

#### 2.2.2 CommandProcessor
**Arquivo**: `br/com/comigo/core/application/service/command/CommandProcessor.java`

**Responsabilidades**:
- Processamento genérico de comandos
- Coordena: leitura de agregado → processamento de comando → persistência → disparo de eventos

**Fluxo de process(Command)**:
```
1. Recebe Command
2. Lê agregado atual via AggregateStore (reconstrói estado)
3. Procura CommandHandler específico (se não achar, usa DefaultCommandHandler)
4. Executa handler → invoca aggregate.process(command)
5. aggregate.process(command) → gera eventos via applyChange()
6. Salva agregado e eventos via AggregateStore
7. Dispara event handlers síncronos (SyncEventHandler)
8. Retorna agregado atualizado
```

**Código simplificado**:
```java
public Aggregate process(Command command) {
    // 1. Lê agregado (reconstrói via eventos)
    Aggregate aggregate = aggregateStore.readAggregate(schemaName, aggregateType, aggregateId);

    // 2. Processa comando (gera eventos)
    commandHandlers.stream()
        .filter(h -> h.getCommandType() == command.getClass())
        .findFirst()
        .ifPresentOrElse(
            h -> h.handle(aggregate, command),
            () -> defaultCommandHandler.handle(aggregate, command)
        );

    // 3. Persiste agregado e eventos
    List<EventWithId<Event>> events = aggregateStore.saveAggregate(schemaName, aggregate);

    // 4. Dispara event handlers
    aggregateChangesHandlers.stream()
        .filter(h -> h.getAggregateType().equals(aggregateType))
        .forEach(h -> h.handleEvents(events, aggregate));

    return aggregate;
}
```

### 2.3 Camada de Infraestrutura - Repositórios

#### 2.3.1 EventRepository
**Arquivo**: `br/com/comigo/core/adapter/outbound/repository/EventRepository.java`

**Responsabilidades**:
- Persistir eventos no PostgreSQL
- Recuperar eventos por agregado
- Suporte a Transactional Outbox Pattern

**Métodos principais**:
```java
// Adiciona evento à tabela ES_EVENT
EventWithId<T> appendEvent(String schemaName, Event event)

// Lê eventos de um agregado (com range de versões)
List<EventWithId<Event>> readEvents(String schemaName, UUID aggregateId,
                                     Integer fromVersion, Integer toVersion)

// Lê eventos após checkpoint (para outbox pattern)
List<EventWithId<Event>> readEventsAfterCheckpoint(String schemaName,
                                                     String aggregateType,
                                                     BigInteger lastProcessedTransactionId,
                                                     long lastProcessedEventId,
                                                     int batchSize)
```

**Detalhes importantes**:
- Usa `NamedParameterJdbcTemplate` do Spring
- Eventos armazenados como JSON (coluna `JSON_DATA`)
- Usa `ObjectMapper` para serialização/deserialização
- Usa `EventTypeMapper` para mapear eventType → Class

#### 2.3.2 AggregateRepository
**Arquivo**: `br/com/comigo/core/adapter/outbound/repository/AggregateRepository.java`

**Responsabilidades**:
- Gerenciar tabela `ES_AGGREGATE`
- Controle de versão (optimistic locking)
- Gerenciar snapshots

**Métodos principais** (inferidos):
```java
void createAggregateIfAbsent(String schemaName, String aggregateType, UUID aggregateId)

boolean checkAndUpdateAggregateVersion(String schemaName, UUID aggregateId,
                                        int expectedVersion, int newVersion)

void createAggregateSnapshot(String schemaName, Aggregate aggregate)

Optional<Aggregate> readAggregateSnapshot(String schemaName, UUID aggregateId,
                                           Integer version)
```

### 2.4 Event Handlers

#### 2.4.1 SyncEventHandler (interface)
**Arquivo**: `br/com/comigo/core/application/service/event/handler/SyncEventHandler.java`

- Executado **síncronamente** após persistência de eventos
- Mesmo contexto transacional
- Usado para: atualizar projeções, enviar eventos de integração

#### 2.4.2 AsyncEventHandler (interface)
**Arquivo**: `br/com/comigo/core/application/service/event/handler/AsyncEventHandler.java`

- Executado **assincronamente** via Transactional Outbox
- Fora do contexto transacional original
- Maior resiliência, mas eventual consistency

---

## 3. Implementação do Agregado Atendimento

Localização: `/home/julio/Codes/YC/Experiments/comigo/assistencia.atendimento/`

### 3.1 Estrutura de Pacotes

```
br.com.comigo.assistencia
├── domain
│   ├── aggregate
│   │   ├── atendimento
│   │   │   ├── AtendimentoAggregate.java          # Agregado
│   │   │   ├── AtendimentoCommandType.java        # Enum de tipos de comando
│   │   │   ├── command/                           # Comandos
│   │   │   │   ├── SolicitarCommand.java
│   │   │   │   ├── AjustarCommand.java
│   │   │   │   ├── ConfirmarCommand.java
│   │   │   │   ├── FinalizarCommand.java
│   │   │   │   └── CancelarCommand.java
│   │   │   ├── event/                             # Eventos
│   │   │   │   ├── SolicitadoEvent.java
│   │   │   │   ├── AjustadoEvent.java
│   │   │   │   ├── ConfirmadoEvent.java
│   │   │   │   ├── FinalizadoEvent.java
│   │   │   │   └── CanceladoEvent.java
│   │   │   └── concept/                           # Value Objects
│   │   │       ├── Atendimento.java               # Entidade principal
│   │   │       ├── ClienteRef.java
│   │   │       ├── PrestadorRef.java
│   │   │       ├── VeiculodeclienteRef.java
│   │   │       ├── ServicoRef.java
│   │   │       ├── Endereco.java
│   │   │       ├── DocFiscal.java
│   │   │       └── Itemdeatendimento.java
│   ├── projection
│   │   ├── AtendimentoProjection.java             # Modelo de leitura
│   │   ├── ClienteProjectionRef.java
│   │   ├── EnderecoProjection.java
│   │   └── repository/
│   │       └── AtendimentoProjectionRepository.java
│   └── service/
├── application
│   ├── service
│   │   ├── aggregate/atendimento
│   │   │   ├── command/                           # Command Handlers
│   │   │   │   ├── SolicitarCommandHandlerImpl.java
│   │   │   │   ├── AjustarCommandHandlerImpl.java
│   │   │   │   └── ConfirmarCommandHandlerImpl.java
│   │   │   └── event/                             # Event Handlers
│   │   │       └── AtendimentoIntegrationEventSender.java
│   │   └── projection/
│   │       └── AtendimentoProjectionServiceImpl.java
│   └── usecase
│       ├── aggregate/atendimento/
│       └── projection/
├── infrastructure
│   ├── config/                                     # Configurações Spring
│   └── rabbitmq/                                   # Configurações RabbitMQ
└── adapter
    ├── aggregate/atendimento
    │   ├── inbound/web/
    │   │   └── AtendimentoController.java         # REST Controller (Commands)
    │   └── dto/
    │       ├── AtendimentoDTO.java
    │       └── ItemdeatendimentoDTO.java
    └── projection
        ├── inbound/web/
        │   └── AtendimentoProjectionController.java # REST Controller (Queries)
        └── dto/
```

### 3.2 AtendimentoAggregate - Análise Detalhada

**Arquivo**: `br/com/comigo/assistencia/domain/aggregate/atendimento/AtendimentoAggregate.java`

**Estrutura**:
```java
public class AtendimentoAggregate extends Aggregate {

    // Estado do agregado (reconstruído via eventos)
    private Atendimento atendimento = new Atendimento();

    // Construtor: define comandos aceitos
    public AtendimentoAggregate(UUID aggregateId, int version) {
        super(aggregateId, version);
        super.defCommands(AtendimentoCommandType.values());
    }

    // Métodos process() - um para cada comando
    public void process(SolicitarCommand command) { ... }
    public void process(AjustarCommand command) { ... }
    public void process(ConfirmarCommand command) { ... }
    public void process(FinalizarCommand command) { ... }
    public void process(CancelarCommand command) { ... }

    // Métodos apply() - um para cada evento
    public void apply(SolicitadoEvent event) { ... }
    public void apply(AjustadoEvent event) { ... }
    public void apply(ConfirmadoEvent event) { ... }
    public void apply(FinalizadoEvent event) { ... }
    public void apply(CanceladoEvent event) { ... }

    @Override
    public String getAggregateType() {
        return AggregateType.COMIGO_ATENDIMENTO.toString();
    }
}
```

**Padrão de implementação**:

```java
// COMANDO: Valida estado atual e dispara evento
public void process(SolicitarCommand command) {
    // 1. Valida que comando está definido
    if (!super.isCommandDef(command)) {
        throw new RuntimeException("Comando não definido no design do agregado");
    }

    // 2. Valida regras de negócio (estado atual)
    if (this.atendimento.getStatus() == null) {
        // OK - pode solicitar
    } else {
        throw new AggregateStateException("O status %s não pode ser ajustado",
                                           this.atendimento.getStatus());
    }

    // 3. Dispara evento (applyChange)
    super.applyChange(SolicitadoEvent.builder()
        .aggregateid(super.aggregateId)
        .version(this.getNextVersion())
        .cliente(command.getCliente())
        .veiculo(command.getVeiculo())
        // ... outros atributos
        .build());
}

// EVENTO: Atualiza estado interno
public void apply(SolicitadoEvent event) {
    this.atendimento.setId(event.getAggregateId());
    this.atendimento.setStatus(Atendimento.Status.SOLICITADO);
    this.atendimento.setDataHoraSolicitado(event.getCreatedDate());
    this.version = event.getVersion();

    // Copia dados do evento para o estado
    this.atendimento.setCliente(event.getCliente());
    this.atendimento.setVeiculo(event.getVeiculo());
    // ... outros atributos
}
```

### 3.3 Fluxo Completo: Da Requisição HTTP à Persistência

**1. Cliente HTTP envia POST /solicitar**
```json
{
  "cliente": {"id": 123, "nome": "João Silva", "docfiscal": {...}},
  "veiculo": {"placa": "ABC1234"},
  "servico": {"id": 1, "nome": "Reboque"},
  "tipodeocorrencia": "Pane elétrica",
  "base": {"tipo": "RESIDENCIAL", "logradouro": "Rua A", ...},
  "origem": {"tipo": "COMERCIAL", "logradouro": "Rua B", ...}
}
```

**2. AtendimentoController.solicitar()**
```java
@PostMapping("/solicitar")
public ResponseEntity<JsonNode> solicitar(@RequestBody AtendimentoDTO dto) {
    Aggregate instance = this.commandProcessor.process(new SolicitarCommand(dto));
    return ResponseEntity.ok()
        .body(objectMapper.createObjectNode()
              .put("id", instance.getAggregateId().toString()));
}
```

**3. CommandProcessor.process()**
- Lê agregado via `aggregateStore.readAggregate()`
  - Como é novo: cria instância vazia (versão 0)
- Invoca `commandHandler.handle(aggregate, command)` ou default handler
- Handler invoca `aggregate.process(command)` → método descoberto via reflexão

**4. AtendimentoAggregate.process(SolicitarCommand)**
- Valida que comando está definido
- Valida regras de negócio (status deve ser null)
- Dispara `applyChange(SolicitadoEvent)`:
  - Invoca `apply(SolicitadoEvent)` → atualiza estado interno
  - Adiciona evento à lista `occurredEvents`
  - Incrementa versão

**5. CommandProcessor continua**
- Chama `aggregateStore.saveAggregate(aggregate)`:
  - Insere/atualiza registro em `ES_AGGREGATE` (id, version, aggregate_type)
  - Insere evento em `ES_EVENT` (transaction_id, aggregate_id, version, event_type, json_data)
  - Retorna `EventWithId<Event>` com ID do evento persistido

**6. CommandProcessor dispara event handlers síncronos**
- `AtendimentoIntegrationEventSender.handleEvent()`:
  - Reconstrói agregado na versão do evento
  - Cria DTO com estado completo
  - Envia para RabbitMQ (exchange + routing key)

**7. Controller retorna UUID do agregado criado**

### 3.4 Comandos e Transições de Estado

```
Status: null → SOLICITADO → AJUSTADO → CONFIRMADO → FINALIZADO
                    ↓            ↓
                CANCELADO    CANCELADO

Comando: solicitar
  - fromState: null
  - endState: SOLICITADO
  - Dados: cliente, veiculo, servico, tipodeocorrencia, base, origem

Comando: ajustar
  - fromState: [SOLICITADO, AJUSTADO]
  - endState: AJUSTADO
  - Dados: prestador, destino, items[], descricao
  - Pode ser executado múltiplas vezes

Comando: confirmar
  - fromState: [SOLICITADO, AJUSTADO]
  - endState: CONFIRMADO
  - Dados: apenas status

Comando: finalizar
  - fromState: CONFIRMADO
  - endState: FINALIZADO
  - Dados: apenas status

Comando: cancelar
  - fromState: [SOLICITADO, AJUSTADO]
  - endState: CANCELADO
  - Dados: apenas status
```

### 3.5 Value Objects e Concepts

#### Atendimento.java (Entidade principal)
```java
public class Atendimento {
    public enum Status {
        SOLICITADO, AJUSTADO, CONFIRMADO, CANCELADO, FINALIZADO
    }

    private UUID id;
    private Status status;
    private Integer version;

    private Timestamp dataHoraSolicitado;
    private Timestamp dataHoraConfirmado;
    private Timestamp dataHoraAjustado;
    private Timestamp dataHoraFinalizado;
    private Timestamp dataHoraCancelado;

    private String tipodeocorrencia;
    private String descricao;

    private Endereco base;
    private Endereco origem;
    private Endereco destino;

    private PrestadorRef prestador;
    private ClienteRef cliente;
    private VeiculodeclienteRef veiculo;
    private ServicoRef servico;

    private List<Itemdeatendimento> items;
}
```

#### Endereco.java (Value Object usando record)
```java
public record Endereco(
    TipoDeEndereco tipo,
    String logradouro,
    String numero,
    String complemento,
    String bairro,
    String cidade,
    String estado,
    String cep
) {
    public Endereco {
        if (!isValidCep(cep)) {
            throw new IllegalArgumentException("CEP invalido: " + cep);
        }
    }

    private static boolean isValidCep(String cep) {
        cep = cep.replaceAll("[^0-9]", "");
        return cep != null && cep.matches("\\d{8}");
    }
}
```

#### ClienteRef.java (Referência a outro agregado)
```java
public record ClienteRef (Long id, String nome, DocFiscal docfiscal) {}
```

**Padrão identificado**: Value objects com "Ref" no nome representam referências a outros agregados, contendo apenas ID e dados básicos (não toda a entidade).

### 3.6 Event Handlers - Integração

#### AtendimentoIntegrationEventSender (AsyncEventHandler)
**Arquivo**: `br/com/comigo/assistencia/application/service/aggregate/atendimento/event/AtendimentoIntegrationEventSender.java`

**Responsabilidade**: Enviar eventos de integração para outros bounded contexts via RabbitMQ

**Fluxo**:
```java
@Override
public void handleEvent(EventWithId<Event> eventWithId) {
    Event event = eventWithId.event();

    // 1. Reconstrói agregado na versão do evento
    Aggregate aggregate = aggregateStore.readAggregate(
        schemaName,
        AggregateType.COMIGO_ATENDIMENTO.toString(),
        event.getAggregateId(),
        event.getVersion()
    );

    // 2. Converte para DTO
    AtendimentoDTO dto = buildDTO(aggregate);

    // 3. Envia para RabbitMQ
    rabbitTemplate.convertAndSend(exchange, routingKey, mapper.writeValueAsString(dto));
}
```

**Configuração (application.yml)**:
```yaml
spring:
  rabbitmq:
    assistencia:
      atendimento:
        exchange: "assistencia.atendimento.exchange"
        events:
          view-update:
            routing-key: "atendimento.view.update"
```

---

## 4. Estrutura de Banco de Dados (Event Sourcing)

### 4.1 Schema

Schema: `ASSISTENCIA_ES` (definido em migration `V0__create_schema_assistencia_es.sql`)

### 4.2 Tabelas Principais

**Arquivo**: `V1__eventsourcing_tables.sql`

#### ES_AGGREGATE
```sql
CREATE TABLE IF NOT EXISTS ASSISTENCIA_ES.ES_AGGREGATE (
  ID              UUID     PRIMARY KEY,          -- UUID do agregado
  VERSION         INTEGER  NOT NULL,             -- Versão atual
  AGGREGATE_TYPE  TEXT     NOT NULL              -- Tipo (ex: COMIGO_ATENDIMENTO)
);

CREATE INDEX IDX_ES_AGGREGATE_AGGREGATE_TYPE ON ASSISTENCIA_ES.ES_AGGREGATE (AGGREGATE_TYPE);
```

**Propósito**:
- Registro "cabeçalho" do agregado
- Controle de versão para optimistic locking
- Índice para consultas por tipo

#### ES_EVENT
```sql
CREATE TABLE IF NOT EXISTS ASSISTENCIA_ES.ES_EVENT (
  ID              BIGSERIAL  PRIMARY KEY,         -- ID sequencial do evento
  TRANSACTION_ID  XID8       NOT NULL,            -- ID da transação PostgreSQL
  AGGREGATE_ID    UUID       NOT NULL REFERENCES ASSISTENCIA_ES.ES_AGGREGATE (ID),
  VERSION         INTEGER    NOT NULL,            -- Versão do agregado neste evento
  EVENT_TYPE      TEXT       NOT NULL,            -- Tipo do evento (ex: SOLICITADO)
  JSON_DATA       JSON       NOT NULL,            -- Evento serializado
  UNIQUE (AGGREGATE_ID, VERSION)                  -- Garante ordem e unicidade
);

CREATE INDEX IDX_ES_EVENT_TRANSACTION_ID_ID ON ASSISTENCIA_ES.ES_EVENT (TRANSACTION_ID, ID);
CREATE INDEX IDX_ES_EVENT_AGGREGATE_ID ON ASSISTENCIA_ES.ES_EVENT (AGGREGATE_ID);
CREATE INDEX IDX_ES_EVENT_VERSION ON ASSISTENCIA_ES.ES_EVENT (VERSION);
```

**Propósito**:
- Event Store: armazena TODOS os eventos
- JSON_DATA: evento serializado completo (permite replay)
- TRANSACTION_ID: suporte a Transactional Outbox
- Índices otimizados para leitura por agregado e processamento outbox

**Exemplo de JSON_DATA**:
```json
{
  "aggregateid": "550e8400-e29b-41d4-a716-446655440000",
  "version": 1,
  "createdDate": "2025-03-10T10:30:00.000Z",
  "status": "SOLICITADO",
  "cliente": {
    "id": 123,
    "nome": "João Silva",
    "docfiscal": {"tipo": "CPF", "numero": "12345678900"}
  },
  "veiculo": {"placa": "ABC1234"},
  "servico": {"id": 1, "nome": "Reboque"},
  "tipodeocorrencia": "Pane elétrica",
  "base": {"tipo": "RESIDENCIAL", "logradouro": "Rua A", ...},
  "origem": {"tipo": "COMERCIAL", "logradouro": "Rua B", ...}
}
```

#### ES_AGGREGATE_SNAPSHOT
```sql
CREATE TABLE IF NOT EXISTS ASSISTENCIA_ES.ES_AGGREGATE_SNAPSHOT (
  AGGREGATE_ID  UUID     NOT NULL REFERENCES ASSISTENCIA_ES.ES_AGGREGATE (ID),
  VERSION       INTEGER  NOT NULL,             -- Versão do snapshot
  JSON_DATA     JSON     NOT NULL,             -- Agregado serializado
  PRIMARY KEY (AGGREGATE_ID, VERSION)
);

CREATE INDEX IDX_ES_AGGREGATE_SNAPSHOT_AGGREGATE_ID ON ASSISTENCIA_ES.ES_AGGREGATE_SNAPSHOT (AGGREGATE_ID);
CREATE INDEX IDX_ES_AGGREGATE_SNAPSHOT_VERSION ON ASSISTENCIA_ES.ES_AGGREGATE_SNAPSHOT (VERSION);
```

**Propósito**:
- Otimização de leitura
- Em vez de replay de 1000 eventos, carrega snapshot na versão 990 + 10 eventos
- Configurável por agregado (a cada N eventos)

#### ES_EVENT_SUBSCRIPTION
```sql
CREATE TABLE IF NOT EXISTS ASSISTENCIA_ES.ES_EVENT_SUBSCRIPTION (
  SUBSCRIPTION_NAME    TEXT    PRIMARY KEY,      -- Nome da subscription
  LAST_TRANSACTION_ID  XID8    NOT NULL,         -- Última transação processada
  LAST_EVENT_ID        BIGINT  NOT NULL          -- Último evento processado
);
```

**Propósito**:
- Checkpoint para Transactional Outbox Pattern
- Garante processamento exactly-once de eventos
- Cada subscription (ex: projection-updater, integration-sender) mantém seu checkpoint

### 4.3 Trigger para Notificação (PostgreSQL LISTEN/NOTIFY)

**Arquivo**: `V2__notify_trigger.sql`

Cria trigger que notifica via PostgreSQL channel quando novos eventos são inseridos, permitindo processamento assíncrono em tempo real.

---

## 5. Padrões e Boas Práticas Identificados

### 5.1 Separação de Responsabilidades

**Domain Layer**: Lógica de negócio pura
- Agregados, Commands, Events, Value Objects
- Sem dependências de infraestrutura

**Application Layer**: Casos de uso e orquestração
- Command Handlers
- Event Handlers
- Serviços de aplicação

**Infrastructure Layer**: Detalhes técnicos
- Repositórios (JDBC)
- Configurações Spring
- RabbitMQ

**Adapter Layer**: Interfaces externas
- Controllers REST
- DTOs
- Mapeadores

### 5.2 Uso de Reflexão para Invocação Polimórfica

O framework usa reflexão para invocar métodos `process(Command)` e `apply(Event)` específicos:

```java
private void invoke(Object o, String methodName) {
    Method method = this.getClass().getMethod(methodName, o.getClass());
    method.invoke(this, o);
}
```

**Vantagem**: Não precisa de switch/case gigante. Basta adicionar métodos `process()` e `apply()` no agregado.

### 5.3 Optimistic Locking

Controle de concorrência via versão do agregado:
- Ao persistir, verifica se `expectedVersion` == versão atual no banco
- Se divergir → `OptimisticConcurrencyControlException`
- Cliente deve recarregar agregado e tentar novamente

### 5.4 Transactional Outbox Pattern

Evita perda de eventos em caso de falha:
1. Eventos são persistidos na MESMA transação que o agregado
2. Worker assíncrono lê eventos novos via `readEventsAfterCheckpoint()`
3. Processa eventos (ex: envia para RabbitMQ)
4. Atualiza checkpoint em `ES_EVENT_SUBSCRIPTION`

**Garantia**: Exactly-once delivery (com idempotência no consumidor)

### 5.5 Snapshots para Performance

- Configurável por tipo de agregado
- Salva snapshot a cada N eventos (ex: a cada 100)
- Ao ler: carrega snapshot + eventos posteriores (em vez de todos)

### 5.6 Imutabilidade

- Events são imutáveis (final fields)
- Value Objects usando `record` (Java 17+)
- Garante que histórico não pode ser alterado

### 5.7 Builder Pattern para Eventos

Uso de Lombok `@Builder` para construção fluente:
```java
SolicitadoEvent.builder()
    .aggregateid(aggregateId)
    .version(version)
    .cliente(cliente)
    .build();
```

### 5.8 Enum para Estados e Tipos

- `Atendimento.Status` (SOLICITADO, AJUSTADO, ...)
- `AtendimentoCommandType` (enum com CommandType)
- `EventType` (enum com strings de tipos de evento)

---

## 6. Configuração e Deploy

### 6.1 Dependências Principais (pom.xml)

```xml
<!-- Framework CQRS customizado -->
<dependency>
    <groupId>br.com.comigo</groupId>
    <artifactId>core-cqrs</artifactId>
</dependency>

<!-- Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- Hibernate Types para JSON -->
<dependency>
    <groupId>com.vladmihalcea</groupId>
    <artifactId>hibernate-types-60</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
</dependency>
```

### 6.2 Application.yml

```yaml
spring:
  application:
    name: assistencia-atendimento

  # Spring Cloud Config
  config:
    import: "configserver:${CONFIG_SERVER_URL}"

  # Datasource configurado via Config Server
  # RabbitMQ configurado via Config Server

# Schema do banco (usado pelo AggregateStore)
db:
  schema: assistencia_es

# Prometheus / Actuator
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics,threaddump
```

### 6.3 Build e Deploy

- **Build**: Maven com plugin Jib para gerar imagem Docker
- **Spring Cloud Config**: Configuração centralizada
- **Eureka**: Service Discovery
- **Kubernetes**: Deployment com HPA (Horizontal Pod Autoscaler)

---

## 7. Checklist para Implementar Novo Agregado

Baseado na análise, para implementar um novo agregado seguindo este padrão:

### 7.1 Domain Layer
- [ ] Criar enum de estados (`Status`)
- [ ] Criar Value Objects (usando `record` quando possível)
- [ ] Criar Commands (um para cada operação)
- [ ] Criar Events (um para cada mudança de estado)
- [ ] Criar Aggregate estendendo `br.com.comigo.core.domain.Aggregate`
  - [ ] Definir comandos aceitos no construtor
  - [ ] Implementar métodos `process(XCommand)` para cada comando
  - [ ] Implementar métodos `apply(XEvent)` para cada evento
  - [ ] Implementar `getAggregateType()`
- [ ] Criar enum `CommandType` e `EventType` com mapeamentos

### 7.2 Application Layer
- [ ] Criar Command Handlers (opcional, pode usar DefaultCommandHandler)
- [ ] Criar Event Handlers (Sync/Async)
  - [ ] Implementar `SyncEventHandler` para atualização de projeções
  - [ ] Implementar `AsyncEventHandler` para integração (RabbitMQ)
- [ ] Criar serviços de aplicação se necessário

### 7.3 Infrastructure Layer
- [ ] Criar migrations de banco de dados
  - [ ] `V0__create_schema_[nome].sql`
  - [ ] `V1__eventsourcing_tables.sql`
  - [ ] `V2__notify_trigger.sql`
- [ ] Configurar RabbitMQ (exchanges, queues, routing keys)
- [ ] Configurar propriedades de snapshotting

### 7.4 Adapter Layer
- [ ] Criar DTOs
- [ ] Criar Controller REST
  - [ ] Endpoints para Commands (POST, PUT)
  - [ ] Endpoint para Query (GET) se necessário
- [ ] Criar mapeadores (DTOs ↔ Commands/Aggregates)

### 7.5 Projection (Read Model)
- [ ] Criar entidade JPA para projeção
- [ ] Criar Repository Spring Data JPA
- [ ] Criar Controller de consulta
- [ ] Implementar handler que atualiza projeção a partir de eventos

### 7.6 Testes
- [ ] Testes unitários do agregado
- [ ] Testes de integração (comando → evento → projeção)
- [ ] Testes de API (REST)

---

## 8. Diferenças com a Especificação domain.aggregates.spec

A especificação em `./domain.aggregates.spec` define um agregado `assistencia.atendimento` que possui diferenças sutis com a implementação atual:

### 8.1 Comando "ocorrencia" (novo)
- **fromState**: [confirmado, ocorrido]
- **endState**: ocorrido
- **Data**: ocorrencias[] (array de textos)
- **Funcionalidade**: Permite registrar múltiplas ocorrências durante atendimento

A implementação atual NÃO possui este comando.

### 8.2 Fluxo de Estados Atualizado
```
[INÍCIO]
   ↓
solicitar → [solicitado]
   ↓                ↓
ajustar         confirmar
   ↓                ↓
[ajustado]      [confirmado]
   ↓                ↓
confirmar       ocorrencia → [ocorrido]
   ↓                ↓              ↓
[confirmado]    finalizar      finalizar
                   ↓              ↓
               [finalizado]   [finalizado]

Cancelamento:
[solicitado] ou [ajustado] → cancelar → [cancelado]
```

**Diferença**: Estado "OCORRIDO" não existe na implementação atual.

### 8.3 Eventos e Triggers

A spec define triggers de integração (domainBus):
- `solicitado` → projection
- `ajustado` → projection + suporte.atendimento (disabled)
- `confirmado` → projection + suporte.atendimento (disabled)
- `ocorrido` → projection
- `finalizado` → projection + suporte.atendimento (disabled) + **financeiro.atendimento (enabled)**
- `cancelado` → projection + suporte.atendimento (disabled)

**Diferença**: A implementação atual envia TODOS os eventos para RabbitMQ de forma genérica, não há filtro por tipo de evento ou destino específico.

---

## 9. Próximos Passos

1. **Mapear especificação completa** em documento separado
2. **Criar matriz de comparação**: spec vs implementação atual
3. **Implementar novo microservice** seguindo este padrão, mas aderente à spec
4. **Adicionar comando "ocorrencia"** e estado "OCORRIDO"
5. **Implementar filtros de integração** por tipo de evento

---

## 10. Referências

- Código-fonte: `/home/julio/Codes/YC/Experiments/comigo/`
- Especificação: `./domain.aggregates.spec`
- Padrões:
  - Event Sourcing: Martin Fowler
  - CQRS: Greg Young
  - DDD: Eric Evans
  - Transactional Outbox: Chris Richardson (Microservices Patterns)
