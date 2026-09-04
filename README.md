# Backend Intern Campus

Spring Boot REST API for customer, loan-product, loan-application, repayment, and audit-trail management. The application runs with PostgreSQL in Docker.

## Requirements

- Java 21
- Docker
- Docker Compose

## Configuration

The Docker setup reads database settings from `.env`. Keep credentials out of source control and provide your local values before starting the application.

The default development profile uses:

- Application URL: `http://localhost:8081`
- PostgreSQL database: `customer_db`
- PostgreSQL container: `postgres_db`

## Run with Docker

From this directory:

```bash
docker compose -f docker-compose.yaml up --build
```

Stop the application:

```bash
docker compose -f docker-compose.yaml down
```

Rebuild after code changes:

```bash 
./gradlew clean build

docker compose up --build -d 

docker compose up --build
```

## Run and test locally

Build the project and run all existing tests:

```bash
./gradlew clean build
```

Run tests only:

```bash
./gradlew test
```

Generate the JaCoCo test report:

```bash
./gradlew test jacocoTestReport
```

The HTML report is created under:

```text
build/reports/jacoco/test/html/index.html
```

## API documentation and health

When the application is running:

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- Health endpoint: `http://localhost:8081/api/health`

Most customer and loan-application operations require a JWT obtained through the authentication endpoints. Admin operations require an account with the `ADMIN` role.

## Main endpoints

### Authentication

```text
POST /api/auth/register
POST /api/auth/login
```

### Customers

```text
GET    /api/v1/customers
GET    /api/v1/customers/{id}
POST   /api/v1/customers
PUT    /api/v1/customers/{id}
DELETE /api/v1/customers/{id}
```

Customer deletion is a soft delete. The record remains in the database but is hidden from normal customer queries.

### Loan products

```text
GET    /api/v1/loan-products
GET    /api/v1/loan-products/{id}
POST   /api/v1/loan-products                 (ADMIN)
PUT    /api/v1/loan-products/{id}            (ADMIN)
DELETE /api/v1/loan-products/{id}             (ADMIN)
```

Loan-product deletion is also a soft delete:

- Active products are visible to normal users.
- Deleted products remain stored for audit purposes.
- Deleted products are hidden from normal list and ID lookup operations.
- Deleting an already inactive product returns `404 Not Found`.

### Loan applications and repayments

```text
POST /api/v1/loan-applications
GET  /api/v1/loan-applications/{id}
GET  /api/v1/loan-applications/customer/{customerId}

PUT  /api/v1/loan-applications/{id}/approve      (ADMIN)
PUT  /api/v1/loan-applications/{id}/reject       (ADMIN)
PUT  /api/v1/loan-applications/{id}/disburse     (ADMIN)

POST /api/v1/loan-applications/{id}/repayments
GET  /api/v1/loan-applications/admin/all         (ADMIN)
GET  /api/v1/loan-applications/{id}/history      (ADMIN)
```

The normal loan lifecycle is:

```text
PENDING -> APPROVED -> DISBURSED -> ACTIVE -> COMPLETED
```

An application can become `REJECTED`. A disbursed or active loan becomes `OVERDUE` after its final term date if it still has a remaining balance.

Repayments are flexible. The customer can pay the calculated minimum, pay more, or skip a month and catch up later. Extra payments reduce the remaining balance and can complete the loan earlier. The loan must be fully paid before the final term deadline to avoid the `OVERDUE` status.

## Audit trail

Every loan status transition is stored in `loan_application_history` with:

- Loan application ID
- Previous status
- New status
- Actor who made the change
- Reason, when applicable
- Timestamp

The initial application record is stored as `null -> PENDING`. Automatic overdue transitions are attributed to `SYSTEM`.

Administrators can view a loan's status history through:

```text
GET /api/v1/loan-applications/{id}/history
```

## Database access

Check the PostgreSQL container:

```bash
docker exec -it postgres_db psql -U postgres -d customer_db
```

Example queries:

```sql
SELECT * FROM customers;
SELECT * FROM loan_products;
SELECT * FROM loan_applications;
SELECT * FROM loan_application_history
ORDER BY changed_at DESC;
```

Flyway applies the database migrations at startup, including the audit-trail migration.

## Example requests

Create a customer:

```bash
curl -X POST http://localhost:8081/api/v1/customers \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","phone":"1234567890"}'
```

List active loan products:

```bash
curl http://localhost:8081/api/v1/loan-products
```

Get an administrator's loan history:

```bash
curl http://localhost:8081/api/v1/loan-applications/1/history \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

## Troubleshooting

Check application logs:

```bash
docker compose -f docker-compose.yaml logs app
```

Check PostgreSQL logs:

```bash
docker logs postgres_db
```

If the application cannot connect to PostgreSQL, confirm that the `.env` database values match the Docker Compose configuration and that the database container is healthy.

If email-related behavior is being tested, use valid SendGrid configuration for the development environment. The automated integration tests mock email delivery and do not require a live SendGrid request.

## Project structure

```text
.
├── Dockerfile
├── docker-compose.yaml
├── .env
├── build.gradle
├── gradlew
├── src/main/java
├── src/main/resources
├── src/test/java
└── README.md
```


## Repayment Model

The system uses a **dynamic minimum payment** model:

### Core Rules

1. **Interest Calculation** — Interest is calculated on disbursement:
   totalPayable = principal + (principal × interestRate / 100)

   #

2. **Monthly Minimum Payment** — Calculated dynamically:
   monthlyMinimum = remainingBalance / remainingMonths

#

3. **Payment Validation**:
- Payment must be ≥ monthlyMinimum
- Payment must be ≤ remainingBalance
- Payment must be > 0
#
4. **Loan Status**:
- **DISBURSED** — Loan is active, first payment not yet made
- **ACTIVE** — Repayments in progress
- **COMPLETED** — Fully repaid (remainingBalance = 0)
- **OVERDUE** — Term ended with positive balance
#
5. **Repayment Flow**:
- User makes a payment
- System validates amount
- Remaining balance is reduced
- If balance = 0 → loan becomes COMPLETED
- If first payment → loan becomes ACTIVE
#
6. **Schedule**:
- No scheduled installments are generated in the database
- Only actual payment transactions are stored
- Minimum payment adjusts dynamically with each payment
