# Spring Boot Request Flow

## Endpoint: GET /hello

## Flow Diagram
``` bash
Browser (http://localhost:8081/hello)
↓
Spring Boot DispatcherServlet
↓
HelloController.sayHello()
↓
HelloService.getHelloMessage()
↓
Returns "Hello from Spring Boot Service!"
↓
Browser displays response

```

## Step-by-Step Explanation

1. **Browser sends request**: `GET http://localhost:8081/hello`

2. **Spring Boot receives request**: DispatcherServlet handles all incoming requests

3. **Maps to controller**: Finds `@GetMapping("/hello")` in `HelloController`

4. **Controller calls service**: `helloService.getHelloMessage()`

5. **Service returns data**: "Hello from Spring Boot Service!"

6. **Response sent back**: Spring Boot converts return value to HTTP response

7. **Browser displays**: Shows "Hello from Spring Boot Service!"

## Components
``` bash 
| Component | Purpose |
|-----------|---------|
| Controller | Handles HTTP requests |
| Service | Contains business logic |
| Repository | Data access (not yet used) |

```