# Database Schema - PostgreSQL Event Store

Estrutura completa do banco de dados PostgreSQL para Event Sourcing.

---

## 1. Visão Geral

O banco de dados é composto por:
- **1 Schema**: `assistencia_es` (Event Store)
- **4 Tabelas principais**: ES_AGGREGATE, ES_EVENT, ES_AGGREGATE_SNAPSHOT, ES_EVENT_SUBSCRIPTION
- **1 Trigger**: Notificação PostgreSQL LISTEN/NOTIFY para processamento assíncrono

---

## 2. Schema

### Criação do Schema

**Arquivo**: `V0__create_schema_assistencia_es.sql`

```sql
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'assistencia_es') THEN
        CREATE SCHEMA assistencia_es;
    END IF;
END $$;
```

**Função**:
- Cria schema `assistencia_es` se não existir
- Isolamento dos dados de Event Sourcing
- Evita conflitos com outras tabelas da aplicação

---

## 3. Tabelas Event Sourcing

### 3.1 ES_AGGREGATE

**Propósito**: Registro "cabeçalho" de cada agregado.

```sql
CREATE TABLE IF NOT EXISTS ASSISTENCIA_ES.ES_AGGREGATE (
  ID              UUID     PRIMARY KEY,
  VERSION         INTEGER  NOT NULL,
  AGGREGATE_TYPE  TEXT     NOT NULL
);

CREATE INDEX IF NOT EXISTS IDX_ES_AGGREGATE_AGGREGATE_TYPE
  ON ASSISTENCIA_ES.ES_AGGREGATE (AGGREGATE_TYPE);
```

#### Colunas

| Coluna | Tipo | Nullable | Descrição |
|--------|------|----------|-----------|
| ID | UUID | NOT NULL | Identificador único do agregado (PK) |
| VERSION | INTEGER | NOT NULL | Versão atual do agregado (para optimistic locking) |
| AGGREGATE_TYPE | TEXT | NOT NULL | Tipo do agregado (ex: "YC_ECOMIGO_ATENDIMENTO") |

#### Índices

- **PK**: `ID` (UUID)
- **IDX_ES_AGGREGATE_AGGREGATE_TYPE**: Para queries por tipo de agregado

#### Uso

```sql
-- Exemplo de registro
INSERT INTO assistencia_es.ES_AGGREGATE (ID, VERSION, AGGREGATE_TYPE)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 0, 'YC_ECOMIGO_ATENDIMENTO');

-- Atualização de versão (optimistic locking)
UPDATE assistencia_es.ES_AGGREGATE
SET VERSION = 3
WHERE ID = '550e8400-e29b-41d4-a716-446655440000'
  AND VERSION = 2;  -- Verifica versão esperada
```

---

### 3.2 ES_EVENT

**Propósito**: Event Store principal. Armazena TODOS os eventos (append-only).

```sql
CREATE TABLE IF NOT EXISTS ASSISTENCIA_ES.ES_EVENT (
  ID              BIGSERIAL  PRIMARY KEY,
  TRANSACTION_ID  XID8       NOT NULL,
  AGGREGATE_ID    UUID       NOT NULL REFERENCES ASSISTENCIA_ES.ES_AGGREGATE (ID),
  VERSION         INTEGER    NOT NULL,
  EVENT_TYPE      TEXT       NOT NULL,
  JSON_DATA       JSON       NOT NULL,
  UNIQUE (AGGREGATE_ID, VERSION)
);

CREATE INDEX IF NOT EXISTS IDX_ES_EVENT_TRANSACTION_ID_ID
  ON ASSISTENCIA_ES.ES_EVENT (TRANSACTION_ID, ID);

CREATE INDEX IF NOT EXISTS IDX_ES_EVENT_AGGREGATE_ID
  ON ASSISTENCIA_ES.ES_EVENT (AGGREGATE_ID);

CREATE INDEX IF NOT EXISTS IDX_ES_EVENT_VERSION
  ON ASSISTENCIA_ES.ES_EVENT (VERSION);
```

#### Colunas

| Coluna | Tipo | Nullable | Descrição |
|--------|------|----------|-----------|
| ID | BIGSERIAL | NOT NULL | ID sequencial do evento (PK) |
| TRANSACTION_ID | XID8 | NOT NULL | ID da transação PostgreSQL (para outbox) |
| AGGREGATE_ID | UUID | NOT NULL | FK para ES_AGGREGATE |
| VERSION | INTEGER | NOT NULL | Versão do agregado neste evento |
| EVENT_TYPE | TEXT | NOT NULL | Tipo do evento (ex: "SOLICITADO", "CONFIRMADO") |
| JSON_DATA | JSON | NOT NULL | Evento serializado completo |

#### Constraints

- **UNIQUE** (AGGREGATE_ID, VERSION): Garante ordem e unicidade de versões por agregado

#### Índices

- **PK**: `ID` (BIGSERIAL)
- **IDX_ES_EVENT_TRANSACTION_ID_ID**: Para Transactional Outbox Pattern
- **IDX_ES_EVENT_AGGREGATE_ID**: Para leitura de eventos de um agregado
- **IDX_ES_EVENT_VERSION**: Para queries por versão

#### Exemplo de JSON_DATA

```json
{
  "aggregateid": "550e8400-e29b-41d4-a716-446655440000",
  "version": 1,
  "createdDate": "2025-03-10T10:30:00.000Z",
  "status": "SOLICITADO",
  "protocolo": "ATD-2025-000123",
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
  "tipodeocorrencia": "Pane elétrica",
  "base": {...},
  "origem": {...}
}
```

#### Uso

```sql
-- Inserir evento (append-only)
INSERT INTO assistencia_es.ES_EVENT
  (TRANSACTION_ID, AGGREGATE_ID, VERSION, EVENT_TYPE, JSON_DATA)
VALUES
  (pg_current_xact_id(),
   '550e8400-e29b-41d4-a716-446655440000',
   1,
   'YC_ECOMIGO_ATENDIMENTO_SOLICITADO',
   '{"aggregateid": "...", "version": 1, ...}'::json);

-- Ler eventos de um agregado (para reconstitução)
SELECT ID, EVENT_TYPE, JSON_DATA
FROM assistencia_es.ES_EVENT
WHERE AGGREGATE_ID = '550e8400-e29b-41d4-a716-446655440000'
ORDER BY VERSION ASC;

-- Ler eventos após versão específica (para snapshot)
SELECT ID, EVENT_TYPE, JSON_DATA
FROM assistencia_es.ES_EVENT
WHERE AGGREGATE_ID = '550e8400-...'
  AND VERSION > 100
ORDER BY VERSION ASC;
```

---

### 3.3 ES_AGGREGATE_SNAPSHOT

**Propósito**: Snapshots para otimização de leitura.

```sql
CREATE TABLE IF NOT EXISTS ASSISTENCIA_ES.ES_AGGREGATE_SNAPSHOT (
  AGGREGATE_ID  UUID     NOT NULL REFERENCES ASSISTENCIA_ES.ES_AGGREGATE (ID),
  VERSION       INTEGER  NOT NULL,
  JSON_DATA     JSON     NOT NULL,
  PRIMARY KEY (AGGREGATE_ID, VERSION)
);

CREATE INDEX IF NOT EXISTS IDX_ES_AGGREGATE_SNAPSHOT_AGGREGATE_ID
  ON ASSISTENCIA_ES.ES_AGGREGATE_SNAPSHOT (AGGREGATE_ID);

CREATE INDEX IF NOT EXISTS IDX_ES_AGGREGATE_SNAPSHOT_VERSION
  ON ASSISTENCIA_ES.ES_AGGREGATE_SNAPSHOT (VERSION);
```

#### Colunas

| Coluna | Tipo | Nullable | Descrição |
|--------|------|----------|-----------|
| AGGREGATE_ID | UUID | NOT NULL | FK para ES_AGGREGATE |
| VERSION | INTEGER | NOT NULL | Versão do snapshot |
| JSON_DATA | JSON | NOT NULL | Agregado serializado completo naquela versão |

#### Uso

```sql
-- Criar snapshot a cada 100 eventos
INSERT INTO assistencia_es.ES_AGGREGATE_SNAPSHOT
  (AGGREGATE_ID, VERSION, JSON_DATA)
VALUES
  ('550e8400-e29b-41d4-a716-446655440000',
   100,
   '{"aggregateId": "...", "atendimento": {...}}'::json);

-- Ler snapshot mais recente
SELECT VERSION, JSON_DATA
FROM assistencia_es.ES_AGGREGATE_SNAPSHOT
WHERE AGGREGATE_ID = '550e8400-...'
ORDER BY VERSION DESC
LIMIT 1;
```

**Benefício**:
- Agregado com 1000 eventos: carrega snapshot v900 + 100 eventos
- Ao invés de carregar 1000 eventos

---

### 3.4 ES_EVENT_SUBSCRIPTION

**Propósito**: Checkpoint para Transactional Outbox Pattern.

```sql
CREATE TABLE IF NOT EXISTS ASSISTENCIA_ES.ES_EVENT_SUBSCRIPTION (
  SUBSCRIPTION_NAME    TEXT    PRIMARY KEY,
  LAST_TRANSACTION_ID  XID8    NOT NULL,
  LAST_EVENT_ID        BIGINT  NOT NULL
);
```

#### Colunas

| Coluna | Tipo | Nullable | Descrição |
|--------|------|----------|-----------|
| SUBSCRIPTION_NAME | TEXT | NOT NULL | Nome da subscription (ex: "financeiro-integration") |
| LAST_TRANSACTION_ID | XID8 | NOT NULL | Última transação processada |
| LAST_EVENT_ID | BIGINT | NOT NULL | Último evento processado |

#### Uso

```sql
-- Criar subscription
INSERT INTO assistencia_es.ES_EVENT_SUBSCRIPTION
  (SUBSCRIPTION_NAME, LAST_TRANSACTION_ID, LAST_EVENT_ID)
VALUES
  ('financeiro-integration', '0', 0);

-- Atualizar checkpoint após processar eventos
UPDATE assistencia_es.ES_EVENT_SUBSCRIPTION
SET LAST_TRANSACTION_ID = '12345',
    LAST_EVENT_ID = 6789
WHERE SUBSCRIPTION_NAME = 'financeiro-integration';

-- Ler eventos novos (não processados)
SELECT e.ID, e.EVENT_TYPE, e.JSON_DATA
FROM assistencia_es.ES_EVENT e
WHERE (e.TRANSACTION_ID, e.ID) > (
  SELECT LAST_TRANSACTION_ID, LAST_EVENT_ID
  FROM assistencia_es.ES_EVENT_SUBSCRIPTION
  WHERE SUBSCRIPTION_NAME = 'financeiro-integration'
)
ORDER BY e.TRANSACTION_ID, e.ID
LIMIT 100;
```

**Garantia**: Exactly-once delivery (com idempotência no consumidor)

---

## 4. Trigger - PostgreSQL LISTEN/NOTIFY

### Função e Trigger

**Arquivo**: `V2__notify_trigger.sql`

```sql
CREATE OR REPLACE FUNCTION CHANNEL_EVENT_NOTIFY_FCT()
RETURNS TRIGGER AS
  $BODY$
  DECLARE
    aggregate_type  TEXT;
  BEGIN
    SELECT a.AGGREGATE_TYPE INTO aggregate_type
    FROM ASSISTENCIA_ES.ES_AGGREGATE a
    WHERE a.ID = NEW.AGGREGATE_ID;

    PERFORM pg_notify('channel_event_notify', aggregate_type);
    RETURN NEW;
  END;
  $BODY$
  LANGUAGE PLPGSQL;

CREATE OR REPLACE TRIGGER CHANNEL_EVENT_NOTIFY_TRG
  AFTER INSERT ON ASSISTENCIA_ES.ES_EVENT
  FOR EACH ROW
  EXECUTE PROCEDURE CHANNEL_EVENT_NOTIFY_FCT();
```

### Propósito

- **Notificação assíncrona**: Quando novo evento é inserido em ES_EVENT
- **Canal PostgreSQL**: `channel_event_notify`
- **Payload**: Tipo do agregado (ex: "YC_ECOMIGO_ATENDIMENTO")

### Uso no Código Java

```java
// Worker assíncrono escuta o canal
Connection conn = dataSource.getConnection();
Statement stmt = conn.createStatement();
stmt.execute("LISTEN channel_event_notify");

while (true) {
    PGNotification[] notifications = ((PGConnection) conn).getNotifications();
    for (PGNotification notification : notifications) {
        String aggregateType = notification.getParameter();
        // Processar eventos novos do tipo aggregateType
        processNewEvents(aggregateType);
    }
    Thread.sleep(1000);
}
```

---

## 5. Diagrama de Relacionamentos

```
┌──────────────────────────┐
│    ES_AGGREGATE          │
│  ┌────────────────────┐  │
│  │ ID (PK)            │  │
│  │ VERSION            │  │
│  │ AGGREGATE_TYPE     │  │
│  └────────────────────┘  │
└────────┬─────────────────┘
         │
         │ 1:N
         ↓
┌──────────────────────────┐
│    ES_EVENT              │
│  ┌────────────────────┐  │
│  │ ID (PK)            │  │
│  │ TRANSACTION_ID     │  │
│  │ AGGREGATE_ID (FK)  │──┘
│  │ VERSION            │
│  │ EVENT_TYPE         │
│  │ JSON_DATA          │
│  └────────────────────┘  │
└──────────────────────────┘
         │
         │ 1:N (opcional)
         ↓
┌──────────────────────────┐
│ ES_AGGREGATE_SNAPSHOT    │
│  ┌────────────────────┐  │
│  │ AGGREGATE_ID (FK)  │──┘
│  │ VERSION            │
│  │ JSON_DATA          │
│  └────────────────────┘  │
└──────────────────────────┘

┌──────────────────────────┐
│ ES_EVENT_SUBSCRIPTION    │
│  ┌────────────────────┐  │
│  │ SUBSCRIPTION_NAME  │  │
│  │ LAST_TRANSACTION_ID│  │
│  │ LAST_EVENT_ID      │  │
│  └────────────────────┘  │
└──────────────────────────┘
```

---

## 6. Migrations Flyway

### Ordem de Execução

```
V0__create_schema_assistencia_es.sql       (Schema)
   ↓
V1__eventsourcing_tables.sql               (Tabelas)
   ↓
V2__notify_trigger.sql                     (Trigger)
```

### Configuração do Flyway

**application.yml**:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    schemas: assistencia_es
```

### Localização dos Arquivos

```
src/main/resources/
└── db/
    └── migration/
        ├── V0__create_schema_assistencia_es.sql
        ├── V1__eventsourcing_tables.sql
        └── V2__notify_trigger.sql
```

---

## 7. Queries Úteis

### Ver todos os eventos de um agregado
```sql
SELECT
  e.VERSION,
  e.EVENT_TYPE,
  e.JSON_DATA->>'createdDate' AS created_at,
  e.JSON_DATA
FROM assistencia_es.ES_EVENT e
WHERE e.AGGREGATE_ID = '550e8400-e29b-41d4-a716-446655440000'
ORDER BY e.VERSION;
```

### Ver agregados por tipo
```sql
SELECT
  a.ID,
  a.VERSION,
  a.AGGREGATE_TYPE
FROM assistencia_es.ES_AGGREGATE a
WHERE a.AGGREGATE_TYPE = 'YC_ECOMIGO_ATENDIMENTO'
ORDER BY a.ID;
```

### Contar eventos por tipo
```sql
SELECT
  EVENT_TYPE,
  COUNT(*) as total
FROM assistencia_es.ES_EVENT
GROUP BY EVENT_TYPE
ORDER BY total DESC;
```

### Ver eventos mais recentes
```sql
SELECT
  e.ID,
  e.AGGREGATE_ID,
  e.VERSION,
  e.EVENT_TYPE,
  e.JSON_DATA->>'createdDate' AS created_at
FROM assistencia_es.ES_EVENT e
ORDER BY e.ID DESC
LIMIT 10;
```

---

## 8. Referências

**Arquivos de Migration de Referência**:
- `/home/julio/Codes/YC/Experiments/comigo/assistencia.atendimento/src/main/resources/db/migration/`

**Arquivos relacionados**:
- [Event Sourcing Concepts](event-sourcing.md) - Conceitos de ES
- [Aggregate Pattern](aggregate-pattern.md) - Como usar agregados

---

**Observação**: Ao implementar o novo projeto, copie estes arquivos de migration e adapte o nome do schema conforme necessário (ex: manter `assistencia_es` ou usar outro nome definido na especificação).
