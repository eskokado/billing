# Billing

Microserviço de **faturamento e pagamentos** do Algashop.

Responsável por emitir invoices, configurar meios de pagamento, capturar pagamentos no gateway e consultar faturas.

## Stack

- Java 21
- Spring Boot 3.4
- Spring Data JPA
- H2 (desenvolvimento)
- ModelMapper
- JaCoCo (cobertura mínima de 100%)

## Executar localmente

```bash
./gradlew bootRun
```

| Recurso | URL |
|---------|-----|
| API | http://localhost:8082 |
| H2 Console | http://localhost:8082/h2-console |

Credenciais padrão do H2: usuário `sa`, senha `123`.

## Testes

```bash
# testes unitários
./gradlew test

# testes de integração (*IT)
./gradlew integrationTest

# suite completa com verificação de cobertura
./gradlew check
```

A task `check` executa testes unitários, integração e falha se a cobertura JaCoCo for inferior a 100%.

Relatórios:

- HTML: `build/reports/jacoco/test/html/index.html`
- XML: `build/reports/jacoco/test/jacocoTestReport.xml`

## Arquitetura

```
src/main/java/com/eskcti/algashop/billing/
├── application/          # casos de uso (management, query)
├── domain/model/       # entidades, serviços de domínio, eventos
└── infrastructure/     # JPA, gateway de pagamento, listeners
```

### Principais capacidades

- **Emissão de invoice** (`InvoiceManagementApplicationService.generate`)
- **Processamento de pagamento** (`InvoiceManagementApplicationService.processPayment`)
- **Consulta por orderId** (`InvoiceQueryService`)
- **Eventos de domínio**: `InvoiceIssuedEvent`, `InvoicePaidEvent`, `InvoiceCanceledEvent`

### Camadas

| Camada | Responsabilidade |
|--------|------------------|
| `application` | Orquestração, DTOs de entrada/saída |
| `domain` | Regras de negócio, agregados, repositórios (interfaces) |
| `infrastructure` | Persistência, integrações externas, configuração |

O agregado `Invoice` estende `AbstractAuditableAggregateRoot` com auditoria JPA (`createdAt`, `createdBy`, `version`, etc.).

## Gateway de pagamento

Em desenvolvimento utiliza `PaymentGatewayServiceFakeImpl`, que simula captura e consulta de pagamentos.

## Cobertura via monorepo

Na raiz do Algashop:

```bash
python check_coverage.py billing
```
