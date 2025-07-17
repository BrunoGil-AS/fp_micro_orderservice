# Order Service

Order management microservice that is part of the FP microservices ecosystem. This service provides full functionality for creating, retrieving, updating, and deleting purchase orders, integrating with authentication, product, and user services via OAuth2 and Kafka messaging.

## Architecture and Components

### Main Technologies

- **Spring Boot 3.5.0** – Core framework
- **Spring Security** – OAuth2 Resource Server with JWT
- **Spring Data JPA** – Persistence with MySQL
- **Spring WebFlux** – Reactive programming
- **Spring Kafka** – Asynchronous messaging
- **Netflix Eureka Client** – Service Discovery
- **Spring Boot AOP** – Aspect-Oriented Programming
- **Lombok** – Reduces boilerplate code

### Key Components

#### 1. Entities and Data Model

- **[Order.java](src/main/java/com/aspiresys/fp_micro_orderservice/order/Order.java)** – Main order entity with complex business logic
- **[Item.java](src/main/java/com/aspiresys/fp_micro_orderservice/order/Item/Item.java)** – Individual items within an order
- **[User.java](src/main/java/com/aspiresys/fp_micro_orderservice/user/User.java)** – Synced user entity
- **[Product.java](src/main/java/com/aspiresys/fp_micro_orderservice/product/Product.java)** – Synced product entity

#### 2. REST Controllers

- **[OrderController.java](src/main/java/com/aspiresys/fp_micro_orderservice/order/OrderController.java)** – REST API for order management with role-based security

#### 3. Business Services

- **[OrderService.java](src/main/java/com/aspiresys/fp_micro_orderservice/order/OrderService.java)** – Order service interface
- **[OrderServiceImpl.java](src/main/java/com/aspiresys/fp_micro_orderservice/order/OrderServiceImpl.java)** – Order service implementation
- **[OrderValidationService.java](src/main/java/com/aspiresys/fp_micro_orderservice/order/OrderValidationService.java)** – Asynchronous order validation
- **[ProductSyncService.java](src/main/java/com/aspiresys/fp_micro_orderservice/product/ProductSyncService.java)** – Kafka-based product synchronization
- **[UserService.java](src/main/java/com/aspiresys/fp_micro_orderservice/user/UserService.java)** – User management

#### 4. Security Configuration

- **[SecurityConfig.java](src/main/java/com/aspiresys/fp_micro_orderservice/config/SecurityConfig.java)** – OAuth2 Resource Server setup

#### 5. Kafka Integration

- **[KafkaConsumerConfig.java](src/main/java/com/aspiresys/fp_micro_orderservice/config/KafkaConsumerConfig.java)** – Kafka consumer configuration
- **[ProductConsumerService.java](src/main/java/com/aspiresys/fp_micro_orderservice/product/ProductConsumerService.java)** – Product events consumer
- **[UserConsumerService.java](src/main/java/com/aspiresys/fp_micro_orderservice/user/UserConsumerService.java)** – User events consumer

#### 6. Aspect-Oriented Programming (AOP)

- **[Auditable.java](src/main/java/com/aspiresys/fp_micro_orderservice/aop/annotation/Auditable.java)** – Annotation for auditing operations
- **[ExecutionTime.java](src/main/java/com/aspiresys/fp_micro_orderservice/aop/annotation/ExecutionTime.java)** – Annotation for execution time measurement
- **[ValidateParameters.java](src/main/java/com/aspiresys/fp_micro_orderservice/aop/annotation/ValidateParameters.java)** – Annotation for parameter validation

## Main Features

### [Order Management](src/main/java/com/aspiresys/fp_micro_orderservice/order/OrderService.java)

- **Order creation** with asynchronous validation of users and products
  ToChange: add code snippet
- **Order retrieval** by user (authenticated) or all orders (admin)
  ToChange: add code snippet
- **Order update** with orphan item handling
  ToChange: add code snippet
- **Order deletion** with ownership validation
  ToChange: add code snippet
- **Automatic calculation** of totals and item management
  add code snippet
  ToChange: insert link to controller

### OAuth2 Security

- **JWT Token Validation** using the Authorization Server
- **Role-based Access Control** (USER/ADMIN)
- **CORS Configuration** for frontend and gateway
- **Method-level Security** using @PreAuthorize
  ToChange: add code snippet of security configuration

### Data Synchronization

- **Kafka Integration** for receiving product and user events
- **Async Product Sync** with validation
- **Event-driven Architecture** for consistency
  ToChange: add code snippet of kafka integration

### Advanced Features

- **AOP Auditing** for critical operation traceability
- **Performance Monitoring** with execution time metrics
- **Async Processing** for complex validations
- **Transaction Management** for data operations
  ToChange: Insert code snippet of AOP features and make a link to the AOP section

## Development Configuration

### Prerequisites

1. **Java 17+**
2. **Maven 3.6+**
3. **MySQL 8.0+**
4. **Apache Kafka 2.8+**
5. **Authorization Server** (fp_micro_authservice) running
6. **Discovery Server** (fp_micro_discoveryserver) running
7. **Product Service** (fp_micro_productservice) running

### Environment Variables

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/fp_orderdb
spring.datasource.username=root
spring.datasource.password=your_password

# OAuth2 Configuration
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/oauth2/jwks

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
kafka.topic.product=product
kafka.topic.user=user

# Service Discovery
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

# CORS Configuration
service.env.frontend.server=http://localhost:3000
service.env.gateway.server=http://localhost:8080
```

### Database Setup

```sql
-- Create the database
CREATE DATABASE fp_orderdb;

-- Tables are auto-generated by JPA/Hibernate
-- Main structure:
-- - orders (id, user_id, created_at, total)
-- - items (id, order_id, product_id, quantity, price)
-- - products (id, name, description, price, stock, brand, category_id)
-- - users (id, email, first_name, last_name, phone)
```

### Kafka Setup

```bash
# Create required topics
kafka-topics.sh --create --topic product --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
kafka-topics.sh --create --topic user --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

## API Endpoints

### Authentication Required

All endpoints require a valid JWT token from the Authorization Server:

```text
Authorization: Bearer <jwt_token>
```

### User Endpoints (Role USER)

#### Get User Orders

```http
GET /orders/me
Authorization: Bearer <jwt_token>
```

**Response:**

```json
{
  "message": "Orders retrieved for user: user@example.com",
  "data": [
    {
      "id": 1,
      "createdAt": "2024-01-15T10:30:00",
      "total": 150.0,
      "items": [
        {
          "id": 1,
          "quantity": 2,
          "price": 75.0,
          "product": {
            "id": 1,
            "name": "Product A",
            "price": 75.0
          }
        }
      ]
    }
  ]
}
```

#### Create Order

```http
POST /orders/me
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "items": [
    { "quantity": 2, "product": { "id": 1 } },
    { "quantity": 1, "product": { "id": 2 } }
  ]
}
```

#### Update Order

```http
PUT /orders/me
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "id": 1,
  "items": [
    { "quantity": 3, "product": { "id": 1 } }
  ]
}
```

#### Delete Order

```http
DELETE /orders/me?id=1
Authorization: Bearer <jwt_token>
```

### Admin Endpoints (Role ADMIN)

#### Get All Orders

```http
GET /orders/
Authorization: Bearer <admin_jwt_token>
```

#### Find Order by ID

```http
GET /orders/find?id=1
Authorization: Bearer <admin_jwt_token>
```

## Kafka Integration

### Consuming Product Events

```java
@KafkaListener(topics = "${kafka.topic.product:product}")
public void consumeProductMessage(@Payload ProductMessage productMessage) {
    // Handles CREATED, UPDATED, DELETED, INITIAL_LOAD events
    productSyncService.saveOrUpdateProduct(productMessage);
}
```

### Supported Event Types

- **CREATED** – New product created
- **UPDATED** – Product updated
- **DELETED** – Product deleted
- **INITIAL_LOAD** – Initial product load

### Consuming User Events

```java
@KafkaListener(topics = "${kafka.topic.user:user}")
public void consumeUserMessage(@Payload UserMessage userMessage) {
    // Handles user events to maintain sync
    userService.processUserEvent(userMessage);
}
```

## Aspect-Oriented Programming

### Operation Auditing

```java
@Auditable(operation = "CREATE_ORDER", entityType = "Order", logParameters = true, logResult = true)
public ResponseEntity<AppResponse<OrderDTO>> createOrder(@RequestBody Order order) {
    // Auditing is handled automatically by AOP
}
```

### Performance Monitoring

```java
@ExecutionTime(operation = "Create Order", warningThreshold = 3000, detailed = true)
public ResponseEntity<AppResponse<OrderDTO>> createOrder(@RequestBody Order order) {
    // Execution time is automatically logged
}
```

### Parameter Validation

```java
@ValidateParameters(notNull = true, notEmpty = true, message = "Order data cannot be null or empty")
public ResponseEntity<AppResponse<OrderDTO>> createOrder(@RequestBody Order order) {
    // Validation is performed before method execution
}
```

## Advanced Business Logic

### Order Entity

```java
public class Order {
    public BigDecimal getTotal() {
        return items.stream()
                .map(Item::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addItem(Item item) {
        // Logic to add items with validation
    }

    public boolean removeItemByProductId(Long productId) {
        // Logic to remove item by product ID
    }

    public void setItemsFromProducts(List<Product> products, List<Integer> quantities) {
        // Set items from product list and quantities
    }
}
```

### Async Validation

```java
@Async
public CompletableFuture<OrderValidationResult> validateOrderAsync(Order order) {
    CompletableFuture<User> userValidation = validateUserAsync(order.getUser());
    CompletableFuture<Boolean> productValidation = validateProductsAsync(order.getItems());

    return CompletableFuture.allOf(userValidation, productValidation)
            .thenApply(v -> new OrderValidationResult(userValidation.join(), productValidation.join()));
}
```

## Logging Configuration

### Logback Configuration

Centralized logging via Config Server:

- **Application Logs**: `logs/order-service/application.log`
- **Error Logs**: `logs/order-service/error.log`
- **Audit Logs**: Included in application logs (INFO level)
- **Performance Logs**: Warnings for slow operations

### Log Structure

```text
[TIMESTAMP] [LEVEL] [THREAD] [LOGGER] - [MESSAGE]
- Kafka events: KAFKA: prefix
- AOP operations: AOP: prefix
- Security events: SECURITY: prefix
- Business logic: ORDER: prefix
```

## Running and Development

### Build and Run

```bash
# Compile project
mvn clean compile

# Run tests
mvn test

# Start application
mvn spring-boot:run
```

### IDE Configuration

1. **Default port**: 8083
2. **Active profile**: development
3. **JVM Options**: `-Xmx512m -Xms256m`
4. **Environment variables**: Set per environment

### Health Checks

```http
GET /actuator/health
GET /actuator/info
```

## Main Dependencies

### Spring Framework

- `spring-boot-starter-data-jpa` – JPA persistence
- `spring-boot-starter-webflux` – Reactive web
- `spring-boot-starter-security` – OAuth2 security
- `spring-boot-starter-aop` – Aspect-oriented programming
- `spring-cloud-starter-netflix-eureka-client` – Service discovery

### Kafka and Messaging

- `spring-kafka` – Kafka integration

### Database

- `mysql-connector-j` – MySQL driver
- `HikariCP` – Connection pool

### Utilities

- `lombok` – Boilerplate reduction
- `mapstruct` – DTO mapping

## Monitoring and Observability

### Available Metrics

- **Endpoint response time**
- **Operation throughput**
- **Errors** by type and endpoint
- **Kafka events** processed
- **Validation** success/failure

### Audit Logs

All critical operations are automatically audited:

- Order create, update, delete
- Protected endpoint access
- Kafka-based data sync
- Validation and security errors

## Troubleshooting

### Common Issues

#### Product Sync Error

```text
Service temporarily unavailable. Product synchronization required.
```

**Solution**: Ensure Product Service is running and sending Kafka events.

#### Invalid JWT Token

```text
401 Unauthorized
```

**Solution**: Ensure Authorization Server is running and token is valid.

#### Database Error

```text
Could not open JPA EntityManager for transaction
```

**Solution**: Check MySQL configuration and that `fp_orderdb` exists.

### Debug Logs

For detailed debugging, set logging level:

```properties
logging.level.com.aspiresys.fp_micro_orderservice=DEBUG
logging.level.org.springframework.kafka=DEBUG
logging.level.org.springframework.security=DEBUG
```

---

This service is part of the FP microservices ecosystem and requires other services (Auth Service, Discovery Server, Product Service) to be running to function properly.
