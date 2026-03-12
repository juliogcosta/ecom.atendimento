# Comandos do Agregado Atendimento

Especificação detalhada dos 6 comandos conforme `domain.aggregates.spec`.

---

## 1. Comando: SOLICITAR

Cria um novo atendimento.

### Metadados
- **Estado Inicial**: `null` (agregado não existe)
- **Estado Final**: `solicitado`
- **Roles**: `MASTER`, `ATENDENTE`, `GERENTE`
- **Pode ser executado múltiplas vezes**: ❌ Não (apenas na criação)

### Dados do Comando

#### Atributos Simples

| Campo | Tipo | Tamanho | Obrigatório | Descrição |
|-------|------|---------|-------------|-----------|
| tipodeocorrencia | String | 100 | Sim | Tipo de ocorrência reportada |
| protocolo | String | 64 | Sim | Chave de protocolo de atendimento |
| status | String | 32 | Sim | Estado atual (será "solicitado") |

#### Value Objects Single

**cliente** (quem solicitou o atendimento)
| Campo | Tipo | Tamanho | Obrigatório |
|-------|------|---------|-------------|
| id | Long | - | Sim |
| nome | String | 200 | Sim |
| docfiscal | Json | - | Sim |

**veiculo** (veículo a ser atendido)
| Campo | Tipo | Tamanho | Obrigatório |
|-------|------|---------|-------------|
| placa | String | 10 | Sim |

**servico** (tipo de serviço solicitado)
| Campo | Tipo | Tamanho | Obrigatório |
|-------|------|---------|-------------|
| id | Long | - | Sim |
| nome | String | 100 | Sim |

**base** (endereço da base de atendimento)
| Campo | Tipo | Tamanho | Obrigatório |
|-------|------|---------|-------------|
| tipo | String | 20 | Sim |
| logradouro | String | 200 | Sim |
| numero | String | 10 | Sim |
| complemento | String | 100 | Não |
| bairro | String | 100 | Sim |
| cidade | String | 100 | Sim |
| estado | String | 2 | Sim (UF) |
| cep | String | 8 | Sim (sem hífen) |

**origem** (onde está o cliente/veículo)
- Mesma estrutura de **base**

### Exemplo JSON

```json
{
  "tipodeocorrencia": "Pane elétrica",
  "protocolo": "ATD-2025-001234",
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
}
```

---

## 2. Comando: AJUSTAR

Ajusta dados do atendimento. Pode ser executado múltiplas vezes.

### Metadados
- **Estados Iniciais**: `solicitado`, `ajustado`
- **Estado Final**: `ajustado`
- **Roles**: `MASTER`, `ATENDENTE`, `GERENTE`
- **Pode ser executado múltiplas vezes**: ✅ Sim

### Dados do Comando

#### Atributos Simples

| Campo | Tipo | Tamanho | Obrigatório | Descrição |
|-------|------|---------|-------------|-----------|
| descricao | Text | - | Não | Descrição do ajuste |
| status | String | 32 | Sim | Estado (será "ajustado") |

#### Value Objects Single

**prestador** (quem executará o serviço)
| Campo | Tipo | Tamanho | Obrigatório |
|-------|------|---------|-------------|
| id | Long | - | Sim |
| nome | String | 200 | Sim |
| docfiscal | Json | - | Sim |

**servico** (pode ser alterado)
| Campo | Tipo | Tamanho | Obrigatório |
|-------|------|---------|-------------|
| id | Long | - | Não |
| nome | String | 100 | Não |

**origem** (pode ser alterado - todos os campos opcionais)
- Mesma estrutura de endereço, mas todos os campos são nullable

**destino** (para onde o veículo será levado - obrigatório)
| Campo | Tipo | Tamanho | Obrigatório |
|-------|------|---------|-------------|
| tipo | String | 20 | Sim |
| logradouro | String | 200 | Sim |
| numero | String | 10 | Sim |
| complemento | String | 100 | Não |
| bairro | String | 100 | Sim |
| cidade | String | 100 | Sim |
| estado | String | 2 | Sim |
| cep | String | 8 | Sim |

#### Value Objects Multiple

**items** (itens de cobrança - array)
| Campo | Tipo | Tamanho | Obrigatório | Descrição |
|-------|------|---------|-------------|-----------|
| nome | String | 200 | Sim | Nome do item |
| unidadedemedida | String | 20 | Sim | Ex: km, hora, unidade |
| precounitario | Integer | - | Sim | Em centavos |
| quantidade | Integer | - | Sim | Quantidade |
| observacao | String | 500 | Não | Observações |

### Exemplo JSON

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "descricao": "Ajuste: definido prestador e itens de cobrança",
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
}
```

---

## 3. Comando: CONFIRMAR

Confirma o atendimento após ajustes.

### Metadados
- **Estados Iniciais**: `ajustado`, `solicitado`
- **Estado Final**: `confirmado`
- **Roles**: `MASTER`, `ATENDENTE`, `GERENTE`
- **Pode ser executado múltiplas vezes**: ❌ Não

### Dados do Comando

Apenas o ID do agregado. Não há dados adicionais.

### Exemplo JSON

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## 4. Comando: OCORRENCIA (NOVO)

Registra ocorrências durante o atendimento. Pode ser executado múltiplas vezes.

### Metadados
- **Estados Iniciais**: `confirmado`, `ocorrido`
- **Estado Final**: `ocorrido`
- **Roles**: `MASTER`, `ATENDENTE`, `GERENTE`
- **Pode ser executado múltiplas vezes**: ✅ Sim

### Dados do Comando

#### Value Objects Multiple

**ocorrencias** (array de strings)
- Cada elemento é um texto descrevendo uma ocorrência

### Exemplo JSON

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "ocorrencias": [
    "Cliente não estava no local, aguardado 15 minutos",
    "Veículo mais pesado que informado, necessário reboque maior"
  ]
}
```

### Observações
- Ocorrências são **acumulativas**: cada comando adiciona à lista existente
- Útil para rastrear problemas durante execução do serviço

---

## 5. Comando: FINALIZAR

Finaliza o atendimento.

### Metadados
- **Estados Iniciais**: `confirmado`, `ocorrido`
- **Estado Final**: `finalizado`
- **Roles**: `MASTER`, `ATENDENTE`, `GERENTE`
- **Pode ser executado múltiplas vezes**: ❌ Não

### Dados do Comando

Apenas o ID do agregado.

### Exemplo JSON

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Integração
⚠️ Este evento aciona integração com `financeiro.atendimento` (conforme spec).

---

## 6. Comando: CANCELAR

Cancela o atendimento.

### Metadados
- **Estados Iniciais**: `solicitado`, `ajustado`
- **Estado Final**: `cancelado`
- **Roles**: `MASTER`, `ATENDENTE`, `GERENTE`
- **Pode ser executado múltiplas vezes**: ❌ Não

### Dados do Comando

Apenas o ID do agregado.

### Exemplo JSON

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Restrição
❌ Não é possível cancelar atendimento após confirmado.

---

## Resumo Comparativo

| Comando | From State | To State | Dados Complexos | Múltiplo |
|---------|-----------|----------|-----------------|----------|
| solicitar | null | solicitado | cliente, veiculo, servico, base, origem | ❌ |
| ajustar | solicitado, ajustado | ajustado | prestador, destino, items[] | ✅ |
| confirmar | solicitado, ajustado | confirmado | - | ❌ |
| ocorrencia | confirmado, ocorrido | ocorrido | ocorrencias[] | ✅ |
| finalizar | confirmado, ocorrido | finalizado | - | ❌ |
| cancelar | solicitado, ajustado | cancelado | - | ❌ |

---

**Arquivos relacionados**:
- [Máquina de Estados](state-machine.md) - Diagrama de transições
- [Eventos](events.md) - Eventos gerados por cada comando
- [Value Objects](value-objects.md) - Detalhes dos VOs

**Referência**: `domain.aggregates.spec` linhas 24-557
