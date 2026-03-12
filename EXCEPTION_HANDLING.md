# Tratamento Global de Exceções - ecom.atendimento

## Visão Geral

Implementação de tratamento global e padronizado de exceções usando `@RestControllerAdvice` do Spring.

## Arquivos Criados

### 1. ErrorResponse.java
**Localização**: `/src/main/java/com/ecom/atendimento/adapter/dto/ErrorResponse.java`

**Propósito**: DTO padronizado para respostas de erro, seguindo RFC 7807 (Problem Details for HTTP APIs).

**Estrutura**:
```json
{
  "timestamp": "2026-03-11T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Erro de validação nos campos da requisição",
  "path": "/api/atendimento/solicitar",
  "exceptionType": "MethodArgumentNotValidException",
  "fieldErrors": [
    {
      "field": "protocolo",
      "message": "não pode ser vazio",
      "rejectedValue": null
    }
  ]
}
```

**Campos**:
- `timestamp` - ISO-8601 da ocorrência do erro
- `status` - Código HTTP (400, 404, 500, etc.)
- `error` - Nome do status HTTP
- `message` - Mensagem legível para humanos
- `path` - URI da requisição que gerou o erro
- `exceptionType` - Classe da exceção (para debugging)
- `fieldErrors` - Lista de erros de validação (opcional)
- `details` - Mapa com informações contextuais (opcional)

### 2. GlobalExceptionHandler.java
**Localização**: `/src/main/java/com/ecom/atendimento/adapter/exception/GlobalExceptionHandler.java`

**Propósito**: Interceptar todas as exceções lançadas nos controllers e retornar ErrorResponse padronizado.

**Anotação**: `@RestControllerAdvice` - aplica-se globalmente a todos os `@RestController`.

## Exceções Tratadas

### 1. Validação de Entrada

| Exceção | Status HTTP | Descrição |
|---------|-------------|-----------|
| `MethodArgumentNotValidException` | 400 Bad Request | Campos anotados com `@Valid` que falharam validação |
| `ConstraintViolationException` | 400 Bad Request | Violações de Bean Validation |
| `MethodArgumentTypeMismatchException` | 400 Bad Request | Tipo de parâmetro incorreto (ex: UUID inválido) |

**Exemplo**: Campo obrigatório faltando no JSON de request.

### 2. Regras de Negócio

| Exceção | Status HTTP | Descrição |
|---------|-------------|-----------|
| `IllegalStateException` | 422 Unprocessable Entity | Transição de estado inválida no agregado |
| `IllegalArgumentException` | 422 Unprocessable Entity | Argumento inválido fornecido |

**Exemplo**: Tentar finalizar um atendimento que ainda está em estado SOLICITADO.

### 3. Recursos Não Encontrados

| Exceção | Status HTTP | Descrição |
|---------|-------------|-----------|
| `NoSuchElementException` | 404 Not Found | Agregado não existe no Event Store |

**Exemplo**: GET /api/atendimento/{id-inexistente}

### 4. Erros Internos

| Exceção | Status HTTP | Descrição |
|---------|-------------|-----------|
| `Exception` (genérica) | 500 Internal Server Error | Qualquer erro não capturado pelos handlers específicos |

## Modificações no Controller

**Antes**:
```java
@GetMapping("/{aggregateId}")
public ResponseEntity<AtendimentoAggregate> getAtendimento(@PathVariable UUID aggregateId) {
    try {
        Aggregate aggregate = aggregateStore.readAggregate(...);
        return ResponseEntity.ok((AtendimentoAggregate) aggregate);
    } catch (Exception e) {
        return ResponseEntity.notFound().build();
    }
}
```

**Depois**:
```java
@GetMapping("/{aggregateId}")
public ResponseEntity<AtendimentoAggregate> getAtendimento(@PathVariable UUID aggregateId) {
    // Exceções automaticamente capturadas pelo GlobalExceptionHandler
    Aggregate aggregate = aggregateStore.readAggregate(...);
    return ResponseEntity.ok((AtendimentoAggregate) aggregate);
}
```

## Benefícios

✅ **Consistência**: Todas as respostas de erro seguem o mesmo formato
✅ **Logs centralizados**: Todas as exceções são logadas no GlobalExceptionHandler
✅ **Código limpo**: Controllers não precisam de try-catch manual
✅ **RFC 7807 compliant**: Segue padrão reconhecido da indústria
✅ **Debugging facilitado**: `exceptionType` ajuda a identificar a causa raiz
✅ **Mensagens claras**: Erros de validação incluem campo e valor rejeitado

## Exemplos de Respostas

### Erro de Validação (400)
```bash
POST /api/atendimento/solicitar
```
```json
{
  "timestamp": "2026-03-11T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Erro de validação nos campos da requisição",
  "path": "/api/atendimento/solicitar",
  "exceptionType": "MethodArgumentNotValidException",
  "fieldErrors": [
    {
      "field": "protocolo",
      "message": "não pode ser vazio",
      "rejectedValue": null
    }
  ]
}
```

### Regra de Negócio (422)
```bash
PUT /api/atendimento/finalizar
```
```json
{
  "timestamp": "2026-03-11T12:00:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Não é possível finalizar atendimento com status SOLICITADO",
  "path": "/api/atendimento/finalizar",
  "exceptionType": "IllegalStateException"
}
```

### Não Encontrado (404)
```bash
GET /api/atendimento/00000000-0000-0000-0000-000000000000
```
```json
{
  "timestamp": "2026-03-11T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Agregado não encontrado",
  "path": "/api/atendimento/00000000-0000-0000-0000-000000000000",
  "exceptionType": "NoSuchElementException"
}
```

### Erro Interno (500)
```json
{
  "timestamp": "2026-03-11T12:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Erro interno no servidor. Contate o suporte se o problema persistir.",
  "path": "/api/atendimento/solicitar",
  "exceptionType": "NullPointerException"
}
```

## Próximos Passos (Opcional)

1. **Exceções customizadas**: Criar exceções de domínio específicas (ex: `AtendimentoNotFoundException`)
2. **I18n**: Internacionalizar mensagens de erro
3. **Métricas**: Adicionar contadores de exceções por tipo (Micrometer)
4. **Dead Letter Queue**: Integrar com RabbitMQ DLQ para erros de processamento assíncrono

## Testando

### Validação
```bash
curl -X POST http://localhost:8080/api/atendimento/solicitar \
  -H "Content-Type: application/json" \
  -d '{}'
```

### UUID Inválido
```bash
curl -X GET http://localhost:8080/api/atendimento/invalid-uuid
```

### Agregado Não Existe
```bash
curl -X GET http://localhost:8080/api/atendimento/00000000-0000-0000-0000-000000000000
```

---

**Última atualização**: 2026-03-11
**Versão**: 1.0
