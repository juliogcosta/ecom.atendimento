# Mapeamento Detalhado da Especificação domain.aggregates.spec

## 1. Visão Geral da Especificação

**Arquivo**: `./domain.aggregates.spec`
**Formato**: JSON
**Bounded Context**: `assistencia` (Gestão de Atendimentos)
**Agregado**: `atendimento`

---

## 2. Metadados do Projeto

```json
{
  "yc.ecomigo": {
    "aggregate": {
      "assistencia.atendimento": {
        "org": {
          "name": "yc"
        },
        "project": {
          "name": "ecomigo"
        },
        "schema": {
          "forWriteModel.name": "t_ae_db.client",
          "forReadModel.name": "assistencia"
        },
        "boundedContext": {
          "name": "assistencia",
          "comment": "Gestão de Atendimentos"
        },
        "tenantId": {
          "forWriteModel": "9d77f828-5e8c-4807-b554-5a29e85fc37f",
          "forReadModel": "9d77f828-5e8c-4807-b554-5a29e85fc37f"
        },
        "type": "atendimento"
      }
    }
  }
}
```

### 2.1 Informações Extraídas

| Campo | Valor | Uso |
|-------|-------|-----|
| Organização | yc | Prefixo de packages |
| Projeto | ecomigo | Nome do projeto |
| Schema Write Model | t_ae_db.client | Schema do Event Store |
| Schema Read Model | assistencia | Schema das Projections |
| Bounded Context | assistencia | Módulo/namespace |
| Tenant ID | 9d77f828-5e8c-4807-b554-5a29e85fc37f | Multi-tenancy |
| Tipo do Agregado | atendimento | Nome da entidade principal |

---

## 3. Comandos Detalhados

### 3.1 Comando: SOLICITAR

Cria um novo atendimento.

**Estado Inicial**: `null` (agregado não existe)
**Estado Final**: `solicitado`

**Roles Permitidas**: `MASTER`, `ATENDENTE`, `GERENTE`

#### Atributos

| Nome | Tipo | Length | Nullable | Comentário |
|------|------|--------|----------|------------|
| tipodeocorrencia | String | 100 | false | Tipo de ocorrência reportada pelo cliente |
| protocolo | String | 64 | false | Chave de protocolo de atendimento |
| status | String | 32 | false | Estado atual do agregado |

#### Value Objects Single

**cliente**
```json
{
  "id": {"type": "Long", "nullable": false, "comment": "ID do cliente"},
  "nome": {"type": "String", "length": 200, "nullable": false, "comment": "Nome completo do cliente"},
  "docfiscal": {"type": "Json", "nullable": false, "comment": "Documento fiscal (tipo e numero)"}
}
```

**veiculo**
```json
{
  "placa": {"type": "String", "length": 10, "nullable": false, "comment": "Placa do veículo"}
}
```

**servico**
```json
{
  "id": {"type": "Long", "nullable": false, "comment": "ID do serviço"},
  "nome": {"type": "String", "length": 100, "nullable": false, "comment": "Nome do serviço"}
}
```

**base** (endereço da base de atendimento)
```json
{
  "tipo": {"type": "String", "length": 20, "nullable": false, "comment": "Tipo de endereço (RESIDENCIAL, COMERCIAL, etc)"},
  "logradouro": {"type": "String", "length": 200, "nullable": false},
  "numero": {"type": "String", "length": 10, "nullable": false},
  "complemento": {"type": "String", "length": 100, "nullable": true},
  "bairro": {"type": "String", "length": 100, "nullable": false},
  "cidade": {"type": "String", "length": 100, "nullable": false},
  "estado": {"type": "String", "length": 2, "nullable": false, "comment": "UF"},
  "cep": {"type": "String", "length": 8, "nullable": false, "comment": "CEP sem formatação"}
}
```

**origem** (local onde está o cliente/veículo)
```json
// Mesma estrutura de "base"
```

#### Value Objects Multiple

Nenhum neste comando.

---

### 3.2 Comando: AJUSTAR

Ajusta dados do atendimento (pode ser executado múltiplas vezes).

**Estados Iniciais**: `solicitado`, `ajustado`
**Estado Final**: `ajustado`

**Roles Permitidas**: `MASTER`, `ATENDENTE`, `GERENTE`

#### Atributos

| Nome | Tipo | Length | Nullable | Comentário |
|------|------|--------|----------|------------|
| descricao | Text | - | true | Descrição do ajuste realizado |
| status | String | 32 | false | Estado atual do agregado |

#### Value Objects Single

**prestador**
```json
{
  "id": {"type": "Long", "nullable": false, "comment": "ID do prestador"},
  "nome": {"type": "String", "length": 200, "nullable": false, "comment": "Nome completo do prestador"},
  "docfiscal": {"type": "Json", "nullable": false, "comment": "Documento fiscal (tipo e numero)"}
}
```

**servico** (pode ser alterado)
```json
{
  "id": {"type": "Long", "nullable": true, "comment": "ID do serviço"},
  "nome": {"type": "String", "length": 100, "nullable": true, "comment": "Nome do serviço"}
}
```

**origem** (pode ser alterado - campos nullable)
```json
{
  "tipo": {"type": "String", "length": 20, "nullable": true},
  "logradouro": {"type": "String", "length": 200, "nullable": true},
  "numero": {"type": "String", "length": 10, "nullable": true},
  "complemento": {"type": "String", "length": 100, "nullable": true},
  "bairro": {"type": "String", "length": 100, "nullable": true},
  "cidade": {"type": "String", "length": 100, "nullable": true},
  "estado": {"type": "String", "length": 2, "nullable": true},
  "cep": {"type": "String", "length": 8, "nullable": true}
}
```

**destino** (para onde o veículo será levado - obrigatório)
```json
{
  "tipo": {"type": "String", "length": 20, "nullable": false},
  "logradouro": {"type": "String", "length": 200, "nullable": false},
  "numero": {"type": "String", "length": 10, "nullable": false},
  "complemento": {"type": "String", "length": 100, "nullable": true},
  "bairro": {"type": "String", "length": 100, "nullable": false},
  "cidade": {"type": "String", "length": 100, "nullable": false},
  "estado": {"type": "String", "length": 2, "nullable": false},
  "cep": {"type": "String", "length": 8, "nullable": false}
}
```

#### Value Objects Multiple

**items** (itens de serviço)
```json
{
  "nome": {"type": "String", "length": 200, "nullable": false, "comment": "Nome do item de serviço"},
  "unidadedemedida": {"type": "String", "length": 20, "nullable": false, "comment": "Unidade (ex: unidade, km, hora)"},
  "precounitario": {"type": "Integer", "nullable": false, "comment": "Preço unitário em centavos"},
  "quantidade": {"type": "Integer", "nullable": false, "comment": "Quantidade do item"},
  "observacao": {"type": "String", "length": 500, "nullable": true, "comment": "Observações sobre o item"}
}
```

**Observação**: `items` é um **array** de objetos com esta estrutura.

---

### 3.3 Comando: CONFIRMAR

Confirma o atendimento após ajustes.

**Estados Iniciais**: `ajustado`, `solicitado`
**Estado Final**: `confirmado`

**Roles Permitidas**: `MASTER`, `ATENDENTE`, `GERENTE`

#### Atributos

| Nome | Tipo | Length | Nullable | Comentário |
|------|------|--------|----------|------------|
| status | String | 32 | false | Estado atual do agregado |

#### Value Objects

Nenhum. Apenas mudança de estado.

---

### 3.4 Comando: OCORRENCIA (NOVO - não existe na implementação atual)

Registra ocorrências durante o atendimento (pode ser executado múltiplas vezes).

**Estados Iniciais**: `confirmado`, `ocorrido`
**Estado Final**: `ocorrido`

**Roles Permitidas**: `MASTER`, `ATENDENTE`, `GERENTE`

#### Atributos

| Nome | Tipo | Length | Nullable | Comentário |
|------|------|--------|----------|------------|
| status | String | 32 | false | Estado atual do agregado |

#### Value Objects Single

Nenhum.

#### Value Objects Multiple

**ocorrencias**
```json
{
  "type": "Text",
  "nullable": false,
  "comment": "Descrição da ocorrência registrada"
}
```

**Observação**: `ocorrencias` é um **array** de strings (textos).

**Exemplo**:
```json
{
  "ocorrencias": [
    "Cliente não estava no local",
    "Necessário reboque adicional devido ao peso do veículo",
    "Trânsito intenso, atraso de 30 minutos"
  ]
}
```

---

### 3.5 Comando: FINALIZAR

Finaliza o atendimento.

**Estados Iniciais**: `confirmado`, `ocorrido`
**Estado Final**: `finalizado`

**Roles Permitidas**: `MASTER`, `ATENDENTE`, `GERENTE`

#### Atributos

| Nome | Tipo | Length | Nullable | Comentário |
|------|------|--------|----------|------------|
| status | String | 32 | false | Estado atual do agregado |

#### Value Objects

Nenhum. Apenas mudança de estado.

---

### 3.6 Comando: CANCELAR

Cancela o atendimento.

**Estados Iniciais**: `solicitado`, `ajustado`
**Estado Final**: `cancelado`

**Roles Permitidas**: `MASTER`, `ATENDENTE`, `GERENTE`

#### Atributos

| Nome | Tipo | Length | Nullable | Comentário |
|------|------|--------|----------|------------|
| status | String | 32 | false | Estado atual do agregado |

#### Value Objects

Nenhum. Apenas mudança de estado.

---

## 4. Eventos Detalhados

### 4.1 Evento: solicitado

**Type**: `solicitado`
**When Attribute**: `solicitadoem` (timestamp)

#### Domain Bus

**Trigger Projection**:
- `persistence/c/e` (persistência command/event)

**Trigger Domain**:
Nenhum.

---

### 4.2 Evento: ajustado

**Type**: `ajustado`
**When Attribute**: `ajustadoem` (timestamp)

#### Domain Bus

**Trigger Projection**:
- `persistence/c/e`

**Trigger Domain**:
```json
{
  "boundedContext": "suporte",
  "aggregate": "atendimento",
  "event": "ajustado",
  "enabled": "false"
}
```

**Observação**: Integração com `suporte.atendimento` está **desabilitada** na spec.

---

### 4.3 Evento: confirmado

**Type**: `confirmado`
**When Attribute**: `confirmadoem` (timestamp)

#### Domain Bus

**Trigger Projection**:
- `persistence/c/e`

**Trigger Domain**:
```json
{
  "boundedContext": "suporte",
  "aggregate": "atendimento",
  "event": "confirmado",
  "enabled": "false"
}
```

---

### 4.4 Evento: ocorrido (NOVO)

**Type**: `ocorrido`
**When Attribute**: `ocorridoem` (timestamp)

#### Domain Bus

**Trigger Projection**:
- `persistence/c/e`

**Trigger Domain**:
Nenhum.

---

### 4.5 Evento: finalizado

**Type**: `finalizado`
**When Attribute**: `finalizadoem` (timestamp)

#### Domain Bus

**Trigger Projection**:
- `persistence/c/e`

**Trigger Domain**:
```json
[
  {
    "boundedContext": "suporte",
    "aggregate": "atendimento",
    "event": "finalizado",
    "enabled": "false"
  },
  {
    "boundedContext": "financeiro",
    "aggregate": "atendimento",
    "event": "finalizado"
    // enabled não especificado = true (habilitado)
  }
]
```

**Observação**: Evento `finalizado` deve acionar integração com `financeiro.atendimento`.

---

### 4.6 Evento: cancelado

**Type**: `cancelado`
**When Attribute**: `canceladoem` (timestamp)

#### Domain Bus

**Trigger Projection**:
- `persistence/c/e`

**Trigger Domain**:
```json
{
  "boundedContext": "suporte",
  "aggregate": "atendimento",
  "event": "cancelado",
  "enabled": "false"
}
```

---

## 5. Máquina de Estados

### 5.1 Diagrama de Transições

```
[INÍCIO (null)]
      ↓
   solicitar
      ↓
[SOLICITADO] ───────────────┐
      ↓                     ↓
   ajustar              confirmar
      ↓                     ↓
[AJUSTADO] ────────────> [CONFIRMADO]
      ↓                     ↓
   confirmar            ocorrencia
      ↓                     ↓
[CONFIRMADO]            [OCORRIDO]
      ↓                     ↓
   finalizar            finalizar
      ↓                     ↓
[FINALIZADO]          [FINALIZADO]

# Cancelamento (apenas de SOLICITADO ou AJUSTADO)
[SOLICITADO] ──cancelar──> [CANCELADO]
[AJUSTADO] ────cancelar──> [CANCELADO]
```

### 5.2 Tabela de Transições

| Estado Atual | Comando | Estado Final | Observações |
|--------------|---------|--------------|-------------|
| `null` | `solicitar` | `solicitado` | Criação do agregado |
| `solicitado` | `ajustar` | `ajustado` | Pode ajustar múltiplas vezes |
| `ajustado` | `ajustar` | `ajustado` | Reajuste |
| `solicitado` | `confirmar` | `confirmado` | Confirmação direta |
| `ajustado` | `confirmar` | `confirmado` | Confirmação após ajuste |
| `confirmado` | `ocorrencia` | `ocorrido` | Registro de ocorrência |
| `ocorrido` | `ocorrencia` | `ocorrido` | Múltiplas ocorrências |
| `confirmado` | `finalizar` | `finalizado` | Finalização sem ocorrências |
| `ocorrido` | `finalizar` | `finalizado` | Finalização após ocorrências |
| `solicitado` | `cancelar` | `cancelado` | Cancelamento antes de confirmar |
| `ajustado` | `cancelar` | `cancelado` | Cancelamento após ajuste |

### 5.3 Estados Terminais

- `finalizado`: Atendimento concluído com sucesso
- `cancelado`: Atendimento cancelado

**Observação**: Não há transições saindo destes estados.

---

## 6. Value Objects e Estruturas de Dados

### 6.1 DocFiscal (JSON)

```json
{
  "tipo": "CPF" | "CNPJ",
  "numero": "string"
}
```

### 6.2 Endereco

| Campo | Tipo | Length | Nullable | Validação |
|-------|------|--------|----------|-----------|
| tipo | String | 20 | varia | RESIDENCIAL, COMERCIAL, etc |
| logradouro | String | 200 | varia | - |
| numero | String | 10 | varia | - |
| complemento | String | 100 | true | Sempre nullable |
| bairro | String | 100 | varia | - |
| cidade | String | 100 | varia | - |
| estado | String | 2 | varia | UF (ex: SP, RJ) |
| cep | String | 8 | varia | Apenas dígitos (sem hífen) |

**Variação**: `nullable` depende do contexto (base/origem/destino e comando).

### 6.3 ClienteRef

| Campo | Tipo | Nullable | Comentário |
|-------|------|----------|------------|
| id | Long | false | ID do cliente |
| nome | String(200) | false | Nome completo |
| docfiscal | Json | false | Documento fiscal |

### 6.4 VeiculodeclienteRef

| Campo | Tipo | Nullable | Comentário |
|-------|------|----------|------------|
| placa | String(10) | false | Placa do veículo |

### 6.5 ServicoRef

| Campo | Tipo | Nullable | Comentário |
|-------|------|----------|------------|
| id | Long | varia | ID do serviço |
| nome | String(100) | varia | Nome do serviço |

**Variação**: No comando `ajustar`, estes campos podem ser `nullable`.

### 6.6 PrestadorRef

| Campo | Tipo | Nullable | Comentário |
|-------|------|----------|------------|
| id | Long | false | ID do prestador |
| nome | String(200) | false | Nome completo |
| docfiscal | Json | false | Documento fiscal |

### 6.7 Itemdeatendimento

| Campo | Tipo | Nullable | Comentário |
|-------|------|----------|------------|
| nome | String(200) | false | Nome do item |
| unidadedemedida | String(20) | false | Ex: unidade, km, hora |
| precounitario | Integer | false | Preço em centavos |
| quantidade | Integer | false | Quantidade |
| observacao | String(500) | true | Observações |

---

## 7. Campos Temporais (When Attributes)

Cada evento possui um campo temporal associado:

| Evento | Campo Timestamp | Tipo |
|--------|----------------|------|
| solicitado | solicitadoem | Timestamp |
| ajustado | ajustadoem | Timestamp |
| confirmado | confirmadoem | Timestamp |
| ocorrido | ocorridoem | Timestamp |
| finalizado | finalizadoem | Timestamp |
| cancelado | canceladoem | Timestamp |

**Uso**: Auditoria, rastreabilidade, ordenação de eventos.

---

## 8. Integração com Outros Bounded Contexts

### 8.1 Mapeamento de Triggers

| Evento | Bounded Context Destino | Habilitado | Propósito |
|--------|-------------------------|------------|-----------|
| ajustado | suporte.atendimento | **NÃO** | (Integração futura) |
| confirmado | suporte.atendimento | **NÃO** | (Integração futura) |
| **finalizado** | **financeiro.atendimento** | **SIM** | **Cobrar/faturar** |
| finalizado | suporte.atendimento | **NÃO** | (Integração futura) |
| cancelado | suporte.atendimento | **NÃO** | (Integração futura) |

### 8.2 Implementação de Integração

Para o evento `finalizado` → `financeiro.atendimento`:

1. Event Handler assíncrono detecta evento `finalizado`
2. Publica mensagem no RabbitMQ:
   - Exchange: `assistencia.atendimento.exchange`
   - Routing Key: `assistencia.atendimento.finalizado` (ou similar)
3. Serviço `financeiro.atendimento` consome mensagem
4. Processa cobrança/faturamento

---

## 9. Diferenças com Implementação Atual

### 9.1 Comando "ocorrencia" (AUSENTE)

**Status**: NÃO IMPLEMENTADO na versão atual.

**Impacto**:
- Estado `OCORRIDO` não existe
- Não é possível registrar ocorrências durante atendimento
- Fluxo vai direto de `CONFIRMADO` para `FINALIZADO`

**Ação Necessária**: Implementar comando, evento e estado.

---

### 9.2 Estado "OCORRIDO" (AUSENTE)

**Status**: NÃO IMPLEMENTADO.

**Ação Necessária**:
- Adicionar `OCORRIDO` no enum `Status`
- Implementar `OcorrenciaCommand`
- Implementar `OcorridoEvent`
- Adicionar métodos `process(OcorrenciaCommand)` e `apply(OcorridoEvent)` no agregado

---

### 9.3 Integração Seletiva por Evento (DIFERENTE)

**Implementação Atual**: Todos os eventos são enviados para RabbitMQ de forma genérica.

**Especificação**: Apenas `finalizado` deve acionar `financeiro.atendimento`.

**Ação Necessária**:
- Implementar filtro no Event Handler
- Configurar routing keys específicos por tipo de evento
- Configurar exchanges/queues para diferentes destinos

---

### 9.4 Campo "protocolo" (AUSENTE)

**Status**: Definido na spec como atributo do comando `solicitar`, mas NÃO IMPLEMENTADO.

**Ação Necessária**:
- Adicionar campo `protocolo` no comando `SolicitarCommand`
- Adicionar no evento `SolicitadoEvent`
- Adicionar no conceito `Atendimento`
- Gerar protocolo automaticamente ou receber como parâmetro

---

### 9.5 Tenant ID (NÃO UTILIZADO)

**Especificação**: Define `tenantId` para Write e Read Models.

**Implementação Atual**: Não há suporte explícito para multi-tenancy.

**Ação Necessária** (se necessário):
- Adicionar `tenantId` nas tabelas ES_AGGREGATE e ES_EVENT
- Filtrar queries por tenant
- Isolar dados por tenant

---

## 10. Mapeamento para Implementação

### 10.1 Classes Java a Criar/Modificar

#### Commands
- [x] `SolicitarCommand` (já existe, verificar campo `protocolo`)
- [x] `AjustarCommand` (já existe)
- [x] `ConfirmarCommand` (já existe)
- [ ] `OcorrenciaCommand` **(CRIAR)**
- [x] `FinalizarCommand` (já existe)
- [x] `CancelarCommand` (já existe)

#### Events
- [x] `SolicitadoEvent` (já existe, verificar campo `protocolo`)
- [x] `AjustadoEvent` (já existe)
- [x] `ConfirmadoEvent` (já existe)
- [ ] `OcorridoEvent` **(CRIAR)**
- [x] `FinalizadoEvent` (já existe)
- [x] `CanceladoEvent` (já existe)

#### Value Objects
- [x] `ClienteRef` (já existe)
- [x] `VeiculodeclienteRef` (já existe)
- [x] `ServicoRef` (já existe)
- [x] `PrestadorRef` (já existe)
- [x] `Endereco` (já existe)
- [x] `DocFiscal` (já existe)
- [x] `Itemdeatendimento` (já existe)

#### Conceito Principal
- [x] `Atendimento` (já existe, adicionar campo `protocolo` e estado `OCORRIDO`)

#### Aggregate
- [x] `AtendimentoAggregate` (modificar para adicionar comando `ocorrencia`)

#### Enums
- [ ] `AtendimentoCommandType` (adicionar `OCORRENCIA`)
- [ ] `EventType` (adicionar `OCORRIDO`)
- [ ] `Atendimento.Status` (adicionar `OCORRIDO`)

---

### 10.2 Estrutura de Banco de Dados

#### Tabelas Event Sourcing (já existem)
- `ES_AGGREGATE`
- `ES_EVENT`
- `ES_AGGREGATE_SNAPSHOT`
- `ES_EVENT_SUBSCRIPTION`

#### Projection (Read Model)
- `AtendimentoProjection` (adicionar campo `ocorridoem` e lista de `ocorrencias`)

---

### 10.3 REST Endpoints

| Endpoint | Método | Comando | Status |
|----------|--------|---------|--------|
| `/solicitar` | POST | SolicitarCommand | Implementado |
| `/ajustar` | PUT | AjustarCommand | Implementado |
| `/confirmar` | PUT | ConfirmarCommand | Implementado |
| `/ocorrencia` | PUT | OcorrenciaCommand | **A CRIAR** |
| `/finalizar` | PUT | FinalizarCommand | Implementado |
| `/cancelar` | PUT | CancelarCommand | Implementado |
| `/{uuid}` | GET | - (query) | Implementado |

---

## 11. Checklist de Implementação

### Fase 1: Adicionar Comando "ocorrencia"
- [ ] Criar `OcorrenciaCommand.java`
- [ ] Criar `OcorridoEvent.java`
- [ ] Adicionar `OCORRIDO` em `Atendimento.Status`
- [ ] Adicionar método `process(OcorrenciaCommand)` em `AtendimentoAggregate`
- [ ] Adicionar método `apply(OcorridoEvent)` em `AtendimentoAggregate`
- [ ] Adicionar lista `ocorrencias` em `Atendimento.java`
- [ ] Adicionar `OCORRENCIA` em `AtendimentoCommandType`
- [ ] Adicionar `OCORRIDO` em `EventType`

### Fase 2: Adicionar Campo "protocolo"
- [ ] Adicionar `protocolo` em `SolicitarCommand`
- [ ] Adicionar `protocolo` em `SolicitadoEvent`
- [ ] Adicionar `protocolo` em `Atendimento`
- [ ] Adicionar `protocolo` em `AtendimentoDTO`
- [ ] Implementar geração automática de protocolo (UUID ou sequencial)

### Fase 3: Implementar Integração Seletiva
- [ ] Criar `FinanceiroIntegrationEventSender` (envia apenas evento `finalizado`)
- [ ] Configurar routing key específico para financeiro
- [ ] Configurar exchange/queue no RabbitMQ
- [ ] Testar integração com serviço financeiro (mock se necessário)

### Fase 4: Atualizar REST Controller
- [ ] Adicionar endpoint `POST /ocorrencia`
- [ ] Criar `OcorrenciaDTO`
- [ ] Testar fluxo completo

### Fase 5: Atualizar Projection
- [ ] Adicionar campo `ocorridoem` (Timestamp)
- [ ] Adicionar lista `ocorrencias` (array de strings)
- [ ] Atualizar `AtendimentoProjectionServiceImpl` para processar `OcorridoEvent`

### Fase 6: Testes
- [ ] Testes unitários do agregado (comando `ocorrencia`)
- [ ] Testes de integração (fluxo completo com ocorrências)
- [ ] Testes de API REST
- [ ] Testes de integração com financeiro

---

## 12. Exemplo de Fluxo Completo (Com Ocorrência)

### 12.1 Solicitar Atendimento
```bash
POST /solicitar
{
  "cliente": {"id": 123, "nome": "João Silva", "docfiscal": {"tipo": "CPF", "numero": "12345678900"}},
  "veiculo": {"placa": "ABC1234"},
  "servico": {"id": 1, "nome": "Reboque"},
  "tipodeocorrencia": "Pane elétrica",
  "base": {"tipo": "RESIDENCIAL", "logradouro": "Rua A", "numero": "10", "bairro": "Centro", "cidade": "São Paulo", "estado": "SP", "cep": "01310100"},
  "origem": {"tipo": "COMERCIAL", "logradouro": "Av B", "numero": "200", "bairro": "Jardins", "cidade": "São Paulo", "estado": "SP", "cep": "01310200"}
}

Resposta: {"id": "550e8400-e29b-41d4-a716-446655440000"}
```

### 12.2 Ajustar Atendimento
```bash
PUT /ajustar
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "prestador": {"id": 456, "nome": "Prestadora XYZ", "docfiscal": {"tipo": "CNPJ", "numero": "12345678000190"}},
  "destino": {"tipo": "COMERCIAL", "logradouro": "Rua C", "numero": "50", "bairro": "Mooca", "cidade": "São Paulo", "estado": "SP", "cep": "03180000"},
  "items": [
    {"nome": "Reboque", "unidadedemedida": "km", "precounitario": 500, "quantidade": 15, "observacao": "15km percorridos"},
    {"nome": "Taxa de saída", "unidadedemedida": "unidade", "precounitario": 5000, "quantidade": 1}
  ],
  "descricao": "Ajuste: definido prestador e itens de cobrança"
}
```

### 12.3 Confirmar Atendimento
```bash
PUT /confirmar
{
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 12.4 Registrar Ocorrência (NOVO)
```bash
PUT /ocorrencia
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "ocorrencias": [
    "Cliente não estava no local, aguardado 15 minutos",
    "Veículo mais pesado que informado, necessário reboque maior"
  ]
}
```

### 12.5 Registrar Segunda Ocorrência
```bash
PUT /ocorrencia
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "ocorrencias": [
    "Trânsito intenso na Marginal, atraso de 30 minutos"
  ]
}
```

### 12.6 Finalizar Atendimento
```bash
PUT /finalizar
{
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Resultado**:
- Evento `finalizado` é persistido
- Event Handler envia mensagem para `financeiro.atendimento`
- Serviço financeiro processa cobrança

---

## 13. Resumo das Mudanças Necessárias

| Item | Status Atual | Ação |
|------|--------------|------|
| Comando `ocorrencia` | ❌ Ausente | ✅ Criar |
| Evento `ocorrido` | ❌ Ausente | ✅ Criar |
| Estado `OCORRIDO` | ❌ Ausente | ✅ Adicionar no enum |
| Campo `protocolo` | ❌ Ausente | ✅ Adicionar |
| Integração seletiva | ⚠️ Genérica | ✅ Filtrar por evento |
| Multi-tenancy | ❌ Ausente | ⏸️ Opcional (não prioritário) |

---

**Fim do documento de mapeamento da especificação.**
