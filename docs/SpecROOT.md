# Documentação do Projeto - Microservice CQRS/ES Atendimento

Este é o arquivo raiz da documentação. Use este índice para navegar pelos tópicos de interesse e carregar apenas as informações necessárias.

---

## 📚 Índice de Tópicos

### 1. Arquitetura CQRS/Event Sourcing

**Quando usar**: Para entender o framework core-cqrs, padrões arquiteturais e funcionamento interno do Event Sourcing.

- **[Core CQRS Framework](01-architecture/core-cqrs-framework.md)**
  - Classes base: Aggregate, Command, Event
  - Repositórios: EventRepository, AggregateRepository
  - Interfaces e contratos principais

- **[Aggregate Pattern](01-architecture/aggregate-pattern.md)**
  - Como funcionam os agregados
  - Métodos process() e apply()
  - Versionamento e optimistic locking
  - Reconstitução de estado via eventos

- **[Command Processor](01-architecture/command-processor.md)**
  - Fluxo completo de processamento
  - Command Handlers
  - Event Handlers (Sync/Async)
  - Transactional Outbox Pattern

- **[Event Sourcing Concepts](01-architecture/event-sourcing.md)**
  - Princípios do Event Sourcing
  - Snapshots e otimização
  - Event Store vs Read Model
  - Replay de eventos

- **[Database Schema](01-architecture/database-schema.md)**
  - Tabelas do Event Store
  - Índices e otimizações
  - Triggers e notificações
  - Migrations

---

### 2. Especificação do Domínio

**Quando usar**: Para consultar a especificação do agregado Atendimento conforme definido em `domain.aggregates.spec`.

- **[Overview da Especificação](02-specification/overview.md)**
  - Bounded Context: assistencia
  - Agregado: atendimento
  - Metadados do projeto
  - Schemas (Write/Read Model)

- **[Comandos](02-specification/commands.md)**
  - solicitar (criação)
  - ajustar (múltiplas vezes)
  - confirmar
  - ocorrencia (NOVO)
  - finalizar
  - cancelar
  - Dados e validações de cada comando

- **[Eventos](02-specification/events.md)**
  - solicitado
  - ajustado
  - confirmado
  - ocorrido (NOVO)
  - finalizado
  - cancelado
  - Triggers de integração (domainBus)

- **[Value Objects](02-specification/value-objects.md)**
  - ClienteRef, PrestadorRef
  - Endereco
  - DocFiscal
  - Itemdeatendimento
  - VeiculodeclienteRef, ServicoRef

- **[Máquina de Estados](02-specification/state-machine.md)**
  - Diagrama de transições
  - Tabela de estados e comandos
  - Estados terminais
  - Regras de negócio

---

### 3. Implementação

**Quando usar**: Para consultar como implementar componentes específicos do microservice.

- **[Estrutura do Projeto](03-implementation/project-structure.md)**
  - Organização de pacotes (Clean Architecture)
  - Camadas: Domain, Application, Infrastructure, Adapter
  - Convenções de nomenclatura

- **[Implementação do Agregado](03-implementation/aggregate-implementation.md)**
  - Código do AtendimentoAggregate
  - Métodos process() para cada comando
  - Métodos apply() para cada evento
  - Validações de estado

- **[Commands e Events](03-implementation/commands-events.md)**
  - Implementação de Commands
  - Implementação de Events
  - Builders e construtores
  - Enums (CommandType, EventType)

- **[Value Objects e Concepts](03-implementation/value-objects-impl.md)**
  - Implementação usando records
  - Validações de negócio
  - Classe Atendimento (estado do agregado)

- **[Controllers REST](03-implementation/controllers.md)**
  - AtendimentoController
  - Endpoints de comandos
  - Endpoint de query
  - DTOs de entrada/saída

- **[Event Handlers](03-implementation/event-handlers.md)**
  - FinanceiroIntegrationEventSender
  - ProjectionUpdater
  - Configuração de filtros por evento

- **[Configuração](03-implementation/configuration.md)**
  - pom.xml e dependências
  - application.yml
  - RabbitMQ configuration
  - Database configuration

---

### 4. Referência

**Quando usar**: Para consultar diferenças, checklists e exemplos práticos.

- **[Diferenças Spec vs Implementação Atual](04-reference/differences.md)**
  - Comando "ocorrencia" (ausente)
  - Estado "OCORRIDO" (ausente)
  - Campo "protocolo" (ausente)
  - Integração seletiva vs genérica
  - Multi-tenancy

- **[Checklist de Implementação](04-reference/implementation-checklist.md)**
  - Fase 1: Adicionar comando "ocorrencia"
  - Fase 2: Adicionar campo "protocolo"
  - Fase 3: Implementar integração seletiva
  - Fase 4: Atualizar REST Controller
  - Fase 5: Atualizar Projection
  - Fase 6: Testes

- **[Exemplos de Uso](04-reference/examples.md)**
  - Fluxo completo via REST API
  - Exemplos de payloads JSON
  - Casos de uso comuns
  - Testes manuais

- **[Padrões e Boas Práticas](04-reference/patterns.md)**
  - Separação de responsabilidades
  - Uso de reflexão
  - Optimistic locking
  - Imutabilidade
  - Builder pattern

---

## 🚀 Início Rápido

### Para estudar a arquitetura:
1. Leia [Event Sourcing Concepts](01-architecture/event-sourcing.md)
2. Entenda [Aggregate Pattern](01-architecture/aggregate-pattern.md)
3. Veja o [Command Processor](01-architecture/command-processor.md)

### Para implementar um novo agregado:
1. Consulte [Estrutura do Projeto](03-implementation/project-structure.md)
2. Siga o [Checklist de Implementação](04-reference/implementation-checklist.md)
3. Use como referência [Implementação do Agregado](03-implementation/aggregate-implementation.md)

### Para entender a especificação:
1. Comece pelo [Overview](02-specification/overview.md)
2. Estude os [Comandos](02-specification/commands.md)
3. Veja a [Máquina de Estados](02-specification/state-machine.md)

### Para comparar spec vs implementação:
1. Leia [Diferenças](04-reference/differences.md)
2. Veja exemplos em [Exemplos de Uso](04-reference/examples.md)

---

## 📋 Status da Documentação

| Tópico | Status | Último Update |
|--------|--------|---------------|
| Arquitetura Core CQRS | ✅ Completo | 2026-03-10 |
| Especificação Domínio | ✅ Completo | 2026-03-10 |
| Design Implementação | ✅ Completo | 2026-03-10 |
| Fragmentação Docs | 🔄 Em progresso | 2026-03-10 |

---

## 🔗 Arquivos Legacy (Consolidados)

Os seguintes arquivos contêm toda a documentação de forma consolidada. Use-os apenas se precisar de uma visão completa:

- `CQRS_ES_ARCHITECTURE.md` - Arquitetura completa (consolidado)
- `SPEC_MAPPING.md` - Especificação completa (consolidado)
- `PROJECT_DESIGN.md` - Design completo (consolidado)

**Recomendação**: Prefira usar os arquivos fragmentados por tópico para melhor performance de leitura.

---

## 📝 Como Usar Este Índice

### Carregamento Seletivo
Quando precisar de informações específicas:

1. Identifique o tópico de interesse neste índice
2. Carregue APENAS o(s) arquivo(s) relevante(s)
3. Evite carregar documentação desnecessária

### Exemplo de Workflow

**Cenário 1**: "Como implementar um novo comando?"
```
1. Carregue: 02-specification/commands.md (ver estrutura na spec)
2. Carregue: 03-implementation/commands-events.md (ver código de exemplo)
3. Implemente seguindo os exemplos
```

**Cenário 2**: "Como funciona o Event Sourcing?"
```
1. Carregue: 01-architecture/event-sourcing.md (conceitos)
2. Carregue: 01-architecture/database-schema.md (persistência)
3. Opcional: 01-architecture/aggregate-pattern.md (aplicação prática)
```

**Cenário 3**: "Quais diferenças entre spec e implementação atual?"
```
1. Carregue: 04-reference/differences.md (resumo executivo)
2. Se precisar detalhes: 02-specification/commands.md (comando "ocorrencia")
```

---

## 🛠️ Manutenção da Documentação

### Ao adicionar nova documentação:
1. Crie arquivo no diretório apropriado (01-04)
2. Adicione entrada neste índice com descrição clara
3. Mantenha arquivos pequenos (< 500 linhas)
4. Use links relativos entre documentos

### Princípios:
- **Fragmentação**: Um tópico = um arquivo
- **Coesão**: Tópicos relacionados no mesmo diretório
- **Navegabilidade**: Sempre ter links para tópicos relacionados
- **Atualização**: Manter status e datas atualizadas

---

**Última atualização**: 2026-03-10
**Versão**: 1.0
