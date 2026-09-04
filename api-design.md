# API Design

## Base URL
```bash
`/api`
```

## Endpoints
```bash
### GET /health
Check if service is running.
```

**Response:**
``` bash
json
{ "status": "UP" }
```

Get all customers
```bash
  Get /api/customers

```

Response:
```bash
json
[
  { "id": 1, "name": "John Doe", "email": "john@email.com", "phone": "1234567890" }
  {Also display all the customers}
]
```

POST /api /customers
```bash
Create a new customer.

Request:
json
{ "name": "Jane Smith", "email": "jane@email.com", "phone": "0987654321" }

Response:
{Created successful}
json
{ "id": 2, "name": "Jane Smith", "email": "jane@email.com", "phone": "0987654321" }

```

```bash

Delete customer

DELETE	/api/customers/{id}

```
```bash
Update customers

PUT	/api/customers/{id}

```

# In short 

```bash

GET    /api/customers     → Get all customers
GET    /api/customers/1   → Get customer with ID 1
POST   /api/customers     → Create a new customer
PUT    /api/customers/1   → Update customer with ID 1
DELETE /api/customers/1   → Delete customer with ID 1

```