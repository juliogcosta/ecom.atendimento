# Assistência Atendimento Service

Microserviço para gerenciar atendimentos de assistência veicular usando **CQRS/Event Sourcing**.

## Visão Geral

Este microserviço implementa o padrão CQRS (Command Query Responsibility Segregation) com Event Sourcing para gerenciar o ciclo de vida completo de atendimentos de assistência a veículos, desde a solicitação até a finalização, com integração seletiva ao sistema financeiro.

### Características Principais

- ✅ **Event Sourcing completo** - Estado derivado de eventos imutáveis
- ✅ **CQRS Pattern** - Separação de comandos e consultas
- ✅ **Máquina de Estados** - Validação rigorosa de transições
- ✅ **Integração Seletiva** - Apenas eventos `FINALIZADO` → Financeiro
- ✅ **Snapshots** - Otimização de performance (a cada 100 eventos)
- ✅ **Transactional Outbox** - Garantia de entrega de eventos
- ✅ **PostgreSQL LISTEN/NOTIFY** - Processamento assíncrono

### Novos Recursos (vs Implementação de Referência)

- 🆕 **Comando `ocorrencia`** - Registrar incidentes durante execução
- 🆕 **Estado `OCORRIDO`** - Novo estado na máquina de estados
- 🆕 **Campo `protocolo`** - Identificador de negócio
- 🆕 **Tracking de ocorrências** - Lista acumulativa de incidentes

---

## Arquitetura

### Máquina de Estados

```
null → SOLICITADO → AJUSTADO → CONFIRMADO → OCORRIDO → FINALIZADO
         ↓             ↓
     CANCELADO    CANCELADO
```

### Comandos Disponíveis

| Comando | From State | To State | Múltiplo | Descrição |
|---------|-----------|----------|----------|-----------|
| `solicitar` | null | SOLICITADO | ❌ | Cria novo atendimento |
| `ajustar` | SOLICITADO, AJUSTADO | AJUSTADO | ✅ | Ajusta prestador/destino/itens |
| `confirmar` | SOLICITADO, AJUSTADO | CONFIRMADO | ❌ | Confirma atendimento |
| `ocorrencia` | CONFIRMADO, OCORRIDO | OCORRIDO | ✅ | Registra ocorrências |
| `finalizar` | CONFIRMADO, OCORRIDO | FINALIZADO | ❌ | Finaliza (aciona integração) |
| `cancelar` | SOLICITADO, AJUSTADO | CANCELADO | ❌ | Cancela atendimento |

### Estrutura do Projeto

```
ecom.atendimento/
├── src/main/java/com/ecom/atendimento/
│   ├── domain/                    # Camada de Domínio (DDD)
│   │   ├── aggregate/             # Agregados
│   │   │   ├── AtendimentoAggregate.java    # Lógica de negócio
│   │   │   ├── Atendimento.java             # Estado do agregado
│   │   │   └── AtendimentoCommandType.java  # Enum de comandos
│   │   ├── command/               # Comandos (6 total)
│   │   ├── event/                 # Eventos (6 total)
│   │   └── valueobject/           # Value Objects (8 total)
│   ├── application/               # Camada de Aplicação
│   │   └── handler/               # Event Handlers
│   │       └── FinanceiroIntegrationEventHandler.java
│   ├── infrastructure/            # Configurações
│   │   └── config/
│   │       ├── AggregateType.java
│   │       ├── DefaultAggregateTypeMapper.java
│   │       └── RabbitMQConfig.java
│   └── adapter/                   # Adaptadores
│       ├── rest/                  # Controllers REST
│       │   └── AtendimentoController.java
│       └── dto/                   # DTOs de Request
└── src/main/resources/
    ├── application.yml
    └── db/migration/              # Flyway migrations
        ├── V0__create_schema_assistencia_es.sql
        ├── V1__eventsourcing_tables.sql
        └── V2__notify_trigger.sql
```

---

## Pré-requisitos

- **Java 17** ou superior
- **Maven 3.8+**
- **PostgreSQL 12+**
- **RabbitMQ 3.8+**
- **core-cqrs** e **core-common** libs instaladas localmente

---

## Configuração

### 1. Instalar Dependências Core

As dependências `ecom.core.cqrs` e `ecom.core.common` devem estar instaladas no repositório Maven local.

```bash
# Se os projetos core estiverem em diretórios irmãos:
cd ../ecom.core.cqrs && mvn clean install -DskipTests
cd ../ecom.core.common && mvn clean install -DskipTests
cd ../ecom.atendimento
```

### 2. Configurar PostgreSQL

Crie o banco de dados:

```sql
CREATE DATABASE assistencia_db;
```

As migrations Flyway criarão automaticamente:
- Schema `assistencia_es`
- Tabelas: `ES_AGGREGATE`, `ES_EVENT`, `ES_AGGREGATE_SNAPSHOT`, `ES_EVENT_SUBSCRIPTION`
- Trigger: `CHANNEL_EVENT_NOTIFY_TRG`

### 3. Configurar RabbitMQ

Certifique-se de que o RabbitMQ está rodando:

```bash
sudo systemctl start rabbitmq-server
```

O microserviço criará automaticamente:
- Exchange: `financeiro.exchange`
- Queue: `atendimento.finalizado.queue`
- Binding com routing key: `atendimento.finalizado`

### 4. Configurar application.yml

Edite `src/main/resources/application.yml` se necessário:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/assistencia_db
    username: postgres
    password: postgres

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

---

## Executar

### Build

```bash
mvn clean package -DskipTests
```

### Run

```bash
mvn spring-boot:run
```

Ou execute o JAR:

```bash
java -jar target/ecom.atendimento-0.0.1.jar
```

### Verificar

```bash
curl http://localhost:8080/actuator/health
```

Resposta esperada:
```json
{"status":"UP"}
```

---

## Uso da API

Base URL: `http://localhost:8080/api/atendimento`

### 1. Solicitar Atendimento

**POST** `/api/atendimento/solicitar`

```bash
curl -X POST http://localhost:8080/api/atendimento/solicitar \
  -H "Content-Type: application/json" \
  -d '{
    "protocolo": "ATD-2025-000001",
    "tipodeocorrencia": "Pane elétrica",
    "cliente": {
      "id": 123,
      "nome": "João Silva",
      "docfiscal": {
        "tipo": "CPF",
        "numero": "12345678900"
      }
    },
    "veiculo": {
      "placa": "ABC1234"
    },
    "servico": {
      "id": 1,
      "nome": "Reboque"
    },
    "base": {
      "tipo": "RESIDENCIAL",
      "logradouro": "Rua A",
      "numero": "10",
      "bairro": "Centro",
      "cidade": "São Paulo",
      "estado": "SP",
      "cep": "01310100"
    },
    "origem": {
      "tipo": "COMERCIAL",
      "logradouro": "Av B",
      "numero": "200",
      "bairro": "Jardins",
      "cidade": "São Paulo",
      "estado": "SP",
      "cep": "01310200"
    }
  }'
```

Resposta:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 2. Ajustar Atendimento

**PUT** `/api/atendimento/ajustar`

```bash
curl -X PUT http://localhost:8080/api/atendimento/ajustar \
  -H "Content-Type: application/json" \
  -d '{
    "aggregateId": "550e8400-e29b-41d4-a716-446655440000",
    "descricao": "Definindo prestador e itens de cobrança",
    "prestador": {
      "id": 456,
      "nome": "Prestadora XYZ Ltda",
      "docfiscal": {
        "tipo": "CNPJ",
        "numero": "12345678000190"
      }
    },
    "destino": {
      "tipo": "COMERCIAL",
      "logradouro": "Rua C",
      "numero": "50",
      "bairro": "Mooca",
      "cidade": "São Paulo",
      "estado": "SP",
      "cep": "03180000"
    },
    "items": [
      {
        "nome": "Reboque",
        "unidadedemedida": "km",
        "precounitario": 500,
        "quantidade": 15,
        "observacao": "15km percorridos"
      },
      {
        "nome": "Taxa de saída",
        "unidadedemedida": "unidade",
        "precounitario": 5000,
        "quantidade": 1
      }
    ]
  }'
```

### 3. Confirmar Atendimento

**PUT** `/api/atendimento/confirmar`

```bash
curl -X PUT http://localhost:8080/api/atendimento/confirmar \
  -H "Content-Type: application/json" \
  -d '{
    "aggregateId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

### 4. Registrar Ocorrência (NOVO)

**PUT** `/api/atendimento/ocorrencia`

```bash
curl -X PUT http://localhost:8080/api/atendimento/ocorrencia \
  -H "Content-Type: application/json" \
  -d '{
    "aggregateId": "550e8400-e29b-41d4-a716-446655440000",
    "ocorrencias": [
      "Cliente não estava no local, aguardado 15 minutos",
      "Veículo mais pesado que informado, necessário reboque maior"
    ]
  }'
```

### 5. Finalizar Atendimento

**PUT** `/api/atendimento/finalizar`

```bash
curl -X PUT http://localhost:8080/api/atendimento/finalizar \
  -H "Content-Type: application/json" \
  -d '{
    "aggregateId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

⚠️ **IMPORTANTE**: Este comando aciona integração com o sistema Financeiro via RabbitMQ.

### 6. Cancelar Atendimento

**PUT** `/api/atendimento/cancelar`

```bash
curl -X PUT http://localhost:8080/api/atendimento/cancelar \
  -H "Content-Type: application/json" \
  -d '{
    "aggregateId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

---

## Integração com Financeiro

### Evento de Integração

Apenas eventos `FINALIZADO` disparam integração. Payload enviado para RabbitMQ:

```json
{
  "aggregateId": "550e8400-e29b-41d4-a716-446655440000",
  "protocolo": "ATD-2025-000001",
  "status": "FINALIZADO",
  "version": 5,
  "cliente": {
    "id": 123,
    "nome": "João Silva"
  },
  "prestador": {
    "id": 456,
    "nome": "Prestadora XYZ Ltda"
  },
  "items": [
    {
      "nome": "Reboque",
      "quantidade": 15,
      "precounitario": 500,
      "valortotal": 7500
    },
    {
      "nome": "Taxa de saída",
      "quantidade": 1,
      "precounitario": 5000,
      "valortotal": 5000
    }
  ],
  "dataHoraSolicitado": "2025-03-10T10:00:00.000Z",
  "dataHoraFinalizado": "2025-03-10T14:30:00.000Z",
  "ocorrencias": [
    "Cliente não estava no local, aguardado 15 minutos"
  ],
  "eventType": "ATENDIMENTO_FINALIZADO",
  "timestamp": 1710079800000
}
```

### Configuração RabbitMQ

- **Exchange**: `financeiro.exchange`
- **Queue**: `atendimento.finalizado.queue`
- **Routing Key**: `atendimento.finalizado`

---

## Banco de Dados

### Event Store Schema

O microserviço usa 4 tabelas principais:

1. **ES_AGGREGATE** - Registro dos agregados
2. **ES_EVENT** - Event store (append-only)
3. **ES_AGGREGATE_SNAPSHOT** - Snapshots para performance
4. **ES_EVENT_SUBSCRIPTION** - Checkpoint do Transactional Outbox

### Consultas Úteis

Ver eventos de um atendimento:

```sql
SELECT version, event_type, created_date, json_data
FROM assistencia_es.ES_EVENT
WHERE aggregate_id = '550e8400-e29b-41d4-a716-446655440000'
ORDER BY version;
```

Ver eventos mais recentes:

```sql
SELECT id, aggregate_id, version, event_type, created_date
FROM assistencia_es.ES_EVENT
ORDER BY id DESC
LIMIT 10;
```

Contar eventos por tipo:

```sql
SELECT event_type, COUNT(*) as total
FROM assistencia_es.ES_EVENT
GROUP BY event_type
ORDER BY total DESC;
```

---

## Monitoramento

### Endpoints Actuator

- **Health**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/metrics`
- **Prometheus**: `http://localhost:8080/actuator/prometheus`

### Logs

Logs são controlados em `application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.ecom: DEBUG
    org.hibernate.SQL: DEBUG
```

---

## Diferenças vs Implementação de Referência

| Feature | Referência | Este Projeto | Status |
|---------|------------|--------------|--------|
| Comando `ocorrencia` | ❌ Ausente | ✅ Implementado | 🆕 NOVO |
| Estado `OCORRIDO` | ❌ Ausente | ✅ Implementado | 🆕 NOVO |
| Campo `protocolo` | ❌ Ausente | ✅ Implementado | 🆕 NOVO |
| Integração seletiva | ⚠️ Genérica | ✅ Apenas FINALIZADO | ✅ MELHORADO |
| Tracking de ocorrências | ❌ Ausente | ✅ Lista acumulativa | 🆕 NOVO |

---

## Desenvolvimento

### Executar Testes

```bash
mvn test
```

### Build Docker

```bash
mvn compile jib:dockerBuild
```

### Code Style

O projeto segue:
- Clean Architecture (DDD)
- CQRS/Event Sourcing Pattern
- Value Objects imutáveis
- Command/Event separation

---

## Troubleshooting

### Erro: "Comando não definido"

Verifique se o `AtendimentoCommandType` inclui todos os comandos.

### Erro: "Não é possível X com status Y"

Verifique a máquina de estados. Algumas transições não são permitidas.

### Eventos não chegam no RabbitMQ

1. Verifique se RabbitMQ está rodando: `sudo systemctl status rabbitmq-server`
2. Verifique logs: `tail -f /var/log/rabbitmq/rabbit@hostname.log`
3. Verifique exchange/queue foram criados: `rabbitmqctl list_exchanges`, `rabbitmqctl list_queues`

### Erro ao conectar no PostgreSQL

Verifique:
1. PostgreSQL rodando: `sudo systemctl status postgresql`
2. Credenciais em `application.yml`
3. Database criada: `psql -l | grep assistencia_db`

---

## Documentação Adicional

- **Especificação**: `./domain.aggregates.spec`
- **Documentação do Projeto**: `./docs/SpecROOT.md`
- **Arquitetura CQRS/ES**: `./docs/01-architecture/event-sourcing.md`
- **Database Schema**: `./docs/01-architecture/database-schema.md`
- **Comandos Detalhados**: `./docs/02-specification/commands.md`
- **Diferenças vs Referência**: `./docs/04-reference/differences.md`

---

## Licença

Propriedade de Comigo. Todos os direitos reservados.

---

## Contato

Para questões sobre este microserviço, consulte a documentação técnica em `./docs/` ou entre em contato com a equipe de desenvolvimento.
