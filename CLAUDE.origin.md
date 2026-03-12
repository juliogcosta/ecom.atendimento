# Instruções para Claude - Projeto Microservice CQRS/ES Atendimento

Este arquivo orienta como Claude deve interagir com este projeto e sua documentação.

---

## 📌 Sobre Este Projeto

### Objetivo

**Construir um microservice para gerenciar criação de instâncias de agregados de Atendimento e eventos relacionados.**

**Contexto de Negócio**: Sistema para registrar atendimentos a clientes que estão com seus veículos necessitando de suporte (assistência veicular).

### Especificações Técnicas

**Nome**: assistencia-atendimento-es
**Padrão**: CQRS/Event Sourcing
**Framework**: Spring Boot 3.4.2
**Linguagem**: Java 17
**Build**: Maven
**Database**: PostgreSQL (Event Store + Projections)
**Messaging**: RabbitMQ
**Migrations**: Flyway (para criação do schema e tabelas)

### Banco de Dados - Implantação Obrigatória

⚠️ **O banco de dados PostgreSQL também precisa ser implantado** com as seguintes estruturas:

**Schema**: `assistencia_es` (ou nome definido na spec)

**Tabelas do Event Store** (Event Sourcing):
1. `ES_AGGREGATE` - Registro dos agregados (ID, versão, tipo)
2. `ES_EVENT` - Log de eventos (event store principal)
3. `ES_AGGREGATE_SNAPSHOT` - Snapshots para otimização
4. `ES_EVENT_SUBSCRIPTION` - Checkpoint para Transactional Outbox

**Referência para Migrations**:
📁 `/home/julio/Codes/YC/Experiments/comigo/assistencia.atendimento/src/main/resources/db/migration/`

Arquivos de referência:
- `V0__create_schema_assistencia_es.sql` - Cria schema
- `V1__eventsourcing_tables.sql` - Cria tabelas ES_AGGREGATE, ES_EVENT, ES_AGGREGATE_SNAPSHOT, ES_EVENT_SUBSCRIPTION
- `V2__notify_trigger.sql` - Cria trigger para PostgreSQL LISTEN/NOTIFY (processamento assíncrono)

**Observação**: Use estes arquivos como base, adaptando nomes conforme necessário (ex: schema name).

### Dependências Obrigatórias

⚠️ **IMPORTANTE**: Este projeto deve usar **ESTRITAMENTE** as seguintes bibliotecas:

**1. core-cqrs** (Framework CQRS/ES customizado)
- **Localização**: `/home/julio/Codes/YC/Experiments/comigo/core-cqrs/`
- **Função**: Framework base para Event Sourcing (Aggregate, Command, Event, Repositories)
- **Uso**: Deve ser importado como dependência Maven

**2. core-common** (Bibliotecas comuns)
- **Localização**: `/home/julio/Codes/YC/Experiments/comigo/core-common/`
- **Função**: Utilitários, exceptions, tipos comuns
- **Uso**: Deve ser importado como dependência Maven

**Observação**: Se possível, estas libs devem ser importadas para o contexto do novo projeto. Se não for possível via Maven, considerar copiá-las para o workspace local.

### Projeto de Referência

**Base para implementação**: `/home/julio/Codes/YC/Experiments/comigo/assistencia.atendimento/`

Este é um projeto **bastante semelhante** que deve servir como **ponto de partida** para:
- Estrutura de pacotes (Clean Architecture)
- Implementação de Agregados, Commands e Events
- Configuração do Spring Boot
- Migrations de banco de dados
- Controllers REST
- Event Handlers
- Integração com RabbitMQ

### Estratégia de Implementação

```
┌──────────────────────────────────────────────────────────────┐
│  1. Configurar Banco de Dados PostgreSQL                     │
│     - Copiar migrations de ../comigo/assistencia.atendimento │
│     - Executar Flyway migrations (V0, V1, V2)                │
│     - Verificar criação do schema e tabelas ES_*             │
└───────────────────────────┬──────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  2. Estudar ../comigo/core-cqrs/                             │
│     (Framework base - classes abstratas, repositórios)       │
└───────────────────────────┬──────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  3. Analisar ../comigo/assistencia.atendimento/              │
│     (Implementação de referência - padrões e estrutura)      │
└───────────────────────────┬──────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  4. Consultar domain.aggregates.spec                         │
│     (Especificação do novo agregado - comandos, eventos)     │
└───────────────────────────┬──────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  5. Implementar novo projeto MELHORADO                       │
│     - Configurar pom.xml com core-cqrs e core-common         │
│     - Seguir estrutura de assistencia.atendimento            │
│     - Copiar/adaptar migrations para o banco                 │
│     - Adicionar melhorias identificadas na spec              │
│     - Corrigir diferenças (comando "ocorrencia", etc)        │
└──────────────────────────────────────────────────────────────┘
```

### Diferencial Deste Projeto

Este projeto é uma **evolução** do `assistencia.atendimento` com:
- ✅ Implementação do comando "ocorrencia" (ausente no projeto de referência)
- ✅ Estado "OCORRIDO" na máquina de estados
- ✅ Campo "protocolo" para identificação de negócio
- ✅ Integração seletiva (apenas evento "finalizado" → financeiro)
- ✅ Aderência estrita à especificação em `domain.aggregates.spec`
- ✅ Documentação completa e fragmentada

---

## 📚 Documentação do Projeto

### Localização da Documentação

Toda a documentação técnica está em **`/docs/`** e é organizada de forma **fragmentada por tópicos**.

### Índice Principal: docs/SpecROOT.md

**Sempre comece por aqui** para navegar pela documentação:

```bash
Read /home/julio/Codes/YC/Experiments/Exp/docs/SpecROOT.md
```

O `SpecROOT.md` é o **índice central** que lista todos os tópicos disponíveis organizados em:
- **01-architecture/** - Arquitetura CQRS/ES e framework core-cqrs
- **02-specification/** - Especificação do domínio (comandos, eventos, estados)
- **03-implementation/** - Exemplos de código e estrutura do projeto
- **04-reference/** - Diferenças, checklists, exemplos práticos

---

## 🎯 Princípios de Carregamento de Documentação

### ⚠️ IMPORTANTE: Carregamento Seletivo

**NÃO carregue toda a documentação de uma vez**. Use carregamento seletivo por tópico:

**❌ Errado**:
```bash
Read docs/CQRS_ES_ARCHITECTURE.md  # Arquivo consolidado muito grande
Read docs/SPEC_MAPPING.md           # Arquivo consolidado muito grande
Read docs/PROJECT_DESIGN.md         # Arquivo consolidado muito grande
```

**✅ Correto**:
```bash
# 1. Leia o índice
Read docs/SpecROOT.md

# 2. Identifique o tópico necessário

# 3. Carregue apenas o arquivo específico
Read docs/01-architecture/event-sourcing.md
# ou
Read docs/04-reference/differences.md
# ou
Read docs/02-specification/commands.md
```

### Workflow Recomendado

```
┌─────────────────────────────────────────────┐
│  1. Usuário faz pergunta                    │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│  2. Claude identifica tópico relevante      │
│     (ex: "como funciona Event Sourcing?")   │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│  3. Claude consulta docs/SpecROOT.md        │
│     para localizar arquivo correto          │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│  4. Claude carrega APENAS arquivo(s)        │
│     específico(s) do tópico                 │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│  5. Claude responde com base no conteúdo    │
│     carregado                               │
└─────────────────────────────────────────────┘
```

---

## 📖 Exemplos de Uso

### Exemplo 1: Usuário pergunta sobre Event Sourcing

**Pergunta**: "Como funciona o Event Sourcing neste projeto?"

**Ações do Claude**:
```bash
# Passo 1: Consultar índice (opcional, se já conhece estrutura)
Read docs/SpecROOT.md

# Passo 2: Carregar tópico específico
Read docs/01-architecture/event-sourcing.md

# Passo 3: Responder com base no conteúdo carregado
```

---

### Exemplo 2: Usuário pergunta sobre diferenças entre spec e implementação

**Pergunta**: "Quais as diferenças entre a especificação e o código atual?"

**Ações do Claude**:
```bash
# Carregar arquivo de diferenças
Read docs/04-reference/differences.md

# Responder com lista de diferenças
```

---

### Exemplo 3: Usuário quer implementar comando "ocorrencia"

**Pergunta**: "Como implementar o comando de ocorrência?"

**Ações do Claude**:
```bash
# Passo 1: Entender o comando na spec
Read docs/02-specification/commands.md

# Passo 2: Ver checklist de implementação
Read docs/04-reference/implementation-checklist.md

# Passo 3: Orientar implementação passo a passo
```

---

### Exemplo 4: Usuário pergunta sobre estrutura do projeto

**Pergunta**: "Como está organizado o código do projeto?"

**Ações do Claude**:
```bash
# Carregar estrutura de pacotes
Read docs/03-implementation/project-structure.md
```

---

## 🗂️ Estrutura de Diretórios

```
/home/julio/Codes/YC/Experiments/Exp/
│
├── CLAUDE.md                          ← Você está aqui (instruções)
├── domain.aggregates.spec             ← Especificação JSON original
│
├── docs/                              ← Documentação técnica
│   ├── SpecROOT.md                    ← ÍNDICE PRINCIPAL (comece aqui)
│   │
│   ├── 01-architecture/               ← Arquitetura e conceitos
│   │   ├── event-sourcing.md
│   │   ├── aggregate-pattern.md
│   │   ├── command-processor.md
│   │   └── database-schema.md
│   │
│   ├── 02-specification/              ← Especificação do domínio
│   │   ├── overview.md
│   │   ├── commands.md
│   │   ├── events.md
│   │   ├── value-objects.md
│   │   └── state-machine.md
│   │
│   ├── 03-implementation/             ← Código e implementação
│   │   ├── project-structure.md
│   │   ├── aggregate-implementation.md
│   │   ├── commands-events.md
│   │   ├── controllers.md
│   │   └── configuration.md
│   │
│   └── 04-reference/                  ← Referências e checklists
│       ├── differences.md
│       ├── implementation-checklist.md
│       ├── examples.md
│       └── patterns.md
│
└── src/                               ← Código-fonte (quando criado)
    ├── main/
    └── test/
```

---

## 🔍 Quando Consultar Código vs Documentação

### Consultar Documentação quando:
- ✅ Entender conceitos e arquitetura
- ✅ Ver especificação de comandos/eventos
- ✅ Comparar spec vs implementação atual
- ✅ Seguir checklist de implementação
- ✅ Ver exemplos e padrões

### Consultar Código quando:
- ✅ Ver implementação atual detalhada
- ✅ Debugar problema específico
- ✅ Verificar detalhes de configuração
- ✅ Analisar testes existentes

---

## 📋 Arquivos de Referência Rápida

Para economia de tempo, aqui estão os arquivos mais consultados:

| Necessidade | Arquivo |
|-------------|---------|
| Índice geral | `docs/SpecROOT.md` |
| O que é Event Sourcing | `docs/01-architecture/event-sourcing.md` |
| Lista de comandos | `docs/02-specification/commands.md` |
| Diferenças spec vs código | `docs/04-reference/differences.md` |
| Como implementar | `docs/04-reference/implementation-checklist.md` |

---

## ⚙️ Convenções do Projeto

### Nomenclatura
- **Packages**: `br.com.yc.ecomigo.assistencia.*`
- **Agregado**: `AtendimentoAggregate`
- **Commands**: `XxxxxCommand` (ex: `SolicitarCommand`)
- **Events**: `XxxxxEvent` (ex: `SolicitadoEvent`)
- **Value Objects**: Records (ex: `ClienteRef`, `Endereco`)

### Padrões de Código
- **Imutabilidade**: Events e VOs são imutáveis
- **Builder Pattern**: Para criação de Events
- **Reflexão**: Métodos `process()` e `apply()` descobertos dinamicamente
- **Estado derivado**: Agregado reconstitui estado via replay de eventos

### Estados do Agregado
```
null → SOLICITADO → AJUSTADO → CONFIRMADO → OCORRIDO → FINALIZADO
                        ↓            ↓
                   CANCELADO    CANCELADO
```

---

## 🚨 Regras Importantes

### 1. Sempre Pergunte Antes de Modificar
Se não tiver certeza sobre uma implementação, **pergunte ao usuário antes de fazer mudanças**.

### 2. Siga a Especificação
A especificação em `domain.aggregates.spec` é a fonte da verdade. Em caso de dúvida, consulte:
```bash
Read /home/julio/Codes/YC/Experiments/Exp/domain.aggregates.spec
```

### 3. Mantenha Documentação Atualizada
Ao fazer mudanças significativas, atualize a documentação correspondente em `docs/`.

### 4. Use Fragmentação
Ao criar nova documentação, siga o padrão fragmentado:
- Um tópico = um arquivo
- Máximo 500 linhas por arquivo
- Linkagem entre documentos relacionados

---

## 🛠️ Comandos Úteis

### Ver estrutura de diretórios
```bash
tree /home/julio/Codes/YC/Experiments/Exp/docs -L 2
```

### Buscar por termo na documentação
```bash
grep -r "Event Sourcing" /home/julio/Codes/YC/Experiments/Exp/docs/
```

### Listar todos os arquivos de especificação
```bash
find /home/julio/Codes/YC/Experiments/Exp/docs -name "*.md" -type f
```

### Ver migrations de referência (Banco de Dados)
```bash
ls -la /home/julio/Codes/YC/Experiments/comigo/assistencia.atendimento/src/main/resources/db/migration/
```

### Ler migration específica
```bash
# Schema
cat /home/julio/Codes/YC/Experiments/comigo/assistencia.atendimento/src/main/resources/db/migration/V0__create_schema_assistencia_es.sql

# Tabelas Event Sourcing
cat /home/julio/Codes/YC/Experiments/comigo/assistencia.atendimento/src/main/resources/db/migration/V1__eventsourcing_tables.sql

# Trigger PostgreSQL LISTEN/NOTIFY
cat /home/julio/Codes/YC/Experiments/comigo/assistencia.atendimento/src/main/resources/db/migration/V2__notify_trigger.sql
```

---

## 📞 Comunicação com o Usuário

### Seja Explícito
Ao referenciar arquivos, sempre use **caminho completo**:
- ✅ "Conforme definido em `/home/julio/Codes/YC/Experiments/Exp/docs/02-specification/commands.md:45`"
- ❌ "Conforme definido na especificação"

### Informe o Que Está Carregando
Ao carregar documentação, informe ao usuário:
```
"Vou consultar a especificação de comandos..."
Read docs/02-specification/commands.md
"Encontrei a definição do comando 'ocorrencia'..."
```

### Peça Confirmação
Antes de implementações complexas, apresente o plano:
```
"Baseado na spec, vou implementar:
1. Criar OcorrenciaCommand
2. Criar OcorridoEvent
3. Adicionar estado OCORRIDO
4. Implementar métodos process/apply

Posso prosseguir?"
```

---

## 📝 Histórico de Sessões

Ao retomar uma sessão, sempre:
1. Ler `CLAUDE.md` (este arquivo)
2. Consultar `docs/SpecROOT.md` para relembrar estrutura
3. Perguntar ao usuário onde parou

---

## 🎓 Contexto de Aprendizado

Este projeto foi criado com finalidade de:
- **Aprender** o padrão CQRS/Event Sourcing
- **Documentar** o conhecimento adquirido
- **Implementar** um microservice seguindo boas práticas
- **Comparar** especificação vs implementação existente

A documentação serve tanto para consulta quanto para aprendizado.

---

## 🔗 Links Rápidos

### Especificação e Documentação
- **Especificação Original**: `./domain.aggregates.spec`
- **Índice da Documentação**: `docs/SpecROOT.md`

### Bibliotecas Obrigatórias (Usar ESTRITAMENTE)
- **core-cqrs**: `/home/julio/Codes/YC/Experiments/comigo/core-cqrs/`
- **core-common**: `/home/julio/Codes/YC/Experiments/comigo/core-common/`

### Projeto de Referência (Base de Implementação)
- **assistencia.atendimento**: `/home/julio/Codes/YC/Experiments/comigo/assistencia.atendimento/`

### Estrutura de Referência
```
/home/julio/Codes/YC/Experiments/comigo/
├── core-cqrs/                    ← Framework CQRS/ES (USAR)
├── core-common/                  ← Bibliotecas comuns (USAR)
└── assistencia.atendimento/      ← Implementação de referência (COPIAR ESTRUTURA)
```

---

**Última atualização**: 2026-03-10
**Versão**: 1.0
