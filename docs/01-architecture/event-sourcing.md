# Event Sourcing - Conceitos e Implementação

## 1. O que é Event Sourcing?

Event Sourcing é um padrão arquitetural onde o **estado do sistema é derivado de uma sequência de eventos imutáveis**, ao invés de persistir o estado atual diretamente.

### Princípios Fundamentais

**1. Eventos como Source of Truth**
- Estado ATUAL não é persistido diretamente
- Apenas EVENTOS são persistidos (Event Store)
- Estado é reconstruído via replay de eventos

**2. Imutabilidade**
- Eventos nunca são alterados ou deletados
- Representam fatos que ocorreram no passado
- Auditoria completa e natural

**3. Temporal Queries**
- Possibilidade de consultar estado em qualquer momento do passado
- "Time travel" para debugging e auditoria

---

## 2. Componentes do Event Sourcing

### 2.1 Event Store

Banco de dados especializado para armazenar eventos.

**Tabela Principal**: `ES_EVENT`

```sql
CREATE TABLE ES_EVENT (
  ID              BIGSERIAL  PRIMARY KEY,
  TRANSACTION_ID  XID8       NOT NULL,
  AGGREGATE_ID    UUID       NOT NULL,
  VERSION         INTEGER    NOT NULL,
  EVENT_TYPE      TEXT       NOT NULL,
  JSON_DATA       JSON       NOT NULL,
  UNIQUE (AGGREGATE_ID, VERSION)
);
```

**Características**:
- Append-only (somente inserção, nunca update/delete)
- Eventos ordenados por VERSION
- JSON_DATA contém evento serializado completo

**Exemplo de evento persistido**:
```json
{
  "aggregateid": "550e8400-e29b-41d4-a716-446655440000",
  "version": 3,
  "createdDate": "2025-03-10T10:30:00.000Z",
  "status": "AJUSTADO",
  "prestador": {
    "id": 456,
    "nome": "Prestadora XYZ",
    "docfiscal": {"tipo": "CNPJ", "numero": "12345678000190"}
  },
  "destino": {...},
  "items": [...]
}
```

### 2.2 Aggregate

Entidade de domínio que:
- Valida comandos
- Gera eventos
- Reconstrói estado via replay

**Exemplo de reconstitução**:
```java
// Criar agregado vazio (versão 0)
Aggregate aggregate = new AtendimentoAggregate(aggregateId, 0);

// Carregar eventos do Event Store
List<Event> events = eventRepository.readEvents(aggregateId);
// events = [SolicitadoEvent(v1), AjustadoEvent(v2), ConfirmadoEvent(v3)]

// Replay: aplica eventos na ordem
aggregate.loadFromHistory(events);

// Estado final reconstruído (versão 3, status CONFIRMADO)
```

### 2.3 Snapshots

Otimização para evitar replay de muitos eventos.

**Problema**: Agregado com 10.000 eventos é lento para carregar.

**Solução**: Snapshot a cada N eventos (ex: 100)

```
Eventos: 1, 2, 3, ..., 100, [SNAPSHOT v100], 101, 102, ..., 200, [SNAPSHOT v200]

Ao carregar versão 210:
1. Carrega snapshot v200 (estado completo na versão 200)
2. Aplica eventos 201-210 (apenas 10 eventos)
3. Total: 1 snapshot + 10 eventos (ao invés de 210 eventos)
```

**Tabela**: `ES_AGGREGATE_SNAPSHOT`
```sql
CREATE TABLE ES_AGGREGATE_SNAPSHOT (
  AGGREGATE_ID  UUID     NOT NULL,
  VERSION       INTEGER  NOT NULL,
  JSON_DATA     JSON     NOT NULL,
  PRIMARY KEY (AGGREGATE_ID, VERSION)
);
```

---

## 3. Fluxo de Escrita (Command → Event)

### 3.1 Processar Comando

```
1. Cliente envia Command
   ↓
2. CommandProcessor lê agregado (reconstrói via eventos)
   ↓
3. Agregado valida comando e gera evento
   aggregate.process(command) → applyChange(event)
   ↓
4. Evento é adicionado à lista occurredEvents
   ↓
5. AggregateStore persiste evento no Event Store
   ↓
6. Evento é propagado para handlers (sync/async)
```

### 3.2 Código Simplificado

```java
// 1. Ler agregado (reconstrói estado)
Aggregate aggregate = aggregateStore.readAggregate(aggregateId);

// 2. Processar comando (gera evento)
aggregate.process(new ConfirmarCommand(aggregateId));

// Internamente:
// - Valida estado atual
// - Cria evento: ConfirmadoEvent
// - Aplica evento: atendimento.status = CONFIRMADO
// - Adiciona a occurredEvents

// 3. Persistir evento
aggregateStore.saveAggregate(aggregate);
// INSERT INTO ES_EVENT (aggregate_id, version, event_type, json_data)
// VALUES (uuid, 4, 'CONFIRMADO', '{...}')
```

---

## 4. Fluxo de Leitura (Query)

### 4.1 Leitura do Write Model (Event Store)

**Quando usar**: Operações de comando (GET /atendimento/{id} para edição)

```java
// Carrega agregado reconstruindo via eventos
Aggregate aggregate = aggregateStore.readAggregate(aggregateId);

// Se houver snapshot:
// 1. Lê snapshot mais recente (ex: v100)
// 2. Lê eventos após snapshot (v101-v120)
// 3. Aplica eventos sobre o snapshot

// Se não houver snapshot:
// 1. Lê TODOS os eventos (v1-v120)
// 2. Aplica eventos na ordem
```

### 4.2 Leitura do Read Model (Projection)

**Quando usar**: Consultas otimizadas (listagens, buscas, relatórios)

```java
// Query otimizada no modelo de leitura
List<AtendimentoProjection> result = repository
    .findByStatusAndCidadeOrderBySolicitadoemDesc("CONFIRMADO", "São Paulo");
```

**Vantagens**:
- Denormalizado (joins pré-computados)
- Índices específicos para queries
- Performance muito superior

**Desvantagem**:
- Eventual consistency (pequeno delay entre write e read)

---

## 5. Separação Write Model vs Read Model

```
┌─────────────────────────────────────────┐
│         WRITE MODEL (Event Store)       │
│                                          │
│  ES_AGGREGATE: (id, version, type)     │
│  ES_EVENT: eventos imutáveis (append)   │
│                                          │
│  Otimizado para ESCRITA                 │
│  Garantia de consistência forte         │
└─────────────────────────────────────────┘
                    ↓
            (Propagação de eventos)
                    ↓
┌─────────────────────────────────────────┐
│         READ MODEL (Projection)         │
│                                          │
│  ATENDIMENTO: (denormalizado)           │
│  Índices otimizados para queries        │
│                                          │
│  Otimizado para LEITURA                 │
│  Eventual consistency                   │
└─────────────────────────────────────────┘
```

---

## 6. Vantagens do Event Sourcing

### 6.1 Auditoria Completa
- Toda mudança de estado é registrada
- "Quem fez o quê e quando" é inerente
- Conformidade regulatória (LGPD, SOX, etc)

### 6.2 Debugging e Reprodução
- Replay de eventos para reproduzir bugs
- Time-travel para entender estado em momento específico

### 6.3 Projeções Múltiplas
- Criar múltiplas views a partir dos mesmos eventos
- Exemplo: view de auditoria, view de relatórios, view operacional

### 6.4 Evolução do Schema
- Adicionar novos campos sem migrar dados antigos
- Eventos antigos continuam funcionando

### 6.5 Event-Driven Architecture
- Eventos são publicados para outros bounded contexts
- Integração assíncrona e desacoplada

---

## 7. Desafios e Considerações

### 7.1 Complexidade
- Curva de aprendizado
- Mais código que CRUD tradicional

### 7.2 Eventual Consistency
- Read Model pode estar ligeiramente desatualizado
- Aceitável na maioria dos cenários de negócio

### 7.3 Evolução de Eventos
- Eventos antigos devem ser compatíveis com código novo
- Estratégias: upcasting, versioning

### 7.4 Replay Performance
- Agregados com muitos eventos podem ser lentos
- Solução: Snapshots

### 7.5 Deleção (GDPR/LGPD)
- Eventos são imutáveis, mas leis exigem "direito ao esquecimento"
- Soluções: crypto-shredding, pseudonimização

---

## 8. Quando Usar Event Sourcing

### Bons Casos de Uso
✅ Domínios complexos com regras de negócio ricas
✅ Necessidade de auditoria detalhada
✅ Sistemas colaborativos (múltiplos usuários editando)
✅ Event-driven architecture
✅ Análise temporal (trends, BI)

### Quando Evitar
❌ CRUDs simples sem lógica de negócio
❌ Sistemas de leitura intensiva sem necessidade de auditoria
❌ Time sem experiência em DDD/Event Sourcing
❌ Requisitos de deleção física de dados (sem alternativas)

---

## 9. Implementação no Projeto

No projeto assistencia-atendimento, Event Sourcing é usado via framework **core-cqrs**:

- **Event Store**: PostgreSQL (tabelas ES_*)
- **Snapshots**: Habilitado, a cada N eventos configurável
- **Read Model**: Tabela ATENDIMENTO (projection)
- **Propagação**: Transactional Outbox + RabbitMQ

**Arquivos relacionados**:
- [Database Schema](database-schema.md) - Estrutura das tabelas
- [Aggregate Pattern](aggregate-pattern.md) - Como usar no código
- [Command Processor](command-processor.md) - Fluxo de processamento

---

**Referências**:
- Martin Fowler: https://martinfowler.com/eaaDev/EventSourcing.html
- Greg Young: Event Sourcing (InfoQ)
