# GUIA DE IMPLEMENTAÇÃO DE SEGURANÇA - AtendimentoController

**Data**: 2026-03-12
**Status**: Implementação Spring Security + JWT (yc.pr)

---

## MUDANÇAS NECESSÁRIAS NO ATENDIMENTO CONTROLLER

### 1. ATUALIZAR IMPORTS (linhas 1-22)

Adicionar após linha 10:
```java
import com.yc.pr.models.User;
```

Adicionar após linha 13:
```java
import org.springframework.http.HttpHeaders;
```

Adicionar após linha 15:
```java
import org.springframework.security.core.context.SecurityContextHolder;
```

### 2. ATUALIZAR DECLARAÇÃO DA CLASSE (linha 34)

**ANTES**:
```java
public class AtendimentoController {
```

**DEPOIS**:
```java
public class AtendimentoController extends BaseController {
```

### 3. ATUALIZAR JAVADOC DA CLASSE (linhas 24-28)

**ANTES**:
```java
/**
 * Controller REST para operações no agregado Atendimento.
 * - Comandos (Write Model): POST/PUT
 * - Consultas (Event Store): GET
 */
```

**DEPOIS**:
```java
/**
 * Controller REST para operações no agregado Atendimento.
 * - Comandos (Write Model): POST/PUT
 * - Consultas (Event Store): GET
 *
 * IMPORTANTE: Todos os endpoints requerem autenticação JWT via headers:
 * - Authorization: Bearer {token}
 * - X-Tenant-Id: {tenant-uuid}
 */
```

---

## ATUALIZAÇÕES POR ENDPOINT

### ENDPOINT 1: POST /solicitar (linhas 46-73)

**ANTES**:
```java
@PostMapping("/solicitar")
public ResponseEntity<Map<String, String>> solicitar(@Valid @RequestBody SolicitarRequest request) {
    log.info("Recebendo solicitação de atendimento com protocolo: {}", request.getProtocolo());

    // Gera novo UUID para o agregado
    UUID aggregateId = UUID.randomUUID();

    // ... resto do código
}
```

**DEPOIS**:
```java
@PostMapping(value = "/solicitar", headers = {HttpHeaders.CONTENT_TYPE, "X-Tenant-Id", HttpHeaders.AUTHORIZATION})
public ResponseEntity<Map<String, String>> solicitar(
        @RequestHeader(name = "Authorization", required = true) String authorization,
        @RequestHeader(name = "X-Tenant-Id", required = true) String tenantId,
        @Valid @RequestBody SolicitarRequest request) {

    // Extrai usuário autenticado do SecurityContext
    final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    try {
        // Valida autoridade do usuário para o tenant
        checkTenantIDAuthority(user, tenantId);

        log.info("[Tenant:{}] [User:{}] Recebendo solicitação de atendimento com protocolo: {}",
            tenantId, user.getUsername(), request.getProtocolo());

        // Gera novo UUID para o agregado
        UUID aggregateId = UUID.randomUUID();

        // Converte DTO → Command
        SolicitarCommand command = new SolicitarCommand(
                aggregateId,
                request.getProtocolo(),
                request.getTipodeocorrencia(),
                toCliente(request.getCliente()),
                toVeiculo(request.getVeiculo()),
                toServico(request.getServico()),
                toEndereco(request.getBase()),
                toEndereco(request.getOrigem())
        );

        // Processa comando
        Aggregate aggregate = commandProcessor.process(command);

        log.info("[Tenant:{}] [User:{}] Atendimento {} solicitado com sucesso",
            tenantId, user.getUsername(), aggregate.getAggregateId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("id", aggregate.getAggregateId().toString()));

    } catch (Exception e) {
        if (this.tracePrint) {
            e.printStackTrace();
        }
        throw new RuntimeException(e);
    }
}
```

---

### ENDPOINT 2: PUT /ajustar (linhas 79-102)

**ANTES**:
```java
@PutMapping("/ajustar")
public ResponseEntity<Void> ajustar(@Valid @RequestBody AjustarRequest request) {
    log.info("Recebendo ajuste para atendimento: {}", request.getAggregateId());
    // ... código
}
```

**DEPOIS**:
```java
@PutMapping(value = "/ajustar", headers = {HttpHeaders.CONTENT_TYPE, "X-Tenant-Id", HttpHeaders.AUTHORIZATION})
public ResponseEntity<Void> ajustar(
        @RequestHeader(name = "Authorization", required = true) String authorization,
        @RequestHeader(name = "X-Tenant-Id", required = true) String tenantId,
        @Valid @RequestBody AjustarRequest request) {

    final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    try {
        checkTenantIDAuthority(user, tenantId);

        log.info("[Tenant:{}] [User:{}] Recebendo ajuste para atendimento: {}",
            tenantId, user.getUsername(), request.getAggregateId());

        // Converte DTO → Command
        AjustarCommand command = new AjustarCommand(
                request.getAggregateId(),
                request.getDescricao(),
                request.getPrestador() != null ? toPrestador(request.getPrestador()) : null,
                request.getServico() != null ? toServico(request.getServico()) : null,
                request.getOrigem() != null ? toEndereco(request.getOrigem()) : null,
                request.getDestino() != null ? toEndereco(request.getDestino()) : null,
                request.getItems() != null ? request.getItems().stream()
                        .map(this::toItem)
                        .collect(Collectors.toList()) : null
        );

        // Processa comando
        commandProcessor.process(command);

        log.info("[Tenant:{}] [User:{}] Atendimento {} ajustado com sucesso",
            tenantId, user.getUsername(), request.getAggregateId());

        return ResponseEntity.ok().build();

    } catch (Exception e) {
        if (this.tracePrint) {
            e.printStackTrace();
        }
        throw new RuntimeException(e);
    }
}
```

---

### ENDPOINT 3: PUT /confirmar (linhas 108-118)

**PADRÃO A SEGUIR**:
```java
@PutMapping(value = "/confirmar", headers = {HttpHeaders.CONTENT_TYPE, "X-Tenant-Id", HttpHeaders.AUTHORIZATION})
public ResponseEntity<Void> confirmar(
        @RequestHeader(name = "Authorization", required = true) String authorization,
        @RequestHeader(name = "X-Tenant-Id", required = true) String tenantId,
        @Valid @RequestBody SimpleCommandRequest request) {

    final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    try {
        checkTenantIDAuthority(user, tenantId);

        log.info("[Tenant:{}] [User:{}] Recebendo confirmação para atendimento: {}",
            tenantId, user.getUsername(), request.getAggregateId());

        ConfirmarCommand command = new ConfirmarCommand(request.getAggregateId());
        commandProcessor.process(command);

        log.info("[Tenant:{}] [User:{}] Atendimento {} confirmado com sucesso",
            tenantId, user.getUsername(), request.getAggregateId());

        return ResponseEntity.ok().build();

    } catch (Exception e) {
        if (this.tracePrint) {
            e.printStackTrace();
        }
        throw new RuntimeException(e);
    }
}
```

---

### ENDPOINT 4: PUT /ocorrencia (linhas 125-140)

Aplicar o mesmo padrão do endpoint 3 (confirmar).

---

### ENDPOINT 5: PUT /finalizar (linhas 146-156)

Aplicar o mesmo padrão do endpoint 3 (confirmar).

---

### ENDPOINT 6: PUT /cancelar (linhas 162-172)

Aplicar o mesmo padrão do endpoint 3 (confirmar).

---

### ENDPOINT 7: GET /{aggregateId} (linhas 181-200)

**ANTES**:
```java
@GetMapping("/{aggregateId}")
public ResponseEntity<AtendimentoAggregate> getAtendimento(@PathVariable UUID aggregateId) {
    log.info("Recuperando estado do agregado: {}", aggregateId);
    // ... código
}
```

**DEPOIS**:
```java
@GetMapping(value = "/{aggregateId}", headers = {HttpHeaders.AUTHORIZATION, "X-Tenant-Id"})
public ResponseEntity<AtendimentoAggregate> getAtendimento(
        @RequestHeader(name = "Authorization", required = true) String authorization,
        @RequestHeader(name = "X-Tenant-Id", required = true) String tenantId,
        @PathVariable UUID aggregateId) {

    final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    try {
        checkTenantIDAuthority(user, tenantId);

        log.info("[Tenant:{}] [User:{}] Recuperando estado do agregado: {}",
            tenantId, user.getUsername(), aggregateId);

        // Lê o agregado do Event Store (reconstrói via replay de eventos)
        Aggregate aggregate = aggregateStore.readAggregate(
                schemaName,
                AggregateType.YC_ECOMIGO_ATENDIMENTO.toString(),
                aggregateId
        );

        AtendimentoAggregate atendimentoAggregate = (AtendimentoAggregate) aggregate;

        log.info("[Tenant:{}] [User:{}] Agregado {} recuperado com sucesso. Versão: {}, Status: {}",
                tenantId, user.getUsername(), aggregateId,
                atendimentoAggregate.getVersion(),
                atendimentoAggregate.getAtendimento().getStatus());

        return ResponseEntity.ok(atendimentoAggregate);

    } catch (Exception e) {
        if (this.tracePrint) {
            e.printStackTrace();
        }
        throw new RuntimeException(e);
    }
}
```

---

## MÉTODOS AUXILIARES (linhas 202-269)

**NÃO ALTERAR** - Manter os métodos de conversão como estão:
- `toCliente()`
- `toVeiculo()`
- `toServico()`
- `toEndereco()`
- `toPrestador()`
- `toItem()`

---

## RESUMO DAS MUDANÇAS

1. ✅ Adicionar 3 imports (User, HttpHeaders, SecurityContextHolder)
2. ✅ Fazer classe estender BaseController
3. ✅ Adicionar headers obrigatórios em TODOS os 7 endpoints: `headers = {HttpHeaders.CONTENT_TYPE, "X-Tenant-Id", HttpHeaders.AUTHORIZATION}` (ou apenas AUTHORIZATION e X-Tenant-Id para GET)
4. ✅ Adicionar parâmetros `@RequestHeader` para `authorization` e `tenantId` em TODOS os 7 métodos
5. ✅ Extrair `User` do SecurityContext no início de CADA método
6. ✅ Chamar `checkTenantIDAuthority(user, tenantId)` em CADA método
7. ✅ Envolver lógica em try-catch em TODOS os métodos
8. ✅ Adicionar tenant e username nos logs
9. ✅ Manter métodos auxiliares inalterados

---

## VALIDAÇÃO APÓS IMPLEMENTAÇÃO

Após fazer as mudanças, compile o projeto:

```bash
cd /home/julio/Codes/YC/yc-platform-v3/ecom.atendimento
mvn clean compile
```

Se houver erros de compilação, verifique:
1. Todos os imports estão corretos
2. Todos os métodos têm os try-catch
3. BaseController está sendo estendido corretamente

---

**IMPORTANTE**: Este guia serve como referência. As mudanças devem ser aplicadas manualmente no arquivo `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/adapter/rest/AtendimentoController.java`.

**Backup do arquivo original**: `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/adapter/rest/AtendimentoController.java.bak`
