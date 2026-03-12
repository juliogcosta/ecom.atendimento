# Próximos Passos - Possibilidades de Expansão

## 1. Testes Unitários
- Testar Aggregate (process/apply methods)
- Testar Value Objects (validações)
- Testar Commands (validações)
- Testar Events (serialização/desserialização)

## 2. Testes de Integração
- Testar endpoints REST end-to-end
- Testar persistência no Event Store
- Testar integração com RabbitMQ
- Testar reconstitução de agregados

## 3. Query Side (Read Model)
- Criar projeções otimizadas para leitura
- Implementar repositórios JPA
- Criar endpoints GET para consultas
- Implementar event handlers para atualizar projeções

## 4. Documentação OpenAPI
- Adicionar SpringDoc/Swagger
- Documentar todos os endpoints
- Adicionar exemplos de request/response
- Configurar UI do Swagger

## 5. Observabilidade
- Adicionar tracing distribuído (Zipkin/Jaeger)
- Configurar métricas customizadas
- Adicionar logs estruturados
- Dashboard de monitoramento

## 6. Docker Compose
- Criar compose com PostgreSQL
- Adicionar RabbitMQ no compose
- Adicionar aplicação no compose
- Scripts de inicialização

## 7. CI/CD
- Configurar GitHub Actions
- Testes automatizados
- Build e deploy automatizado
- Versionamento semântico

## 8. Segurança
- Adicionar autenticação JWT
- Implementar autorização por roles
- Rate limiting
- Validação de input avançada
