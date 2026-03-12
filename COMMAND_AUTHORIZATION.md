# Autorização de Comandos CQRS (Command-Level RBAC)

## Visão Geral

Este documento descreve a implementação de autorização RBAC (Role-Based Access Control) a nível de comandos CQRS no microserviço `ecom.atendimento`.

O padrão foi adaptado do `ecom.suporte`, que implementa autorização a nível de entidades (entity-level RBAC). Como `ecom.atendimento` usa CQRS/Event Sourcing, a autorização é aplicada **antes do processamento dos comandos**, no nível do Controller.

---

## Arquitetura da Solução

### Fluxo de Autorização

```
Client Request (JWT)
       ↓
AtendimentoController
       ↓
1. Extrai User do SecurityContext
2. Cria Command com dados do request
3. ⭐ CommandAuthorizationService.checkCommandAuthorization(command, user)
       ↓
   RoleAuthorizationService.checkWriteAccess(user, command.toWrite())
       ↓
   Verifica se user possui ALGUMA das roles permitidas
       ↓
   [AUTORIZADO] → CommandProcessor.process(command) → Event Store
   [NEGADO]     → UnauthorizedException (HTTP 403 Forbidden)
```

### Componentes Principais

#### 1. **Interface `Authorizable`**

Localização: `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/infrastructure/security/Authorizable.java`

Define o contrato de autorização implementado pelos comandos:

```java
public interface Authorizable {
    String[] toWrite();  // Roles permitidas para escrita (comandos)
    String[] toRead();   // Roles permitidas para leitura (queries)
}
```

**Todos os 6 comandos implementam esta interface**:
- `SolicitarCommand`
- `AjustarCommand`
- `ConfirmarCommand`
- `OcorrenciaCommand`
- `FinalizarCommand`
- `CancelarCommand`

#### 2. **Service `RoleAuthorizationService`**

Localização: `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/infrastructure/security/RoleAuthorizationService.java`

Cópia exata do `ecom.suporte`. Responsável pela lógica de verificação de roles:

- Extrai roles do `User` (vêm do JWT via yc.pr)
- Adiciona prefixo `ROLE_` (convenção do yc.pr)
- Verifica se user possui **ALGUMA** das roles permitidas
- Lança `UnauthorizedException` (HTTP 403) se acesso negado

**Métodos principais**:
- `checkReadAccess(User user, String[] allowedRoles)`
- `checkWriteAccess(User user, String[] allowedRoles)`

**Logging detalhado** para auditoria:
- Username, User ID, Tenants
- Roles do JWT
- Roles requeridas
- Resultado da autorização

#### 3. **Service `CommandAuthorizationService`**

Localização: `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/infrastructure/security/CommandAuthorizationService.java`

Service específico para autorização de comandos CQRS. Adapta o padrão do `ecom.suporte` para CQRS.

**Método principal**:
```java
public void checkCommandAuthorization(Authorizable command, User user) throws UnauthorizedException {
    String[] allowedRoles = command.toWrite();
    roleAuthorizationService.checkWriteAccess(user, allowedRoles);
}
```

**Logging adicional** para rastreabilidade:
- Nome do comando (ex: `SolicitarCommand`)
- Username
- Roles permitidas

#### 4. **Controller `AtendimentoController`**

Localização: `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/adapter/rest/AtendimentoController.java`

**Todos os 6 métodos de comando** foram atualizados para incluir autorização:

```java
try {
    checkTenantIDAuthority(user, tenantId);

    // Cria comando
    XxxCommand command = new XxxCommand(...);

    // ⭐ VALIDA AUTORIZAÇÃO ANTES DE PROCESSAR
    commandAuthorizationService.checkCommandAuthorization(command, user);

    // Processa comando (só executado se autorizado)
    Aggregate aggregate = commandProcessor.process(command);

    return ResponseEntity...;

} catch (UnauthorizedException e) {
    log.error("[Tenant:{}] [User:{}] Acesso negado: {}", tenantId, user.getUsername(), e.getReason());
    throw e; // HTTP 403 Forbidden
} catch (Exception e) {
    // Outros erros
}
```

**Métodos atualizados**:
1. `solicitar()` → SolicitarCommand
2. `ajustar()` → AjustarCommand
3. `confirmar()` → ConfirmarCommand
4. `ocorrencia()` → OcorrenciaCommand
5. `finalizar()` → FinalizarCommand
6. `cancelar()` → CancelarCommand

---

## Configuração de Roles

### Roles Permitidas

Atualmente, **todos os comandos** permitem as mesmas roles:

```java
public String[] toWrite() {
    return new String[] { "GERENTE", "ADMINISTRADOR" };
}

public String[] toRead() {
    return new String[] { "GERENTE", "ADMINISTRADOR" };
}
```

### Como Alterar Roles por Comando

Para alterar as roles permitidas em um comando específico, edite o método `toWrite()` no arquivo do comando:

**Exemplo**: Permitir apenas ADMINISTRADOR para finalizar atendimentos:

```java
// Arquivo: FinalizarCommand.java
public String[] toWrite() {
    return new String[] { "ADMINISTRADOR" }; // Removido GERENTE
}
```

**Exemplo**: Adicionar role OPERADOR para solicitar atendimentos:

```java
// Arquivo: SolicitarCommand.java
public String[] toWrite() {
    return new String[] { "GERENTE", "ADMINISTRADOR", "OPERADOR" };
}
```

---

## Diferenças Entre ecom.suporte e ecom.atendimento

| Aspecto | ecom.suporte | ecom.atendimento |
|---------|--------------|------------------|
| **Padrão** | CRUD tradicional | CQRS/Event Sourcing |
| **Autorização** | Entity-level (SecuredRepository) | Command-level (Controller) |
| **Interceptor** | SecuredRepository wrapper | CommandAuthorizationService |
| **Onde valida** | Repository.save/findById | Controller (antes de commandProcessor) |
| **Interface** | Não tem (métodos diretos na entidade) | `Authorizable` (implementada pelos comandos) |
| **RoleAuthorizationService** | Mesmo código | Mesmo código (copiado) |

---

## Testes de Autorização

### Teste Manual via cURL

#### 1. Obter Token JWT

```bash
# Endpoint de autenticação (ajustar conforme yc.pr)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "gerente@example.com",
    "password": "senha123"
  }'
```

Resposta:
```json
{
  "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGc..."
}
```

#### 2. Testar Comando com Role Permitida (GERENTE)

```bash
curl -X POST http://localhost:8080/api/atendimento/solicitar \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGc..." \
  -H "X-Tenant-Id: 9d77f828-5e8c-4807-b554-5a29e85fc37f" \
  -d '{
    "protocolo": "AT-2024-001",
    "tipodeocorrencia": "Pneu furado",
    "cliente": {
      "id": 1,
      "nome": "João Silva",
      "docfiscal": {"tipo": "CPF", "numero": "12345678900"}
    },
    ...
  }'
```

**Resposta esperada**: HTTP 201 Created
```json
{
  "id": "682a8c34-bb58-4192-831a-7828519d8ab8"
}
```

#### 3. Testar Comando com Role Não Permitida (USUARIO)

```bash
# Mesmo comando acima, mas com token de user sem role GERENTE/ADMINISTRADOR
```

**Resposta esperada**: HTTP 403 Forbidden
```json
{
  "timestamp": "2026-03-12T13:15:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Usuário usuario@example.com não autorizado para operação WRITE. Roles requeridas: [GERENTE, ADMINISTRADOR]",
  "path": "/api/atendimento/solicitar"
}
```

### Verificação de Logs

Com logging em `DEBUG`, você verá nos logs:

```
>>> [AUTH] ========================================
>>> [AUTH] Iniciando verificação de autorização
>>> [AUTH] Operação: WRITE
>>> [AUTH] Username: gerente@example.com
>>> [AUTH] User ID: 123
>>> [AUTH] User tenants: [9d77f828-5e8c-4807-b554-5a29e85fc37f]
>>> [AUTH] User roles (do JWT): [ROLE_GERENTE, ROLE_USUARIO]
>>> [AUTH] Roles permitidas: [GERENTE, ADMINISTRADOR]
>>> [AUTH] Roles requeridas com prefixo (ROLE_): [ROLE_GERENTE, ROLE_ADMINISTRADOR]
>>> [AUTH] ACESSO AUTORIZADO!
>>> [AUTH] ========================================
>>> [COMMAND_AUTH] ========================================
>>> [COMMAND_AUTH] Validando autorização para comando: SolicitarCommand
>>> [COMMAND_AUTH] User: gerente@example.com
>>> [COMMAND_AUTH] Roles permitidas para comando: [GERENTE, ADMINISTRADOR]
>>> [COMMAND_AUTH] ✓ Autorização concedida para comando: SolicitarCommand
>>> [COMMAND_AUTH] ========================================
```

---

## Impacto nos Testes

### Testes Unitários

Comandos podem ser testados sem autenticação:

```java
@Test
void testSolicitarCommandValidation() {
    UUID aggregateId = UUID.randomUUID();

    SolicitarCommand command = new SolicitarCommand(
        aggregateId,
        "AT-2024-001",
        // ...
    );

    // Testa apenas validação de negócio
    assertNotNull(command.getProtocolo());
}
```

### Testes de Integração

**Necessário** incluir JWT válido nos testes:

```java
@Test
@WithMockUser(username = "gerente@example.com", roles = {"GERENTE"})
void testSolicitarAtendimentoComAutorizacao() {
    // Teste passa com role GERENTE
    mockMvc.perform(post("/api/atendimento/solicitar")
            .header("Authorization", "Bearer " + validToken)
            .header("X-Tenant-Id", tenantId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated());
}

@Test
@WithMockUser(username = "usuario@example.com", roles = {"USUARIO"})
void testSolicitarAtendimentoSemAutorizacao() {
    // Teste falha com role USUARIO
    mockMvc.perform(post("/api/atendimento/solicitar")
            .header("Authorization", "Bearer " + validToken)
            .header("X-Tenant-Id", tenantId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isForbidden());
}
```

---

## Segurança e Boas Práticas

### 1. Princípio do Menor Privilégio

- Cada comando deve definir **apenas** as roles estritamente necessárias
- Comandos críticos (ex: `FinalizarCommand`) devem ter roles mais restritivas

### 2. Auditoria

- Logs detalhados registram **todas** as tentativas de acesso (autorizadas e negadas)
- Use ferramentas de análise de logs (ELK, Splunk) para detectar tentativas de acesso não autorizado

### 3. Validação em Múltiplas Camadas

- **Controller**: Autorização RBAC (implementada)
- **Agregado**: Validações de negócio (já implementadas)
- **Event Store**: Validação de integridade (já implementada pelo framework)

### 4. Tratamento de Exceções

- `UnauthorizedException` retorna HTTP 403 (Forbidden)
- **NÃO** expõe detalhes de segurança na resposta ao cliente
- Detalhes completos ficam nos logs (apenas para auditoria)

---

## Manutenção e Extensão

### Adicionar Nova Role

1. **Atualizar JWT** (via yc.pr) para incluir nova role no token
2. **Atualizar comando** para incluir role em `toWrite()`:

```java
public String[] toWrite() {
    return new String[] { "GERENTE", "ADMINISTRADOR", "NOVA_ROLE" };
}
```

3. **Recompilar** e testar

### Criar Comando com Autorização Customizada

```java
package com.ecom.atendimento.domain.command;

import com.ecom.atendimento.infrastructure.security.Authorizable;
import com.ecom.core.cqrs.domain.command.Command;
// ... outros imports

public class NovoCommand extends Command implements Authorizable {

    // ... construtor e campos

    @Override
    public String[] toWrite() {
        return new String[] { "ROLE_CUSTOMIZADA" };
    }

    @Override
    public String[] toRead() {
        return new String[] { "ROLE_CUSTOMIZADA", "ADMINISTRADOR" };
    }
}
```

### Autorização Dinâmica (Futuro)

Para regras mais complexas (ex: "somente o dono do atendimento pode cancelar"):

1. Criar `DynamicAuthorizationService`
2. Verificar não só roles, mas também **dados do comando** vs **dados do user**
3. Chamar após `CommandAuthorizationService`:

```java
commandAuthorizationService.checkCommandAuthorization(command, user);
dynamicAuthorizationService.checkOwnership(command, user); // Futuro
```

---

## Troubleshooting

### Erro: "Usuário não autorizado"

**Causa**: User não possui role permitida

**Solução**:
1. Verificar roles no JWT (decodificar em jwt.io)
2. Verificar roles permitidas em `command.toWrite()`
3. Verificar logs para detalhes

### Erro: "cannot find symbol: method toWrite()"

**Causa**: Comando não implementa `Authorizable`

**Solução**:
```java
public class XxxCommand extends Command implements Authorizable {
    // ... código

    public String[] toWrite() {
        return new String[] { "GERENTE", "ADMINISTRADOR" };
    }

    public String[] toRead() {
        return new String[] { "GERENTE", "ADMINISTRADOR" };
    }
}
```

### Erro de Compilação em CommandAuthorizationService

**Causa**: Tipo do parâmetro `command` incorreto

**Solução**:
```java
// CORRETO
public void checkCommandAuthorization(Authorizable command, User user) { ... }

// INCORRETO (Command não tem toWrite/toRead)
public void checkCommandAuthorization(Command command, User user) { ... }
```

---

## Arquivos Criados/Modificados

### Arquivos Criados

1. `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/infrastructure/security/Authorizable.java`
2. `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/infrastructure/security/RoleAuthorizationService.java`
3. `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/infrastructure/security/CommandAuthorizationService.java`
4. `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/COMMAND_AUTHORIZATION.md` (este arquivo)

### Arquivos Modificados

1. `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/adapter/rest/AtendimentoController.java`
   - Adicionado campo `commandAuthorizationService`
   - Adicionadas chamadas de autorização nos 6 métodos de comando
   - Adicionado tratamento de `UnauthorizedException`

2. **Todos os 6 comandos** agora implementam `Authorizable`:
   - `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/domain/command/SolicitarCommand.java`
   - `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/domain/command/AjustarCommand.java`
   - `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/domain/command/ConfirmarCommand.java`
   - `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/domain/command/OcorrenciaCommand.java`
   - `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/domain/command/FinalizarCommand.java`
   - `/home/julio/Codes/YC/yc-platform-v3/ecom.atendimento/src/main/java/com/ecom/atendimento/domain/command/CancelarCommand.java`

---

## Status da Implementação

✅ **COMPLETO**

- [x] Interface `Authorizable` criada
- [x] `RoleAuthorizationService` implementado (copiado de ecom.suporte)
- [x] `CommandAuthorizationService` implementado
- [x] Todos os 6 comandos implementam `Authorizable`
- [x] `AtendimentoController` atualizado com autorização nos 6 métodos
- [x] Compilação bem-sucedida
- [x] Documentação criada

---

## Referências

- **Padrão base**: `/home/julio/Codes/YC/yc-platform-v3/ecom.suporte`
- **CQRS Framework**: `/home/julio/Codes/YC/yc-platform-v3/ecom.core.cqrs`
- **JWT/Auth**: `/home/julio/Codes/YC/yc-platform-v3/yc.pr`

---

**Última atualização**: 2026-03-12
**Autor**: Claude Code (Anthropic)
**Versão**: 1.0
