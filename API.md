# API ECOM.ATENDIMENTO - DOCUMENTAÇÃO COMPLETA

**Versão**: 1.0
**Data**: 2026-03-11
**Base URL Write Model**: `http://localhost:8080`
**Base URL Read Model**: `http://localhost:8082`

---

## ARQUITETURA CQRS/EVENT SOURCING

Este microserviço implementa **CQRS** (Command Query Responsibility Segregation) com **Event Sourcing**.

### Write Model (Porta 8080)
- Processa **comandos** (solicitar, ajustar, confirmar, etc.)
- Persiste **eventos imutáveis** no Event Store (PostgreSQL schema `ecom_ae`)
- Publica eventos no **RabbitMQ** para consumo assíncrono
- Endpoint: `GET /api/atendimento/{aggregateId}` - Reconstrói estado via replay de eventos

### Read Model (Porta 8082)
- Consome eventos do RabbitMQ
- Atualiza **projeções** otimizadas para leitura (PostgreSQL schema `ecom_proj`)
- Endpoint: `GET /api/projection/atendimento/{aggregateId}` - Consulta direta na projeção

---

## MÁQUINA DE ESTADOS

```
null → SOLICITADO → AJUSTADO → CONFIRMADO → OCORRIDO → FINALIZADO
         ↓             ↓
     CANCELADO    CANCELADO
```

**Transições Válidas**:
- `solicitar`: null → SOLICITADO (único comando sem aggregateId)
- `ajustar`: SOLICITADO | AJUSTADO → AJUSTADO (múltiplas vezes)
- `confirmar`: SOLICITADO | AJUSTADO → CONFIRMADO
- `ocorrencia`: CONFIRMADO | OCORRIDO → OCORRIDO (múltiplas vezes, acumulativo)
- `finalizar`: CONFIRMADO | OCORRIDO → FINALIZADO (dispara integração com Financeiro)
- `cancelar`: SOLICITADO | AJUSTADO → CANCELADO (bloqueado após CONFIRMADO)

---

## ENDPOINTS - WRITE MODEL (COMANDOS)

### 1. SOLICITAR ATENDIMENTO

**Endpoint**: `POST /api/atendimento/solicitar`
**HTTP Status**: `201 Created`
**Descrição**: Cria novo atendimento (aggregateId gerado pelo servidor)

**Request Body**:
```json
{
  "protocolo": "PROT-2024-001",
  "tipodeocorrencia": "Pane elétrica",
  "cliente": {
    "id": 1,
    "nome": "João Silva",
    "docfiscal": {
      "tipo": "CPF",
      "numero": "12345678900"
    }
  },
  "veiculo": {
    "placa": "ABC-1234"
  },
  "servico": {
    "id": 1,
    "nome": "Reboque"
  },
  "base": {
    "tipo": "COMERCIAL",
    "logradouro": "Av Principal",
    "numero": "100",
    "bairro": "Centro",
    "cidade": "São Paulo",
    "estado": "SP",
    "cep": "01000000"
  },
  "origem": {
    "tipo": "RESIDENCIAL",
    "logradouro": "Rua Teste",
    "numero": "50",
    "bairro": "Jardim",
    "cidade": "São Paulo",
    "estado": "SP",
    "cep": "02000000"
  }
}
```

**Response**:
```json
{
  "id": "7effcbdf-05aa-472c-9c4f-d38f6620bb69"
}
```

**Exemplo cURL**:
```bash
curl -X POST http://localhost:8080/api/atendimento/solicitar \
  -H "Content-Type: application/json" \
  -d '{
    "protocolo": "PROT-001",
    "tipodeocorrencia": "Pane elétrica",
    "cliente": {"id": 1, "nome": "João Silva", "docfiscal": {"tipo":"CPF","numero":"12345678900"}},
    "veiculo": {"placa": "ABC-1234"},
    "servico": {"id": 1, "nome": "Reboque"},
    "base": {"tipo": "COMERCIAL", "logradouro": "Av Principal", "numero": "100", "bairro": "Centro", "cidade": "São Paulo", "estado": "SP", "cep": "01000000"},
    "origem": {"tipo": "RESIDENCIAL", "logradouro": "Rua Teste", "numero": "50", "bairro": "Jardim", "cidade": "São Paulo", "estado": "SP", "cep": "02000000"}
  }'
```

**Evento Gerado**: `YC_ECOMIGO_ATENDIMENTO_SOLICITADO` (version 1)

---

### 2. AJUSTAR ATENDIMENTO

**Endpoint**: `PUT /api/atendimento/ajustar`
**HTTP Status**: `200 OK`
**Descrição**: Ajusta prestador, destino, itens (pode ser executado múltiplas vezes)

**Request Body**:
```json
{
  "aggregateId": "7effcbdf-05aa-472c-9c4f-d38f6620bb69",
  "descricao": "Cliente solicitou alteração de prestador",
  "prestador": {
    "id": 1,
    "nome": "Prestador Premium",
    "docfiscal": {
      "tipo": "CNPJ",
      "numero": "12345678000190"
    }
  },
  "destino": {
    "tipo": "COMERCIAL",
    "logradouro": "Av Destino",
    "numero": "200",
    "bairro": "Vila Nova",
    "cidade": "São Paulo",
    "estado": "SP",
    "cep": "03000000"
  },
  "items": [
    {
      "nome": "Quilometragem",
      "unidadedemedida": "km",
      "precounitario": 550
    },
    {
      "nome": "Taxa base",
      "unidadedemedida": "unidade",
      "precounitario": 5000
    }
  ]
}
```

**Response**: `200 OK` (sem body)

**Exemplo cURL**:
```bash
curl -X PUT http://localhost:8080/api/atendimento/ajustar \
  -H "Content-Type: application/json" \
  -d '{
    "aggregateId": "7effcbdf-05aa-472c-9c4f-d38f6620bb69",
    "descricao": "Ajuste solicitado",
    "prestador": {"id": 1, "nome": "Prestador Premium", "docfiscal": {"tipo":"CNPJ","numero":"12345678000190"}},
    "destino": {"tipo": "COMERCIAL", "logradouro": "Av Destino", "numero": "200", "bairro": "Vila Nova", "cidade": "São Paulo", "estado": "SP", "cep": "03000000"},
    "items": [{"nome": "Quilometragem", "unidadedemedida": "km", "precounitario": 550}]
  }'
```

**Evento Gerado**: `YC_ECOMIGO_ATENDIMENTO_AJUSTADO` (version incrementada)

---

### 3. CONFIRMAR ATENDIMENTO

**Endpoint**: `PUT /api/atendimento/confirmar`
**HTTP Status**: `200 OK`
**Descrição**: Confirma o atendimento

**Request Body**:
```json
{
  "aggregateId": "7effcbdf-05aa-472c-9c4f-d38f6620bb69"
}
```

**Response**: `200 OK` (sem body)

**Exemplo cURL**:
```bash
curl -X PUT http://localhost:8080/api/atendimento/confirmar \
  -H "Content-Type: application/json" \
  -d '{"aggregateId": "7effcbdf-05aa-472c-9c4f-d38f6620bb69"}'
```

**Evento Gerado**: `YC_ECOMIGO_ATENDIMENTO_CONFIRMADO`

---

### 4. REGISTRAR OCORRÊNCIA

**Endpoint**: `PUT /api/atendimento/ocorrencia`
**HTTP Status**: `200 OK`
**Descrição**: Registra ocorrências durante atendimento (acumulativo - pode ser executado múltiplas vezes)

**Request Body**:
```json
{
  "aggregateId": "7effcbdf-05aa-472c-9c4f-d38f6620bb69",
  "ocorrencias": [
    {
      "tipo": "CHEGADA_LOCAL",
      "descricao": "Prestador chegou ao local da pane",
      "observacao": "Veículo localizado conforme informado"
    },
    {
      "tipo": "INICIO_ATENDIMENTO",
      "descricao": "Início do procedimento de reboque",
      "observacao": "Veículo sendo preparado para transporte"
    }
  ]
}
```

**Response**: `200 OK` (sem body)

**Exemplo cURL**:
```bash
curl -X PUT http://localhost:8080/api/atendimento/ocorrencia \
  -H "Content-Type: application/json" \
  -d '{
    "aggregateId": "7effcbdf-05aa-472c-9c4f-d38f6620bb69",
    "ocorrencias": [
      {"tipo": "CHEGADA_LOCAL", "descricao": "Prestador no local"},
      {"tipo": "INICIO_ATENDIMENTO", "descricao": "Início do reboque"}
    ]
  }'
```

**Evento Gerado**: `YC_ECOMIGO_ATENDIMENTO_OCORRIDO`

---

### 5. FINALIZAR ATENDIMENTO

**Endpoint**: `PUT /api/atendimento/finalizar`
**HTTP Status**: `200 OK`
**Descrição**: Finaliza o atendimento (dispara integração com Financeiro via RabbitMQ)

**Request Body**:
```json
{
  "aggregateId": "7effcbdf-05aa-472c-9c4f-d38f6620bb69"
}
```

**Response**: `200 OK` (sem body)

**Exemplo cURL**:
```bash
curl -X PUT http://localhost:8080/api/atendimento/finalizar \
  -H "Content-Type: application/json" \
  -d '{"aggregateId": "7effcbdf-05aa-472c-9c4f-d38f6620bb69"}'
```

**Evento Gerado**: `YC_ECOMIGO_ATENDIMENTO_FINALIZADO`

**Integração**: Publica mensagem no RabbitMQ:
- Exchange: `financeiro.exchange`
- Queue: `atendimento.finalizado.queue`
- Routing Key: `atendimento.finalizado`

---

### 6. CANCELAR ATENDIMENTO

**Endpoint**: `PUT /api/atendimento/cancelar`
**HTTP Status**: `200 OK`
**Descrição**: Cancela o atendimento (só permitido antes de CONFIRMADO)

**Request Body**:
```json
{
  "aggregateId": "7effcbdf-05aa-472c-9c4f-d38f6620bb69"
}
```

**Response**: `200 OK` (sem body)

**Exemplo cURL**:
```bash
curl -X PUT http://localhost:8080/api/atendimento/cancelar \
  -H "Content-Type: application/json" \
  -d '{"aggregateId": "7effcbdf-05aa-472c-9c4f-d38f6620bb69"}'
```

**Evento Gerado**: `YC_ECOMIGO_ATENDIMENTO_CANCELADO`

**Restrição**: Se tentar cancelar após CONFIRMADO, retorna HTTP 409 Conflict

---

### 7. RECUPERAR ESTADO DO AGREGADO (WRITE MODEL)

**Endpoint**: `GET /api/atendimento/{aggregateId}`
**HTTP Status**: `200 OK`
**Descrição**: Recupera estado do agregado via replay de eventos (Event Store)

**Path Parameter**: `aggregateId` (UUID)

**Response**:
```json
{
  "aggregateId": "7effcbdf-05aa-472c-9c4f-d38f6620bb69",
  "version": 3,
  "atendimento": {
    "id": "7effcbdf-05aa-472c-9c4f-d38f6620bb69",
    "status": "CONFIRMADO",
    "protocolo": "PROT-001",
    "tipodeocorrencia": "Pane elétrica",
    "cliente": { ... },
    "veiculo": { ... },
    "servico": { ... },
    "base": { ... },
    "origem": { ... },
    "prestador": { ... },
    "destino": { ... },
    "items": [ ... ],
    "dataHoraSolicitado": "2026-03-11T10:30:00.000Z",
    "dataHoraAjustado": "2026-03-11T10:35:00.000Z",
    "dataHoraConfirmado": "2026-03-11T10:40:00.000Z"
  }
}
```

**Exemplo cURL**:
```bash
curl -X GET http://localhost:8080/api/atendimento/7effcbdf-05aa-472c-9c4f-d38f6620bb69
```

**Observação**: Este endpoint reconstitui o agregado fazendo replay de TODOS os eventos. Use para validação e auditoria.

---

## ENDPOINTS - READ MODEL (CONSULTAS)

### 8. CONSULTAR PROJEÇÃO

**Endpoint**: `GET /api/projection/atendimento/{aggregateId}`
**HTTP Status**: `200 OK` ou `404 Not Found`
**Descrição**: Consulta direta na projeção otimizada (mais rápido que replay)

**Path Parameter**: `aggregateId` (UUID)

**Response** (200 OK):
```json
{
  "id": 1,
  "aggregateId": "7effcbdf-05aa-472c-9c4f-d38f6620bb69",
  "protocolo": "PROT-001",
  "status": "CONFIRMADO",
  "tipodeocorrencia": "Pane elétrica",
  "descricao": "Ajuste solicitado",
  "cliente": "{\"id\":1,\"nome\":\"João Silva\",\"docfiscal\":{\"tipo\":\"CPF\",\"numero\":\"12345678900\"}}",
  "veiculo": "{\"placa\":\"ABC-1234\"}",
  "servico": "{\"id\":1,\"nome\":\"Reboque\"}",
  "prestador": "{\"id\":1,\"nome\":\"Prestador Premium\",\"docfiscal\":{\"tipo\":\"CNPJ\",\"numero\":\"12345678000190\"}}",
  "base": "{...}",
  "origem": "{...}",
  "destino": "{...}",
  "items": "[{\"nome\":\"Quilometragem\",\"unidadedemedida\":\"km\",\"precounitario\":550}]",
  "solicitadoem": "2026-03-11T10:30:00.000+00:00",
  "ajustadoem": "2026-03-11T10:35:00.000+00:00",
  "confirmadoem": "2026-03-11T10:40:00.000+00:00",
  "ocorridoem": null,
  "finalizadoem": null,
  "canceladoem": null,
  "logversion": 3,
  "logrole": "SYSTEM",
  "loguser": "rabbitmq-consumer"
}
```

**Response** (404 Not Found): Sem body

**Exemplo cURL**:
```bash
curl -X GET http://localhost:8082/api/projection/atendimento/7effcbdf-05aa-472c-9c4f-d38f6620bb69
```

**Observações**:
- Campos JSON (`cliente`, `veiculo`, etc.) são strings JSON
- `logversion`: Versão do último evento processado (idempotência)
- Mais rápido que GET do Write Model (consulta direta, sem replay)
- Pode ter delay de até 1 segundo após comando (processamento assíncrono)

---

## CÓDIGOS DE RESPOSTA HTTP

| Código | Descrição |
|--------|-----------|
| `200 OK` | Comando processado com sucesso (PUT) |
| `201 Created` | Atendimento criado com sucesso (POST /solicitar) |
| `400 Bad Request` | Payload inválido (validação falhou) |
| `404 Not Found` | Agregado não encontrado (GET) |
| `409 Conflict` | Transição de estado inválida |
| `500 Internal Server Error` | Erro interno do servidor |

---

## FLUXO COMPLETO DE TESTE

```bash
# 1. Solicitar atendimento (captura aggregateId)
RESPONSE=$(curl -s -X POST http://localhost:8080/api/atendimento/solicitar \
  -H "Content-Type: application/json" \
  -d '{
    "protocolo": "PROT-001",
    "tipodeocorrencia": "Pane elétrica",
    "cliente": {"id": 1, "nome": "João Silva", "docfiscal": {"tipo":"CPF","numero":"12345678900"}},
    "veiculo": {"placa": "ABC-1234"},
    "servico": {"id": 1, "nome": "Reboque"},
    "base": {"tipo": "COMERCIAL", "logradouro": "Av Principal", "numero": "100", "bairro": "Centro", "cidade": "São Paulo", "estado": "SP", "cep": "01000000"},
    "origem": {"tipo": "RESIDENCIAL", "logradouro": "Rua Teste", "numero": "50", "bairro": "Jardim", "cidade": "São Paulo", "estado": "SP", "cep": "02000000"}
  }')

# Extrai aggregateId
AGGREGATE_ID=$(echo $RESPONSE | grep -oP '"id"\s*:\s*"\K[^"]+')
echo "AggregateId: $AGGREGATE_ID"

# 2. Ajustar atendimento
curl -X PUT http://localhost:8080/api/atendimento/ajustar \
  -H "Content-Type: application/json" \
  -d "{\"aggregateId\": \"$AGGREGATE_ID\", \"descricao\": \"Ajuste solicitado\", \"prestador\": {\"id\": 1, \"nome\": \"Prestador Premium\", \"docfiscal\": {\"tipo\":\"CNPJ\",\"numero\":\"12345678000190\"}}, \"destino\": {\"tipo\": \"COMERCIAL\", \"logradouro\": \"Av Destino\", \"numero\": \"200\", \"bairro\": \"Vila Nova\", \"cidade\": \"São Paulo\", \"estado\": \"SP\", \"cep\": \"03000000\"}}"

# 3. Confirmar atendimento
curl -X PUT http://localhost:8080/api/atendimento/confirmar \
  -H "Content-Type: application/json" \
  -d "{\"aggregateId\": \"$AGGREGATE_ID\"}"

# 4. Registrar ocorrências
curl -X PUT http://localhost:8080/api/atendimento/ocorrencia \
  -H "Content-Type: application/json" \
  -d "{\"aggregateId\": \"$AGGREGATE_ID\", \"ocorrencias\": [{\"tipo\": \"CHEGADA_LOCAL\", \"descricao\": \"Prestador no local\"}]}"

# 5. Finalizar atendimento
curl -X PUT http://localhost:8080/api/atendimento/finalizar \
  -H "Content-Type: application/json" \
  -d "{\"aggregateId\": \"$AGGREGATE_ID\"}"

# 6. Consultar estado (Write Model - replay de eventos)
curl -X GET http://localhost:8080/api/atendimento/$AGGREGATE_ID

# 7. Consultar projeção (Read Model - consulta direta)
sleep 1  # Aguarda processamento assíncrono
curl -X GET http://localhost:8082/api/projection/atendimento/$AGGREGATE_ID
```

---

## QUERIES ÚTEIS NO POSTGRESQL

### Event Store (ecom_ae)

```sql
-- Ver todos os eventos de um atendimento
SELECT id, aggregate_id, version, event_type, created_date
FROM ecom_ae.event
WHERE aggregate_id = '7effcbdf-05aa-472c-9c4f-d38f6620bb69'
ORDER BY version;

-- Contar eventos por tipo
SELECT event_type, COUNT(*) as total
FROM ecom_ae.event
GROUP BY event_type
ORDER BY total DESC;

-- Ver últimos 10 eventos criados
SELECT id, aggregate_id, version, event_type, created_date
FROM ecom_ae.event
ORDER BY id DESC
LIMIT 10;

-- Ver agregados e versões
SELECT id, version, aggregate_type
FROM ecom_ae.aggregate
ORDER BY id DESC;
```

### Projeções (ecom_proj)

```sql
-- Ver todas as projeções de atendimento
SELECT aggregateid, protocolo, status, logversion
FROM ecom_proj.atendimento
ORDER BY id DESC;

-- Ver projeção específica
SELECT *
FROM ecom_proj.atendimento
WHERE aggregateid = '7effcbdf-05aa-472c-9c4f-d38f6620bb69';

-- Contar atendimentos por status
SELECT status, COUNT(*) as total
FROM ecom_proj.atendimento
GROUP BY status
ORDER BY total DESC;
```

---

## VALIDAÇÃO E TESTES

### Script de Teste Automatizado

Localização: `/tmp/test_atendimento_cqrs_fixed.sh`

Executa 10 cadeias de comandos cobrindo todas as transições de estado:

```bash
bash /tmp/test_atendimento_cqrs_fixed.sh
```

**Resultados esperados**:
- Total de comandos: 31
- Comandos bem-sucedidos: 31
- Falhas: 0
- Eventos criados: 41
- Agregados criados: 10
- Projeções atualizadas: 10

---

## ARQUITETURA DE SCHEMAS

### ecom_ae (Event Store)
- `event`: Log imutável de eventos (append-only)
- `aggregate`: Metadados dos agregados (ID, versão, tipo)
- `aggregate_snapshot`: Snapshots otimizados (a cada 100 eventos)

### ecom_proj (Read Model)
- `atendimento`: Projeção otimizada para consultas

### ecom_suporte (Dados de Suporte)
- `cliente`: Dados de clientes
- `prestador`: Dados de prestadores
- `servico`: Catálogo de serviços
- `servicodeprestador`: Serviços oferecidos por prestadores
- `veiculodecliente`: Veículos dos clientes

---

## INTEGRAÇÃO COM OUTROS MICROSERVIÇOS

### Financeiro (via RabbitMQ)

**Evento disparado**: Apenas `FINALIZADO`

**Payload**:
```json
{
  "aggregateId": "uuid",
  "protocolo": "string",
  "cliente": { ... },
  "prestador": { ... },
  "servico": { ... },
  "items": [ ... ],
  "ocorrencias": [ ... ],
  "dataHoraFinalizado": "timestamp"
}
```

**RabbitMQ**:
- Exchange: `financeiro.exchange`
- Queue: `atendimento.finalizado.queue`
- Routing Key: `atendimento.finalizado`

---

## OBSERVABILIDADE

### Actuator Endpoints (Porta 8080)

```bash
# Health check
curl http://localhost:8080/actuator/health

# Métricas
curl http://localhost:8080/actuator/metrics

# Prometheus
curl http://localhost:8080/actuator/prometheus
```

### Logs

Padrão de logs:
```
[INFO] Recebendo solicitação de atendimento com protocolo: PROT-001
[INFO] Atendimento 7effcbdf-05aa-472c-9c4f-d38f6620bb69 solicitado com sucesso
[INFO] Atendimento 7effcbdf-05aa-472c-9c4f-d38f6620bb69 ajustado com sucesso
[INFO] Atendimento 7effcbdf-05aa-472c-9c4f-d38f6620bb69 confirmado com sucesso
[INFO] Atendimento 7effcbdf-05aa-472c-9c4f-d38f6620bb69 finalizado com sucesso
```

---

## DIFERENÇAS WRITE MODEL VS READ MODEL

| Aspecto | Write Model (8080) | Read Model (8082) |
|---------|-------------------|-------------------|
| **Endpoint** | `GET /api/atendimento/{id}` | `GET /api/projection/atendimento/{id}` |
| **Fonte** | Event Store (replay) | Projeção (tabela) |
| **Performance** | Mais lento (replay) | Mais rápido (query direta) |
| **Consistência** | Sempre atualizado | Eventual (delay ~1s) |
| **Uso** | Validação, auditoria | Consultas, dashboards |
| **Campos** | Objetos Java | Strings JSON |

---

**Última atualização**: 2026-03-11
**Versão**: 1.0
**Autor**: Sistema CQRS/ES ecom.atendimento
