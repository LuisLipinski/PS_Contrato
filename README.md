# PS_Contrato — My Pet Admin

Microsserviço responsável pelo ciclo de vida dos contratos do My Pet Admin.

## Responsabilidade de domínio

O PS_Contrato é o dono das regras contratuais. Ele não cria empresas nem usuários e não orquestra onboarding.

Fluxo alvo:

```text
Onboarding Orchestrator
        |
        +--> PS_Empresa
        +--> PS_User
        +--> PS_Contrato

PS_Payment --pagamento confirmado--> PS_Contrato --status--> PS_Empresa
```

Nesta fase, a automação de API representa os futuros chamadores internos.

## Regras principais

- Um contrato é criado somente após a empresa existir no PS_Empresa.
- A criação recebe `empresaId` e `onboardingId`.
- O mesmo `onboardingId` para a mesma empresa é idempotente e retorna o mesmo contrato.
- Um `onboardingId` não pode ser reutilizado para outra empresa.
- Uma empresa pode possuir no máximo um contrato não inativo.
- O número do contrato segue `yyyyMM` + sequência mensal de 6 dígitos, por exemplo `202608000001`.
- A sequência é gerada atomicamente no PostgreSQL e suporta concorrência entre instâncias.
- Novo contrato nasce em `AGUARDANDO_PAGAMENTO`.
- A ativação não pode ser feita pelo endpoint administrativo de status.
- A ativação ocorre por confirmação semântica de pagamento.
- O mesmo `paymentId` é idempotente.
- Um pagamento diferente para um contrato já confirmado gera conflito.
- Retry tardio do pagamento original não reativa contrato `INATIVO`.
- O endpoint administrativo permite atualmente `ATIVO -> INATIVO`.

## Integração com PS_Empresa

O PS_Contrato usa OpenFeign para:

- validar a empresa em `GET /internal/empresas/{id}/status`;
- sincronizar o estado contratual em `PATCH /internal/contratos/status`.

A integração é fail-closed: falha de comunicação ou resposta inválida impede a operação local e retorna erro de integração.

O client Feign usa Apache HttpClient 5 para suportar corretamente chamadas `PATCH`.

## API

### Criar contrato

```http
POST /contratos
X-Internal-Key: <internal-key>
Content-Type: application/json

{
  "empresaId": "<uuid>",
  "onboardingId": "<uuid>"
}
```

### Confirmar pagamento

```http
POST /contratos/{id}/pagamentos/confirmacao
X-Internal-Key: <internal-key>
Content-Type: application/json

{
  "paymentId": "<uuid>",
  "paidAt": "2026-08-24T20:00:00"
}
```

Somente uma nova confirmação em contrato `AGUARDANDO_PAGAMENTO` pode promover o contrato para `ATIVO`.

### Alterar status administrativamente

```http
PATCH /contratos/{id}/status
X-Internal-Key: <internal-key>
Content-Type: application/json

{
  "statusId": 3
}
```

A ativação administrativa direta é bloqueada. Nesta fase, o fluxo administrativo permitido é `ATIVO -> INATIVO`.

### Buscar contratos

```http
GET /contratos
X-Internal-Key: <internal-key>
```

Suporta filtros, paginação e ordenação dinâmica.

## Segurança

As rotas de negócio são protegidas por `X-Internal-Key` nesta fase service-to-service.

A credencial é recebida exclusivamente por variável de ambiente e nunca deve ser versionada ou registrada em logs.

Health e info ficam públicos para operação; métricas e demais endpoints seguem a política de segurança do serviço.

## Variáveis de ambiente

### Produção / Render

Ative o profile de produção:

```text
SPRING_PROFILES_ACTIVE=prod
```

Configure:

```text
DB_URL=jdbc:postgresql://<host>:5432/<database>
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
INTERNAL_API_KEY=<shared-internal-secret>
PS_EMPRESA_URL=<base-url-do-ps-empresa>
```

`PORT` é lido automaticamente pelo Spring através da variável fornecida pela plataforma. Não é necessário fixar uma porta no Render.

`INTERNAL_API_KEY` deve possuir o mesmo valor configurado no PS_Empresa enquanto esta estratégia de autenticação interna estiver em uso.

Em Render, prefira rede privada para comunicação entre serviços e para PostgreSQL quando os recursos estiverem no mesmo workspace e região.

## Banco de dados

- PostgreSQL
- Flyway como fonte do schema
- `ddl-auto=validate`
- unicidade de contrato aberto por empresa
- idempotência de onboarding e pagamento protegida também por índices únicos
- sequência mensal de número de contrato persistida no banco

Migrations atuais:

```text
V1__init_contrato_schema.sql
V2__harden_existing_contrato_schema.sql
V3__onboarding_payment_idempotency_and_contract_sequence.sql
```

## Observabilidade

- logs estruturados por eventos de negócio;
- correlation ID;
- Actuator;
- Prometheus;
- ausência de PII e segredos nos logs.

Política de nível:

- `INFO`: mutações relevantes;
- `DEBUG`: leituras, retries idempotentes e integrações bem-sucedidas;
- `WARN`: rejeições de negócio, validação e segurança;
- `ERROR`: falhas inesperadas e dependências indisponíveis.

## Testes e CI

O pipeline executa:

- unit/component tests;
- JaCoCo;
- migrations em PostgreSQL real;
- Docker build;
- PS_Empresa + PS_Contrato efêmeros;
- lifecycle cross-service com Playwright;
- teste concorrente de geração de número contratual;
- artefatos de evidência e logs dos dois serviços em caso de falha.

O lifecycle integrado valida criação, onboarding idempotente, ativação por pagamento, retries, conflitos, inativação, não reativação e sincronização com Empresa.

## Consistência distribuída

A integração entre PS_Contrato e PS_Empresa ainda é síncrona e não usa transação distribuída, outbox ou broker. Essa decisão é intencional nesta fase para evitar overengineering.

Quando o fluxo de billing/webhooks exigir maior garantia de entrega, outbox/eventos poderão ser avaliados.

## Stack

- Java 21
- Spring Boot
- Spring Data JPA
- Spring Security
- Spring Cloud OpenFeign
- Apache HttpClient 5
- PostgreSQL
- Flyway
- Swagger/OpenAPI
- Actuator/Prometheus
- Docker

## Próximas integrações

- Onboarding Orchestrator como chamador oficial da criação de contrato;
- PS_Payment como origem oficial da confirmação de pagamento;
- autenticação/tenant isolation integrada ao futuro PS_Login, PS_User e API Gateway.
