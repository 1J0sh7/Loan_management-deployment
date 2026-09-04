# HTTP API Lab - Postman Collection and API Notes

## Base URL
http://localhost:8081/api


```bash

## Postman Collection

### Export Your Collection from Postman

1. Open Postman
2. Click the **Collections** tab
3. Click the **...** (three dots) next to your collection
4. Select **Export**
5. Choose **Collection v2.1**
6. Click **Export**
7. Save as `postman-collection.json`

```

## Endpoints Summary

```bash
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Check if service is running |
| GET | `/customers` | Get all customers |
| GET | `/customers/{id}` | Get customer by ID |
| POST | `/customers` | Create a new customer |
| PUT | `/customers/{id}` | Update an existing customer |
| DELETE | `/customers/{id}` | Delete a customer |

```

## Request and Response Examples
```bash
### 1. Health Check
**GET** `/health`

**Request:**
```http
GET http://localhost:8081/api/health



Response:

json
"UP"
2. Get All Customers
GET /customers

Request:

http
GET http://localhost:8081/api/customers



Response:

json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@email.com",
    "phone": "1234567890"
  }
]
3. Get Customer by ID
GET /customers/{id}

Request:

http
GET http://localhost:8081/api/customers/1



Response (200 OK):

json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@email.com",
  "phone": "1234567890"
}
Response (404 Not Found):

json
{
  "timestamp": "2026-08-06T...",
  "status": 404,
  "error": "Not Found"
}
4. Create a Customer
POST /customers

Request:

http
POST http://localhost:8081/api/customers
Content-Type: application/json

{
  "name": "Jane Smith",
  "email": "jane@email.com",
  "phone": "0987654321"
}



Response (201 Created):

json
{
  "id": 2,
  "name": "Jane Smith",
  "email": "jane@email.com",
  "phone": "0987654321"
}
5. Update a Customer
PUT /customers/{id}

Request:

http
PUT http://localhost:8081/api/customers/1
Content-Type: application/json

{
  "name": "John Doe Updated",
  "email": "john.updated@email.com",
  "phone": "1111111111"
}



Response (200 OK):

json
{
  "id": 1,
  "name": "John Doe Updated",
  "email": "john.updated@email.com",
  "phone": "1111111111"
}
6. Delete a Customer
DELETE /customers/{id}

Request:

http
DELETE http://localhost:8081/api/customers/1



Response: (204 No Content)

cURL Commands
Health Check
bash
curl http://localhost:8081/api/health

Get All Customers
bash
curl http://localhost:8081/api/customers

Get Customer by ID
bash
curl http://localhost:8081/api/customers/1

Create a Customer
bash
curl -X POST http://localhost:8081/api/customers \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane Smith","email":"jane@email.com","phone":"0987654321"}'
  
Update a Customer
bash
curl -X PUT http://localhost:8081/api/customers/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe Updated","email":"john.updated@email.com","phone":"1111111111"}'
  
Delete a Customer
bash
curl -X DELETE http://localhost:8081/api/customers/1

Status Codes 
Code	Meaning	When Used
200	OK	GET, PUT success
201	Created	POST success
204	No Content	DELETE success
400	Bad Request	Invalid input
404	Not Found	Resource not found
500	Internal Server Error	Server error


What I Learned
Concept of what I Did
HTTP       >    Methods	                    | GET, POST, PUT, DELETE
Status     >    Codes	                    | 200, 201, 204, 404
Request    >    Body	                    | JSON data in POST/PUT
Path       >    Variables	               | {id} in URL
Postman	   >    Tested all endpoints
cURL	   >    Tested from command line


```