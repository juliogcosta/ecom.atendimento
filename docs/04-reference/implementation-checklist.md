# Checklist de Implementação

Passos detalhados para implementar o microservice CQRS/ES Atendimento conforme especificação.

---

## Fase 1: Adicionar Comando "ocorrencia" e Estado "OCORRIDO"

### 1.1 Adicionar Estado no Enum

**Arquivo**: `Atendimento.java:12-14`

```java
public enum Status {
    SOLICITADO,
    AJUSTADO,
    CONFIRMADO,
    OCORRIDO,      // ← ADICIONAR
    FINALIZADO,
    CANCELADO
}
```

### 1.2 Criar OcorrenciaCommand

**Arquivo**: `domain/aggregate/atendimento/command/OcorrenciaCommand.java` (CRIAR)

```java
package br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.command;

import br.com.comigo.core.domain.command.Command;
import java.util.List;
import java.util.UUID;

public final class OcorrenciaCommand extends Command {
    private final List<String> ocorrencias;

    public OcorrenciaCommand(UUID aggregateId, List<String> ocorrencias) {
        super("YC_ECOMIGO_ATENDIMENTO", aggregateId, OcorrenciaCommand.class);

        if (ocorrencias == null || ocorrencias.isEmpty()) {
            throw new IllegalArgumentException("Deve haver pelo menos uma ocorrência");
        }

        this.ocorrencias = List.copyOf(ocorrencias);
    }

    public List<String> getOcorrencias() {
        return ocorrencias;
    }
}
```

### 1.3 Criar OcorridoEvent

**Arquivo**: `domain/aggregate/atendimento/event/OcorridoEvent.java` (CRIAR)

```java
package br.com.yc.ecomigo.assistencia.domain.aggregate.atendimento.event;

import br.com.comigo.core.domain.event.Event;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public final class OcorridoEvent extends Event {
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

    public List<String> getOcorrencias() {
        return ocorrencias;
    }

    @Override
    public String getEventType() {
        return "YC_ECOMIGO_ATENDIMENTO_OCORRIDO";
    }

    // Builder (Lombok @Builder ou manual)
}
```

### 1.4 Adicionar Lista de Ocorrências em Atendimento

**Arquivo**: `domain/aggregate/atendimento/concept/Atendimento.java`

```java
// Adicionar campo:
private Timestamp ocorridoem;
private List<String> ocorrencias = new ArrayList<>();

// Adicionar getters/setters
```

### 1.5 Atualizar AtendimentoAggregate

**Arquivo**: `domain/aggregate/atendimento/AtendimentoAggregate.java`

```java
// Adicionar método process
public void process(OcorrenciaCommand command) {
    // Valida: estado deve ser CONFIRMADO ou OCORRIDO
    if (!EnumSet.of(Atendimento.Status.CONFIRMADO, Atendimento.Status.OCORRIDO)
            .contains(this.atendimento.getStatus())) {
        throw new IllegalStateException("Não é possível registrar ocorrência no estado "
            + this.atendimento.getStatus());
    }

    super.applyChange(OcorridoEvent.builder()
        .aggregateid(super.aggregateId)
        .version(this.getNextVersion())
        .ocorrencias(command.getOcorrencias())
        .build());
}

// Adicionar método apply
public void apply(OcorridoEvent event) {
    this.atendimento.setStatus(Atendimento.Status.OCORRIDO);
    this.atendimento.setOcorridoem(event.getCreatedDate());
    // Adiciona ocorrências à lista existente (acumulativo)
    this.atendimento.getOcorrencias().addAll(event.getOcorrencias());
    this.version = event.getVersion();
}
```

### 1.6 Adicionar em Enums

**Arquivo**: `domain/aggregate/atendimento/AtendimentoCommandType.java`

```java
public enum AtendimentoCommandType implements CommandType {
    SOLICITAR(SolicitarCommand.class),
    AJUSTAR(AjustarCommand.class),
    CONFIRMAR(ConfirmarCommand.class),
    OCORRENCIA(OcorrenciaCommand.class),  // ← ADICIONAR
    FINALIZAR(FinalizarCommand.class),
    CANCELAR(CancelarCommand.class);
    // ...
}
```

**Arquivo**: `domain/aggregate/EventType.java`

```java
public enum EventType {
    YC_ECOMIGO_ATENDIMENTO_SOLICITADO("solicitado", SolicitadoEvent.class),
    YC_ECOMIGO_ATENDIMENTO_AJUSTADO("ajustado", AjustadoEvent.class),
    YC_ECOMIGO_ATENDIMENTO_CONFIRMADO("confirmado", ConfirmadoEvent.class),
    YC_ECOMIGO_ATENDIMENTO_OCORRIDO("ocorrido", OcorridoEvent.class),  // ← ADICIONAR
    YC_ECOMIGO_ATENDIMENTO_FINALIZADO("finalizado", FinalizadoEvent.class),
    YC_ECOMIGO_ATENDIMENTO_CANCELADO("cancelado", CanceladoEvent.class);
    // ...
}
```

### 1.7 Criar DTO

**Arquivo**: `adapter/aggregate/atendimento/dto/OcorrenciaDTO.java` (CRIAR)

```java
package br.com.yc.ecomigo.assistencia.adapter.aggregate.atendimento.dto;

import java.util.List;

public record OcorrenciaDTO(
    String id,
    List<String> ocorrencias
) {
    public OcorrenciaDTO {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID é obrigatório");
        }
        if (ocorrencias == null || ocorrencias.isEmpty()) {
            throw new IllegalArgumentException("Deve haver pelo menos uma ocorrência");
        }
    }
}
```

### 1.8 Adicionar Endpoint no Controller

**Arquivo**: `adapter/aggregate/atendimento/inbound/web/AtendimentoController.java`

```java
@PutMapping("/ocorrencia")
public ResponseEntity<Void> ocorrencia(@RequestBody OcorrenciaDTO dto) {
    OcorrenciaCommand command = new OcorrenciaCommand(
        UUID.fromString(dto.id()),
        dto.ocorrencias()
    );
    commandProcessor.process(command);
    return ResponseEntity.ok().build();
}
```

### 1.9 Testar

```bash
# Solicitar
POST /atendimento/solicitar
{...}
# Retorna: {"id": "550e8400-..."}

# Confirmar
PUT /atendimento/confirmar
{"id": "550e8400-..."}

# Registrar ocorrência
PUT /atendimento/ocorrencia
{
  "id": "550e8400-...",
  "ocorrencias": [
    "Cliente não estava no local",
    "Aguardado 15 minutos"
  ]
}

# Registrar segunda ocorrência
PUT /atendimento/ocorrencia
{
  "id": "550e8400-...",
  "ocorrencias": [
    "Trânsito intenso, atraso de 30 minutos"
  ]
}

# Verificar estado
GET /atendimento/550e8400-...
# Deve retornar status: "OCORRIDO" e lista com 3 ocorrências
```

**Checklist Fase 1**:
- [ ] Enum Status com OCORRIDO
- [ ] OcorrenciaCommand criado
- [ ] OcorridoEvent criado
- [ ] Lista ocorrencias em Atendimento
- [ ] Método process(OcorrenciaCommand)
- [ ] Método apply(OcorridoEvent)
- [ ] Enum AtendimentoCommandType atualizado
- [ ] Enum EventType atualizado
- [ ] OcorrenciaDTO criado
- [ ] Endpoint /ocorrencia adicionado
- [ ] Testes manuais realizados

---

## Fase 2: Adicionar Campo "protocolo"

### 2.1 Adicionar em SolicitarCommand

**Arquivo**: `domain/aggregate/atendimento/command/SolicitarCommand.java`

```java
public final class SolicitarCommand extends Command {
    private final String protocolo;  // ← ADICIONAR
    private final ClienteRef cliente;
    // ... outros campos

    public SolicitarCommand(
            String protocolo,  // ← ADICIONAR
            ClienteRef cliente,
            VeiculodeclienteRef veiculo,
            // ... outros parâmetros
    ) {
        super("YC_ECOMIGO_ATENDIMENTO", generateAggregateId(), SolicitarCommand.class);
        this.protocolo = protocolo;
        // ...
    }

    public String getProtocolo() {
        return protocolo;
    }
}
```

### 2.2 Adicionar em SolicitadoEvent

**Arquivo**: `domain/aggregate/atendimento/event/SolicitadoEvent.java`

```java
public final class SolicitadoEvent extends Event {
    private final String protocolo;  // ← ADICIONAR
    // ... outros campos

    @JsonCreator
    public SolicitadoEvent(
            @JsonProperty("aggregateid") UUID aggregateid,
            @JsonProperty("version") int version,
            @JsonProperty("protocolo") String protocolo,  // ← ADICIONAR
            // ... outros parâmetros
    ) {
        super(aggregateid, version);
        this.protocolo = protocolo;
        // ...
    }
}
```

### 2.3 Adicionar em Atendimento

**Arquivo**: `domain/aggregate/atendimento/concept/Atendimento.java`

```java
public class Atendimento {
    private UUID id;
    private String protocolo;  // ← ADICIONAR
    private Status status;
    // ...

    // Getter/Setter
}
```

### 2.4 Atualizar Agregado

**Arquivo**: `domain/aggregate/atendimento/AtendimentoAggregate.java`

```java
public void process(SolicitarCommand command) {
    // ...
    super.applyChange(SolicitadoEvent.builder()
        .aggregateid(super.aggregateId)
        .version(this.getNextVersion())
        .protocolo(command.getProtocolo())  // ← ADICIONAR
        // ... outros campos
        .build());
}

public void apply(SolicitadoEvent event) {
    this.atendimento.setId(event.getAggregateId());
    this.atendimento.setProtocolo(event.getProtocolo());  // ← ADICIONAR
    // ...
}
```

### 2.5 Geração Automática de Protocolo (Opcional)

**Estratégia 1**: Baseado em timestamp
```java
private static String generateProtocolo() {
    return "ATD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%06d", new Random().nextInt(999999));
    // Exemplo: ATD-20250310-000123
}
```

**Estratégia 2**: Baseado em sequência do banco
```sql
CREATE SEQUENCE assistencia_es.seq_protocolo START 1;

-- No código:
String protocolo = "ATD-" + LocalDate.now().getYear()
                   + "-" + String.format("%06d", nextval('seq_protocolo'));
```

### 2.6 Atualizar DTO

**Arquivo**: `adapter/aggregate/atendimento/dto/SolicitarDTO.java`

```java
public record SolicitarDTO(
    String protocolo,  // ← ADICIONAR (ou deixar null para gerar automaticamente)
    ClienteDTO cliente,
    VeiculoDTO veiculo,
    // ...
) {}
```

**Checklist Fase 2**:
- [ ] Campo protocolo em SolicitarCommand
- [ ] Campo protocolo em SolicitadoEvent
- [ ] Campo protocolo em Atendimento
- [ ] Atualizar process() e apply()
- [ ] Implementar geração automática (se aplicável)
- [ ] Atualizar DTOs
- [ ] Testar criação com protocolo

---

## Fase 3: Implementar Integração Seletiva

### 3.1 Criar FinanceiroIntegrationEventSender

**Arquivo**: `application/service/aggregate/atendimento/event/FinanceiroIntegrationEventSender.java` (CRIAR)

```java
package br.com.yc.ecomigo.assistencia.application.service.aggregate.atendimento.event;

import br.com.comigo.core.application.service.event.handler.SyncEventHandler;
import br.com.comigo.core.domain.event.EventWithId;
import org.springframework.stereotype.Component;

@Component
public class FinanceiroIntegrationEventSender implements SyncEventHandler {

    @Override
    public void handleEvents(List<EventWithId<Event>> events, Aggregate aggregate) {
        // Filtra apenas eventos FINALIZADO
        events.stream()
            .filter(e -> "YC_ECOMIGO_ATENDIMENTO_FINALIZADO".equals(e.event().getEventType()))
            .forEach(this::sendToFinanceiro);
    }

    private void sendToFinanceiro(EventWithId<Event> eventWithId) {
        // Reconstrói agregado
        // Cria DTO
        // Envia para RabbitMQ (exchange: financeiro)
    }

    @Override
    public String getAggregateType() {
        return "YC_ECOMIGO_ATENDIMENTO";
    }
}
```

### 3.2 Configurar RabbitMQ

**Arquivo**: `infrastructure/config/RabbitMQConfiguration.java`

```java
@Configuration
public class RabbitMQConfiguration {

    @Bean
    public DirectExchange financeiroExchange() {
        return new DirectExchange("assistencia.atendimento.financeiro.exchange");
    }

    @Bean
    public Queue financeiroQueue() {
        return new Queue("financeiro.atendimento.queue");
    }

    @Bean
    public Binding financeiroBinding(Queue financeiroQueue, DirectExchange financeiroExchange) {
        return BindingBuilder.bind(financeiroQueue)
                .to(financeiroExchange)
                .with("atendimento.finalizado");
    }
}
```

### 3.3 Configurar application.yml

```yaml
spring:
  rabbitmq:
    assistencia:
      atendimento:
        financeiro:
          exchange: "assistencia.atendimento.financeiro.exchange"
          routing-key: "atendimento.finalizado"
```

**Checklist Fase 3**:
- [ ] FinanceiroIntegrationEventSender criado
- [ ] Filtro por evento FINALIZADO implementado
- [ ] RabbitMQ exchange/queue configurados
- [ ] application.yml atualizado
- [ ] Testar envio apenas de eventos finalizados

---

## Fase 4: Atualizar Projection (Read Model)

### 4.1 Adicionar Campos

**Arquivo**: `domain/projection/AtendimentoProjection.java`

```java
@Entity
@Table(name = "atendimento", schema = "assistencia")
public class AtendimentoProjection {

    @Column(name = "protocolo", length = 64)
    private String protocolo;  // ← ADICIONAR

    @Column(name = "ocorridoem")
    private Timestamp ocorridoem;  // ← ADICIONAR

    @Column(name = "ocorrencias", columnDefinition = "json")
    @Type(JsonType.class)
    private List<String> ocorrencias;  // ← ADICIONAR

    // ... outros campos
}
```

### 4.2 Migration

**Arquivo**: `src/main/resources/db/migration/V4__add_protocolo_and_ocorrencias.sql` (CRIAR)

```sql
ALTER TABLE assistencia.atendimento
  ADD COLUMN protocolo VARCHAR(64);

ALTER TABLE assistencia.atendimento
  ADD COLUMN ocorridoem TIMESTAMP;

ALTER TABLE assistencia.atendimento
  ADD COLUMN ocorrencias JSON;

CREATE INDEX idx_atendimento_protocolo ON assistencia.atendimento (protocolo);
```

### 4.3 Atualizar Projection Updater

**Arquivo**: `application/service/aggregate/atendimento/event/AtendimentoProjectionUpdater.java`

```java
// Adicionar handler para OcorridoEvent
private void handleOcorrido(OcorridoEvent event) {
    projection.setStatus("OCORRIDO");
    projection.setOcorridoem(event.getCreatedDate());

    // Acumula ocorrências
    List<String> existing = projection.getOcorrencias() != null
        ? new ArrayList<>(projection.getOcorrencias())
        : new ArrayList<>();
    existing.addAll(event.getOcorrencias());
    projection.setOcorrencias(existing);

    repository.save(projection);
}
```

**Checklist Fase 4**:
- [ ] Campos adicionados em AtendimentoProjection
- [ ] Migration criada e executada
- [ ] Projection Updater atualizado
- [ ] Testar query com novos campos

---

## Fase 5: Testes

### 5.1 Testes Unitários

**Arquivo**: `src/test/java/AtendimentoAggregateTest.java` (CRIAR)

```java
@Test
void deveRegistrarOcorrenciaQuandoConfirmado() {
    // Given
    AtendimentoAggregate aggregate = new AtendimentoAggregate(UUID.randomUUID(), 0);
    aggregate.loadFromHistory(List.of(
        solicitadoEvent(),
        confirmadoEvent()
    ));

    // When
    aggregate.process(new OcorrenciaCommand(
        aggregate.getAggregateId(),
        List.of("Cliente não estava no local")
    ));

    // Then
    assertEquals(Atendimento.Status.OCORRIDO, aggregate.getAtendimento().getStatus());
    assertEquals(1, aggregate.getAtendimento().getOcorrencias().size());
}

@Test
void naoDeveRegistrarOcorrenciaQuandoSolicitado() {
    // Given
    AtendimentoAggregate aggregate = new AtendimentoAggregate(UUID.randomUUID(), 0);
    aggregate.loadFromHistory(List.of(solicitadoEvent()));

    // When/Then
    assertThrows(IllegalStateException.class, () -> {
        aggregate.process(new OcorrenciaCommand(
            aggregate.getAggregateId(),
            List.of("Teste")
        ));
    });
}
```

### 5.2 Testes de Integração

**Arquivo**: `src/test/java/AtendimentoIntegrationTest.java` (CRIAR)

```java
@SpringBootTest
@AutoConfigureMockMvc
class AtendimentoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRegistrarOcorrenciaViaREST() throws Exception {
        // Solicitar
        String createResponse = mockMvc.perform(post("/atendimento/solicitar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitarJSON()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String id = extractId(createResponse);

        // Confirmar
        mockMvc.perform(put("/atendimento/confirmar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmarJSON(id)))
            .andExpect(status().isOk());

        // Ocorrência
        mockMvc.perform(put("/atendimento/ocorrencia")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ocorrenciaJSON(id)))
            .andExpect(status().isOk());

        // Verificar
        mockMvc.perform(get("/atendimento/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OCORRIDO"))
            .andExpect(jsonPath("$.ocorrencias").isArray())
            .andExpect(jsonPath("$.ocorrencias.length()").value(2));
    }
}
```

**Checklist Fase 5**:
- [ ] Testes unitários do agregado
- [ ] Testes de máquina de estados
- [ ] Testes de integração REST
- [ ] Testes de Event Handlers
- [ ] Coverage > 80%

---

## Resumo de Arquivos a Criar/Modificar

### Criar (Novos Arquivos)
- [ ] `domain/aggregate/atendimento/command/OcorrenciaCommand.java`
- [ ] `domain/aggregate/atendimento/event/OcorridoEvent.java`
- [ ] `adapter/aggregate/atendimento/dto/OcorrenciaDTO.java`
- [ ] `application/service/aggregate/atendimento/event/FinanceiroIntegrationEventSender.java`
- [ ] `src/main/resources/db/migration/V4__add_protocolo_and_ocorrencias.sql`
- [ ] Testes

### Modificar (Arquivos Existentes)
- [ ] `domain/aggregate/atendimento/concept/Atendimento.java` (+ protocolo, ocorridoem, ocorrencias)
- [ ] `domain/aggregate/atendimento/AtendimentoAggregate.java` (+ process/apply)
- [ ] `domain/aggregate/atendimento/AtendimentoCommandType.java` (+ OCORRENCIA)
- [ ] `domain/aggregate/EventType.java` (+ OCORRIDO)
- [ ] `domain/aggregate/atendimento/command/SolicitarCommand.java` (+ protocolo)
- [ ] `domain/aggregate/atendimento/event/SolicitadoEvent.java` (+ protocolo)
- [ ] `adapter/aggregate/atendimento/inbound/web/AtendimentoController.java` (+ endpoint)
- [ ] `domain/projection/AtendimentoProjection.java` (+ campos)
- [ ] `application/service/aggregate/atendimento/event/AtendimentoProjectionUpdater.java`

---

**Total estimado**: 15 arquivos novos + 10 modificados = ~3-5 dias de trabalho

**Arquivos relacionados**:
- [Differences](differences.md) - Contexto das diferenças
- [Commands Spec](../02-specification/commands.md) - Detalhes dos comandos
