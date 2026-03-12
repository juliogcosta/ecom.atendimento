# Design do Projeto - Microservice CQRS/ES Atendimento

## 1. Visão Geral do Projeto

**Nome**: assistencia-atendimento-es
**Tipo**: Microservice com padrão CQRS/Event Sourcing
**Framework**: Spring Boot 3.4.2 + core-cqrs
**Linguagem**: Java 17
**Build**: Maven
**Banco de Dados**: PostgreSQL (Event Store + Read Model)
**Mensageria**: RabbitMQ

---

## 2. Arquitetura Geral

### 2.1 Camadas

```
┌─────────────────────────────────────────────────────────────┐
│                     ADAPTER LAYER                            │
│  ┌───────────────────┐         ┌────────────────────────┐   │
│  │  REST Controllers │         │   RabbitMQ Producers   │   │
│  │   (Inbound Web)   │         │      (Outbound)        │   │
│  └───────────────────┘         └────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                           ↓  ↑
┌─────────────────────────────────────────────────────────────┐
│                   APPLICATION LAYER                          │
│  ┌─────────────────┐  ┌──────────────────┐                  │
│  │ Command Handler │  │  Event Handler   │                  │
│  │  (Use Cases)    │  │   (Integration)  │                  │
│  └─────────────────┘  └──────────────────┘                  │
│                                                               │
│            ┌────────────────────────────────┐                │
│            │     CommandProcessor (core)    │                │
│            │     AggregateStore (core)      │                │
│            └────────────────────────────────┘                │
└─────────────────────────────────────────────────────────────┘
                           ↓  ↑
┌─────────────────────────────────────────────────────────────┐
│                     DOMAIN LAYER                             │
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │    Aggregate     │  │  Value Objects   │                 │
│  │  (Atendimento)   │  │  (ClienteRef,    │                 │
│  │                  │  │   Endereco, etc) │                 │
│  └──────────────────┘  └──────────────────┘                 │
│                                                               │
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │    Commands      │  │     Events       │                 │
│  │  (6 comandos)    │  │   (6 eventos)    │                 │
│  └──────────────────┘  └──────────────────┘                 │
└─────────────────────────────────────────────────────────────┘
                           ↓  ↑
┌─────────────────────────────────────────────────────────────┐
│                 INFRASTRUCTURE LAYER                         │
│  ┌─────────────────────┐  ┌───────────────────────────┐    │
│  │  Event Repository   │  │  Aggregate Repository     │    │
│  │  (PostgreSQL JDBC)  │  │    (PostgreSQL JDBC)      │    │
│  └─────────────────────┘  └───────────────────────────┘    │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │           Spring Configuration & Properties         │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Estrutura de Pacotes (Clean Architecture)

```
src/main/java/br/com/yc/ecomigo/assistencia/
├── domain/
│   ├── aggregate/
│   │   ├── AggregateType.java                         # Enum: tipos de agregados
│   │   ├── EventType.java                             # Enum: tipos de eventos
│   │   ├── DefaultAggregateTypeMapper.java            # Mapper para core-cqrs
│   │   └── atendimento/
│   │       ├── AtendimentoAggregate.java              # Agregado principal
│   │       ├── AtendimentoCommandType.java            # Enum: tipos de comando
│   │       ├── command/
│   │       │   ├── SolicitarCommand.java
│   │       │   ├── AjustarCommand.java
│   │       │   ├── ConfirmarCommand.java
│   │       │   ├── OcorrenciaCommand.java             # NOVO
│   │       │   ├── FinalizarCommand.java
│   │       │   └── CancelarCommand.java
│   │       ├── event/
│   │       │   ├── SolicitadoEvent.java
│   │       │   ├── AjustadoEvent.java
│   │       │   ├── ConfirmadoEvent.java
│   │       │   ├── OcorridoEvent.java                 # NOVO
│   │       │   ├── FinalizadoEvent.java
│   │       │   └── CanceladoEvent.java
│   │       └── concept/                               # Value Objects
│   │           ├── Atendimento.java                   # Entidade principal (estado)
│   │           ├── ClienteRef.java                    # record
│   │           ├── VeiculodeclienteRef.java           # record
│   │           ├── ServicoRef.java                    # record
│   │           ├── PrestadorRef.java                  # record
│   │           ├── Endereco.java                      # record
│   │           ├── DocFiscal.java                     # record
│   │           ├── Itemdeatendimento.java             # record
│   │           └── Ocorrencia.java                    # record (NOVO)
│   │
│   ├── projection/                                     # Read Model
│   │   ├── AtendimentoProjection.java                 # Entidade JPA
│   │   ├── ClienteProjectionRef.java
│   │   ├── EnderecoProjection.java
│   │   ├── ItemdeatendimentoProjection.java
│   │   └── repository/
│   │       └── AtendimentoProjectionRepository.java   # Spring Data JPA
│   │
│   └── service/                                        # Domain Services (se necessário)
│
├── application/
│   ├── service/
│   │   ├── aggregate/atendimento/
│   │   │   ├── command/                               # Command Handlers (opcional)
│   │   │   │   ├── SolicitarCommandHandlerImpl.java
│   │   │   │   ├── AjustarCommandHandlerImpl.java
│   │   │   │   └── (outros handlers se necessário)
│   │   │   └── event/                                 # Event Handlers
│   │   │       ├── AtendimentoProjectionUpdater.java  # Atualiza Read Model
│   │   │       └── FinanceiroIntegrationEventSender.java # Envia para financeiro
│   │   │
│   │   └── projection/
│   │       └── AtendimentoProjectionServiceImpl.java  # Serviço de consulta
│   │
│   └── usecase/                                        # Use Cases (se necessário)
│       ├── aggregate/atendimento/
│       └── projection/
│
├── infrastructure/
│   ├── config/
│   │   ├── AppConfiguration.java                      # Configurações gerais
│   │   ├── RabbitMQConfiguration.java                 # Configuração RabbitMQ
│   │   ├── DataSourceConfiguration.java               # Configuração JDBC
│   │   └── JacksonConfiguration.java                  # Configuração JSON
│   │
│   └── rabbitmq/
│       ├── RabbitMQProperties.java                    # Properties do RabbitMQ
│       └── ExchangeConfiguration.java                 # Exchanges, queues, bindings
│
└── adapter/
    ├── aggregate/atendimento/
    │   ├── inbound/
    │   │   └── web/
    │   │       └── AtendimentoController.java         # REST Controller (Commands)
    │   └── dto/
    │       ├── AtendimentoDTO.java
    │       ├── ItemdeatendimentoDTO.java
    │       ├── OcorrenciaDTO.java                     # NOVO
    │       ├── ClienteDTO.java
    │       ├── VeiculoDTO.java
    │       ├── EnderecoDTO.java
    │       └── (outros DTOs)
    │
    └── projection/
        ├── inbound/
        │   └── web/
        │       └── AtendimentoProjectionController.java # REST Controller (Queries)
        └── dto/
            └── AtendimentoProjectionDTO.java

src/main/resources/
├── application.yml                                     # Configuração local/dev
├── application-kubernetes.yml                          # Configuração Kubernetes
├── logback-spring.xml                                  # Configuração de logs
└── db/migration/                                       # Flyway migrations
    ├── V0__create_schema_assistencia_es.sql
    ├── V1__eventsourcing_tables.sql
    ├── V2__notify_trigger.sql
    └── V3__projection_tables.sql                       # Read Model
```

---

## 4. Detalhamento dos Componentes

### 4.1 Domain Layer - Agregado

#### AtendimentoAggregate.java

```java
package br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento;

import br.com.comigo.core.domain.Aggregate;
import br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.concept.Atendimento;
import br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.command.*;
import br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.event.*;

import java.util.EnumSet;
import java.util.UUID;

public class AtendimentoAggregate extends Aggregate {

    private Atendimento atendimento = new Atendimento();

    public AtendimentoAggregate(UUID aggregateId, int version) {
        super(aggregateId, version);
        super.defCommands(AtendimentoCommandType.values());
    }

    // ===== MÉTODOS PROCESS (Comandos) =====

    public void process(SolicitarCommand command) {
        // Valida: estado deve ser null
        if (this.atendimento.getStatus() != null) {
            throw new IllegalStateException("Atendimento já foi solicitado");
        }

        super.applyChange(SolicitadoEvent.builder()
            .aggregateid(super.aggregateId)
            .version(this.getNextVersion())
            .protocolo(command.getProtocolo())
            .cliente(command.getCliente())
            .veiculo(command.getVeiculo())
            .servico(command.getServico())
            .tipodeocorrencia(command.getTipodeocorrencia())
            .base(command.getBase())
            .origem(command.getOrigem())
            .build());
    }

    public void process(AjustarCommand command) {
        // Valida: estado deve ser SOLICITADO ou AJUSTADO
        if (!EnumSet.of(Atendimento.Status.SOLICITADO, Atendimento.Status.AJUSTADO)
                .contains(this.atendimento.getStatus())) {
            throw new IllegalStateException("Atendimento não pode ser ajustado no estado " + this.atendimento.getStatus());
        }

        super.applyChange(AjustadoEvent.builder()
            .aggregateid(super.aggregateId)
            .version(this.getNextVersion())
            .prestador(command.getPrestador())
            .destino(command.getDestino())
            .items(command.getItems())
            .descricao(command.getDescricao())
            // servico e origem são opcionais
            .servico(command.getServico())
            .origem(command.getOrigem())
            .build());
    }

    public void process(ConfirmarCommand command) {
        // Valida: estado deve ser SOLICITADO ou AJUSTADO
        if (!EnumSet.of(Atendimento.Status.SOLICITADO, Atendimento.Status.AJUSTADO)
                .contains(this.atendimento.getStatus())) {
            throw new IllegalStateException("Atendimento não pode ser confirmado no estado " + this.atendimento.getStatus());
        }

        super.applyChange(ConfirmadoEvent.builder()
            .aggregateid(super.aggregateId)
            .version(this.getNextVersion())
            .build());
    }

    public void process(OcorrenciaCommand command) {
        // Valida: estado deve ser CONFIRMADO ou OCORRIDO
        if (!EnumSet.of(Atendimento.Status.CONFIRMADO, Atendimento.Status.OCORRIDO)
                .contains(this.atendimento.getStatus())) {
            throw new IllegalStateException("Não é possível registrar ocorrência no estado " + this.atendimento.getStatus());
        }

        super.applyChange(OcorridoEvent.builder()
            .aggregateid(super.aggregateId)
            .version(this.getNextVersion())
            .ocorrencias(command.getOcorrencias())
            .build());
    }

    public void process(FinalizarCommand command) {
        // Valida: estado deve ser CONFIRMADO ou OCORRIDO
        if (!EnumSet.of(Atendimento.Status.CONFIRMADO, Atendimento.Status.OCORRIDO)
                .contains(this.atendimento.getStatus())) {
            throw new IllegalStateException("Atendimento não pode ser finalizado no estado " + this.atendimento.getStatus());
        }

        super.applyChange(FinalizadoEvent.builder()
            .aggregateid(super.aggregateId)
            .version(this.getNextVersion())
            .build());
    }

    public void process(CancelarCommand command) {
        // Valida: estado deve ser SOLICITADO ou AJUSTADO
        if (!EnumSet.of(Atendimento.Status.SOLICITADO, Atendimento.Status.AJUSTADO)
                .contains(this.atendimento.getStatus())) {
            throw new IllegalStateException("Atendimento não pode ser cancelado no estado " + this.atendimento.getStatus());
        }

        super.applyChange(CanceladoEvent.builder()
            .aggregateid(super.aggregateId)
            .version(this.getNextVersion())
            .build());
    }

    // ===== MÉTODOS APPLY (Eventos) =====

    public void apply(SolicitadoEvent event) {
        this.atendimento.setId(event.getAggregateId());
        this.atendimento.setStatus(Atendimento.Status.SOLICITADO);
        this.atendimento.setProtocolo(event.getProtocolo());
        this.atendimento.setSolicitadoem(event.getCreatedDate());
        this.atendimento.setCliente(event.getCliente());
        this.atendimento.setVeiculo(event.getVeiculo());
        this.atendimento.setServico(event.getServico());
        this.atendimento.setTipodeocorrencia(event.getTipodeocorrencia());
        this.atendimento.setBase(event.getBase());
        this.atendimento.setOrigem(event.getOrigem());
        this.version = event.getVersion();
    }

    public void apply(AjustadoEvent event) {
        this.atendimento.setStatus(Atendimento.Status.AJUSTADO);
        this.atendimento.setAjustadoem(event.getCreatedDate());
        this.atendimento.setPrestador(event.getPrestador());
        this.atendimento.setDestino(event.getDestino());
        this.atendimento.setItems(event.getItems());
        this.atendimento.setDescricao(event.getDescricao());

        // Atualiza servico e origem se fornecidos
        if (event.getServico() != null) {
            this.atendimento.setServico(event.getServico());
        }
        if (event.getOrigem() != null) {
            this.atendimento.setOrigem(event.getOrigem());
        }

        this.version = event.getVersion();
    }

    public void apply(ConfirmadoEvent event) {
        this.atendimento.setStatus(Atendimento.Status.CONFIRMADO);
        this.atendimento.setConfirmadoem(event.getCreatedDate());
        this.version = event.getVersion();
    }

    public void apply(OcorridoEvent event) {
        this.atendimento.setStatus(Atendimento.Status.OCORRIDO);
        this.atendimento.setOcorridoem(event.getCreatedDate());
        // Adiciona ocorrências à lista existente
        this.atendimento.getOcorrencias().addAll(event.getOcorrencias());
        this.version = event.getVersion();
    }

    public void apply(FinalizadoEvent event) {
        this.atendimento.setStatus(Atendimento.Status.FINALIZADO);
        this.atendimento.setFinalizadoem(event.getCreatedDate());
        this.version = event.getVersion();
    }

    public void apply(CanceladoEvent event) {
        this.atendimento.setStatus(Atendimento.Status.CANCELADO);
        this.atendimento.setCanceladoem(event.getCreatedDate());
        this.version = event.getVersion();
    }

    @Override
    public String getAggregateType() {
        return AggregateType.YC_ECOMIGO_ATENDIMENTO.toString();
    }
}
```

---

### 4.2 Domain Layer - Value Objects (Concepts)

#### Atendimento.java (Estado do Agregado)

```java
package br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.concept;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Atendimento {

    public enum Status {
        SOLICITADO,
        AJUSTADO,
        CONFIRMADO,
        OCORRIDO,      // NOVO
        FINALIZADO,
        CANCELADO
    }

    private UUID id;
    private String protocolo;          // NOVO
    private Status status;
    private Integer version;

    // Timestamps dos eventos
    private Timestamp solicitadoem;
    private Timestamp ajustadoem;
    private Timestamp confirmadoem;
    private Timestamp ocorridoem;      // NOVO
    private Timestamp finalizadoem;
    private Timestamp canceladoem;

    // Dados de negócio
    private String tipodeocorrencia;
    private String descricao;

    // Referências a outros agregados
    private ClienteRef cliente;
    private VeiculodeclienteRef veiculo;
    private ServicoRef servico;
    private PrestadorRef prestador;

    // Endereços
    private Endereco base;
    private Endereco origem;
    private Endereco destino;

    // Coleções
    private List<Itemdeatendimento> items = new ArrayList<>();
    private List<String> ocorrencias = new ArrayList<>();  // NOVO

    public Atendimento() {}

    // Getters e Setters...
}
```

#### Endereco.java (Value Object)

```java
package br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.concept;

import br.com.yc.ecomigo.common.model.records.TipoDeEndereco;

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
            throw new IllegalArgumentException("CEP inválido: " + cep);
        }
    }

    private static boolean isValidCep(String cep) {
        if (cep == null) return false;
        String cleaned = cep.replaceAll("[^0-9]", "");
        return cleaned.matches("\\d{8}");
    }
}
```

#### ClienteRef.java

```java
package br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.concept;

public record ClienteRef(Long id, String nome, DocFiscal docfiscal) {}
```

#### DocFiscal.java

```java
package br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.concept;

public record DocFiscal(String tipo, String numero) {
    public DocFiscal {
        if (tipo == null || (!tipo.equals("CPF") && !tipo.equals("CNPJ"))) {
            throw new IllegalArgumentException("Tipo de documento deve ser CPF ou CNPJ");
        }
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("Número do documento é obrigatório");
        }
    }
}
```

#### Itemdeatendimento.java

```java
package br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.concept;

public record Itemdeatendimento(
    String nome,
    String unidadedemedida,
    Integer precounitario,  // em centavos
    Integer quantidade,
    String observacao
) {
    public Itemdeatendimento {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do item é obrigatório");
        }
        if (precounitario == null || precounitario < 0) {
            throw new IllegalArgumentException("Preço unitário deve ser >= 0");
        }
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser > 0");
        }
    }

    public Integer getValorTotal() {
        return precounitario * quantidade;
    }
}
```

---

### 4.3 Domain Layer - Commands

#### OcorrenciaCommand.java (NOVO)

```java
package br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.command;

import br.com.comigo.core.domain.command.Command;
import br.com.yc.ecomigo.assistencia.domain.aggregate.AggregateType;

import java.util.List;
import java.util.UUID;

public final class OcorrenciaCommand extends Command {

    private final List<String> ocorrencias;

    public OcorrenciaCommand(UUID aggregateId, List<String> ocorrencias) {
        super(
            AggregateType.YC_ECOMIGO_ATENDIMENTO.toString(),
            aggregateId,
            OcorrenciaCommand.class
        );

        if (ocorrencias == null || ocorrencias.isEmpty()) {
            throw new IllegalArgumentException("Deve haver pelo menos uma ocorrência");
        }

        this.ocorrencias = List.copyOf(ocorrencias); // imutável
    }

    public List<String> getOcorrencias() {
        return ocorrencias;
    }
}
```

---

### 4.4 Domain Layer - Events

#### OcorridoEvent.java (NOVO)

```java
package br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.event;

import br.com.comigo.core.domain.event.Event;
import br.com.yc.ecomigo.assistencia.domain.aggregate.EventType;
import br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.concept.Atendimento.Status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public final class OcorridoEvent extends Event {

    private final Status status = Status.OCORRIDO;
    private final List<String> ocorrencias;

    @JsonCreator
    public OcorridoEvent(
            @JsonProperty("aggregateid") UUID aggregateid,
            @JsonProperty("version") int version,
            @JsonProperty("ocorrencias") List<String> ocorrencias
    ) {
        super(aggregateid, version);
        this.ocorrencias = ocorrencias != null ? List.copyOf(ocorrencias) : List.of();
    }

    public static OcorridoEventBuilder builder() {
        return new OcorridoEventBuilder();
    }

    public Status getStatus() {
        return status;
    }

    public List<String> getOcorrencias() {
        return ocorrencias;
    }

    @Override
    public String getEventType() {
        return EventType.YC_ECOMIGO_ATENDIMENTO_OCORRIDO.toString();
    }

    // Builder class
    public static class OcorridoEventBuilder {
        private UUID aggregateid;
        private int version;
        private List<String> ocorrencias;

        public OcorridoEventBuilder aggregateid(UUID aggregateid) {
            this.aggregateid = aggregateid;
            return this;
        }

        public OcorridoEventBuilder version(int version) {
            this.version = version;
            return this;
        }

        public OcorridoEventBuilder ocorrencias(List<String> ocorrencias) {
            this.ocorrencias = ocorrencias;
            return this;
        }

        public OcorridoEvent build() {
            return new OcorridoEvent(aggregateid, version, ocorrencias);
        }
    }
}
```

---

### 4.5 Application Layer - Event Handlers

#### FinanceiroIntegrationEventSender.java (NOVO)

```java
package br.com.yc.ecomigo.assistencia.application.service.aggregate.atendimento.event;

import br.com.comigo.core.application.service.AggregateStore;
import br.com.comigo.core.application.service.event.handler.SyncEventHandler;
import br.com.comigo.core.domain.Aggregate;
import br.com.comigo.core.domain.event.Event;
import br.com.comigo.core.domain.event.EventWithId;

import br.com.yc.ecomigo.assistencia.domain.aggregate.AggregateType;
import br.com.yc.ecomigo.assistencia.domain.aggregate.EventType;
import br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.AtendimentoAggregate;
import br.com.yc.ecomigo.assistencia.adapter.aggregate.atendimento.dto.AtendimentoDTO;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class FinanceiroIntegrationEventSender implements SyncEventHandler {

    private final AggregateStore aggregateStore;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper;

    @Value("${db.schema}")
    private String schemaName;

    @Value("${spring.rabbitmq.assistencia.atendimento.financeiro.exchange}")
    private String financeiroExchange;

    @Value("${spring.rabbitmq.assistencia.atendimento.financeiro.routing-key}")
    private String financeiroRoutingKey;

    public FinanceiroIntegrationEventSender(
            AggregateStore aggregateStore,
            RabbitTemplate rabbitTemplate,
            ObjectMapper mapper
    ) {
        this.aggregateStore = aggregateStore;
        this.rabbitTemplate = rabbitTemplate;
        this.mapper = mapper;
    }

    @Override
    public void handleEvents(List<EventWithId<Event>> events, Aggregate aggregate) {
        // Filtra apenas eventos FINALIZADO
        events.stream()
            .filter(eventWithId -> EventType.YC_ECOMIGO_ATENDIMENTO_FINALIZADO.toString()
                    .equals(eventWithId.event().getEventType()))
            .forEach(this::handleEvent);
    }

    private void handleEvent(EventWithId<Event> eventWithId) {
        Event event = eventWithId.event();

        // Reconstrói agregado na versão do evento
        Aggregate aggregate = aggregateStore.readAggregate(
            schemaName,
            AggregateType.YC_ECOMIGO_ATENDIMENTO.toString(),
            event.getAggregateId(),
            event.getVersion()
        );

        AtendimentoAggregate atendimentoAggregate = (AtendimentoAggregate) aggregate;

        // Cria DTO com dados completos
        AtendimentoDTO dto = AtendimentoDTO.fromAggregate(atendimentoAggregate);

        // Envia para RabbitMQ
        try {
            String json = mapper.writeValueAsString(dto);
            rabbitTemplate.convertAndSend(financeiroExchange, financeiroRoutingKey, json);
            log.info("Enviado evento finalizado para financeiro: {}", event.getAggregateId());
        } catch (Exception e) {
            log.error("Erro ao enviar evento para financeiro", e);
            throw new RuntimeException("Falha ao enviar evento para financeiro", e);
        }
    }

    @Override
    public String getAggregateType() {
        return AggregateType.YC_ECOMIGO_ATENDIMENTO.toString();
    }
}
```

---

### 4.6 Adapter Layer - Controller

#### AtendimentoController.java

```java
package br.com.yc.ecomigo.assistencia.adapter.aggregate.atendimento.inbound.web;

import br.com.comigo.core.application.service.AggregateStore;
import br.com.comigo.core.application.service.command.CommandProcessor;
import br.com.comigo.core.domain.Aggregate;

import br.com.yc.ecomigo.assistencia.domain.aggregate.AggregateType;
import br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.AtendimentoAggregate;
import br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.command.*;
import br.com.yc.ecomigo.assistencia.adapter.aggregate.atendimento.dto.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

@RestController
@RequestMapping("/atendimento")
public class AtendimentoController {

    @Value("${db.schema}")
    private String schemaName;

    private final ObjectMapper objectMapper;
    private final CommandProcessor commandProcessor;
    private final AggregateStore aggregateStore;

    public AtendimentoController(
            ObjectMapper objectMapper,
            CommandProcessor commandProcessor,
            AggregateStore aggregateStore
    ) {
        this.objectMapper = objectMapper;
        this.commandProcessor = commandProcessor;
        this.aggregateStore = aggregateStore;
    }

    @PostMapping("/solicitar")
    public ResponseEntity<JsonNode> solicitar(@RequestBody SolicitarDTO dto) {
        SolicitarCommand command = SolicitarCommand.fromDTO(dto);
        Aggregate instance = commandProcessor.process(command);

        return ResponseEntity.ok()
            .body(objectMapper.createObjectNode()
                  .put("id", instance.getAggregateId().toString()));
    }

    @PutMapping("/ajustar")
    public ResponseEntity<Void> ajustar(@RequestBody AjustarDTO dto) {
        AjustarCommand command = AjustarCommand.fromDTO(dto);
        commandProcessor.process(command);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/confirmar")
    public ResponseEntity<Void> confirmar(@RequestBody ConfirmarDTO dto) {
        ConfirmarCommand command = new ConfirmarCommand(UUID.fromString(dto.id()));
        commandProcessor.process(command);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/ocorrencia")  // NOVO
    public ResponseEntity<Void> ocorrencia(@RequestBody OcorrenciaDTO dto) {
        OcorrenciaCommand command = new OcorrenciaCommand(
            UUID.fromString(dto.id()),
            dto.ocorrencias()
        );
        commandProcessor.process(command);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/finalizar")
    public ResponseEntity<Void> finalizar(@RequestBody FinalizarDTO dto) {
        FinalizarCommand command = new FinalizarCommand(UUID.fromString(dto.id()));
        commandProcessor.process(command);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/cancelar")
    public ResponseEntity<Void> cancelar(@RequestBody CancelarDTO dto) {
        CancelarCommand command = new CancelarCommand(UUID.fromString(dto.id()));
        commandProcessor.process(command);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<AtendimentoDTO> getById(@PathVariable String uuid) {
        AtendimentoAggregate aggregate = (AtendimentoAggregate) aggregateStore.readAggregate(
            schemaName,
            AggregateType.YC_ECOMIGO_ATENDIMENTO.toString(),
            UUID.fromString(uuid)
        );

        AtendimentoDTO dto = AtendimentoDTO.fromAggregate(aggregate);
        return ResponseEntity.ok(dto);
    }
}
```

---

## 5. Configuração do Projeto

### 5.1 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.2</version>
        <relativePath/>
    </parent>

    <groupId>br.com.yc.ecomigo</groupId>
    <artifactId>assistencia-atendimento-es</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>assistencia-atendimento-es</name>
    <description>Microservice CQRS/ES para Atendimento</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2024.0.0</spring-cloud.version>
    </properties>

    <dependencies>
        <!-- Core CQRS (framework customizado) -->
        <dependency>
            <groupId>br.com.comigo</groupId>
            <artifactId>core-cqrs</artifactId>
            <version>0.0.1</version>
        </dependency>

        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Spring Cloud -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <!-- Hibernate Types para JSON -->
        <dependency>
            <groupId>com.vladmihalcea</groupId>
            <artifactId>hibernate-types-60</artifactId>
            <version>2.21.1</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- Micrometer -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Testes -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### 5.2 application.yml

```yaml
spring:
  application:
    name: assistencia-atendimento-es

  # Spring Cloud Config Server
  config:
    import: "configserver:${CONFIG_SERVER_URL:http://localhost:9000}"

  # Datasource (PostgreSQL)
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:assistencia_db}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  # JPA
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  # RabbitMQ
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    assistencia:
      atendimento:
        financeiro:
          exchange: "assistencia.atendimento.financeiro.exchange"
          routing-key: "atendimento.finalizado"

# Schema do Event Store
db:
  schema: assistencia_es

# Server
server:
  port: 8080

# Actuator / Prometheus
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,metrics

# Logging
logging:
  level:
    br.com.yc.ecomigo: DEBUG
    br.com.comigo.core: DEBUG
    org.springframework: INFO
```

---

## 6. Migrations de Banco de Dados

### V0__create_schema_assistencia_es.sql

```sql
CREATE SCHEMA IF NOT EXISTS assistencia_es;
```

### V1__eventsourcing_tables.sql

```sql
-- Tabela de agregados
CREATE TABLE IF NOT EXISTS assistencia_es.ES_AGGREGATE (
  ID              UUID     PRIMARY KEY,
  VERSION         INTEGER  NOT NULL,
  AGGREGATE_TYPE  TEXT     NOT NULL
);

CREATE INDEX IF NOT EXISTS IDX_ES_AGGREGATE_AGGREGATE_TYPE
  ON assistencia_es.ES_AGGREGATE (AGGREGATE_TYPE);

-- Tabela de eventos
CREATE TABLE IF NOT EXISTS assistencia_es.ES_EVENT (
  ID              BIGSERIAL  PRIMARY KEY,
  TRANSACTION_ID  XID8       NOT NULL,
  AGGREGATE_ID    UUID       NOT NULL REFERENCES assistencia_es.ES_AGGREGATE (ID),
  VERSION         INTEGER    NOT NULL,
  EVENT_TYPE      TEXT       NOT NULL,
  JSON_DATA       JSON       NOT NULL,
  UNIQUE (AGGREGATE_ID, VERSION)
);

CREATE INDEX IF NOT EXISTS IDX_ES_EVENT_TRANSACTION_ID_ID
  ON assistencia_es.ES_EVENT (TRANSACTION_ID, ID);

CREATE INDEX IF NOT EXISTS IDX_ES_EVENT_AGGREGATE_ID
  ON assistencia_es.ES_EVENT (AGGREGATE_ID);

CREATE INDEX IF NOT EXISTS IDX_ES_EVENT_VERSION
  ON assistencia_es.ES_EVENT (VERSION);

-- Tabela de snapshots
CREATE TABLE IF NOT EXISTS assistencia_es.ES_AGGREGATE_SNAPSHOT (
  AGGREGATE_ID  UUID     NOT NULL REFERENCES assistencia_es.ES_AGGREGATE (ID),
  VERSION       INTEGER  NOT NULL,
  JSON_DATA     JSON     NOT NULL,
  PRIMARY KEY (AGGREGATE_ID, VERSION)
);

CREATE INDEX IF NOT EXISTS IDX_ES_AGGREGATE_SNAPSHOT_AGGREGATE_ID
  ON assistencia_es.ES_AGGREGATE_SNAPSHOT (AGGREGATE_ID);

CREATE INDEX IF NOT EXISTS IDX_ES_AGGREGATE_SNAPSHOT_VERSION
  ON assistencia_es.ES_AGGREGATE_SNAPSHOT (VERSION);

-- Tabela de subscriptions (outbox pattern)
CREATE TABLE IF NOT EXISTS assistencia_es.ES_EVENT_SUBSCRIPTION (
  SUBSCRIPTION_NAME    TEXT    PRIMARY KEY,
  LAST_TRANSACTION_ID  XID8    NOT NULL,
  LAST_EVENT_ID        BIGINT  NOT NULL
);
```

### V2__notify_trigger.sql

```sql
-- Trigger para notificar novos eventos (LISTEN/NOTIFY)
CREATE OR REPLACE FUNCTION assistencia_es.notify_new_event()
RETURNS TRIGGER AS $$
BEGIN
  PERFORM pg_notify('assistencia_es_new_event', NEW.ID::text);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_notify_new_event
AFTER INSERT ON assistencia_es.ES_EVENT
FOR EACH ROW EXECUTE FUNCTION assistencia_es.notify_new_event();
```

---

## 7. Próximos Passos de Implementação

1. [ ] Criar estrutura de diretórios do projeto
2. [ ] Configurar pom.xml e dependências
3. [ ] Implementar Value Objects (records)
4. [ ] Implementar Commands (6 comandos)
5. [ ] Implementar Events (6 eventos)
6. [ ] Implementar AtendimentoAggregate
7. [ ] Implementar Enums (AggregateType, EventType, CommandType, Status)
8. [ ] Implementar DTOs
9. [ ] Implementar Controller REST
10. [ ] Implementar Event Handlers (Financeiro Integration)
11. [ ] Configurar RabbitMQ
12. [ ] Criar migrations de banco de dados
13. [ ] Implementar Projection (Read Model)
14. [ ] Testes unitários
15. [ ] Testes de integração

---

**Fim do documento de design do projeto.**
