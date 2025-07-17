# Order Service

The Order Service is a microservice responsible for managing customer orders in the Food Delivery Platform. It provides a RESTful API for order creation, retrieval, updating, and deletion, with advanced business logic and security features.

This service is part of the FP microservices ecosystem and requires other services (Auth Service, Discovery Server, Product Service) to be running to function properly.

## Table of Contents

- [Architecture and Components](#architecture-and-components)
- [Main Technologies](#main-technologies)
- [Key Components](#key-components)
  - [Entities and Data Model](#1-entities-and-data-model)
  - [REST Controllers](#2-rest-controllers)
  - [Business Services](#3-business-services)
  - [OAuth2 Security](#4-oauth2-security)
  - [Kafka Integration - Data Synchronization](#5-kafka-integration---data-synchronization)
  - [Aspect-Oriented Programming (AOP)](#6-aspect-oriented-programming-aop)
- [Development Configuration](#development-configuration)
  - [Prerequisites](#prerequisites)
  - [Environment Variables](#environment-variables)
  - [Database Setup](#database-setup)
  - [Kafka Setup](#kafka-setup)
- [API Endpoints](#api-endpoints)
  - [Authentication Required](#authentication-required)
  - [User Endpoints (Role USER)](#user-endpoints-role-user)
  - [Admin Endpoints (Role ADMIN)](#admin-endpoints-role-admin)
- [Kafka Integration](#kafka-integration)
  - [Consuming Product Events](#consuming-product-events)
  - [Supported Event Types](#supported-event-types)
  - [Consuming User Events](#consuming-user-events)
- [Aspect-Oriented Programming](#aspect-oriented-programming)
  - [Operation Auditing](#operation-auditing)
  - [Performance Monitoring](#performance-monitoring)
  - [Parameter Validation](#parameter-validation)
- [Advanced Business Logic](#advanced-business-logic)
  - [Order Entity Business Methods](#order-entity-business-methods)
  - [Async Validation](#async-validation)
- [Logging Configuration](#logging-configuration)
  - [Logback Configuration](#logback-configuration)
  - [Log Structure](#log-structure)
- [Running and Development](#running-and-development)
  - [Build and Run](#build-and-run)
  - [IDE Configuration](#ide-configuration)
  - [Health Checks](#health-checks)

---

## Functionality Overview

### Architecture and Components

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

```java
@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items = new ArrayList<>();

    private LocalDateTime createdAt;

    // Business logic methods
    public BigDecimal getTotal() {
        return items.stream()
                .map(Item::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addItem(Item item) {
        items.add(item);
        item.setOrder(this);
    }

    public boolean removeItemByProductId(Long productId) {
        return items.removeIf(item ->
            item.getProduct() != null &&
            Objects.equals(item.getProduct().getId(), productId));
    }
}
```

- **[Item.java](src/main/java/com/aspiresys/fp_micro_orderservice/order/Item/Item.java)** – Individual items within an order
- **[User.java](src/main/java/com/aspiresys/fp_micro_orderservice/user/User.java)** – Synced user entity
- **[Product.java](src/main/java/com/aspiresys/fp_micro_orderservice/product/Product.java)** – Synced product entity

#### 2. REST Controllers

**[OrderController.java](src/main/java/com/aspiresys/fp_micro_orderservice/order/OrderController.java)** – REST API for order management with role-based security

- The order management system provides complete CRUD operations with advanced business logic:

- **Order creation** with asynchronous validation of users and products

```java
@PostMapping("/me")
@PreAuthorize("hasRole('USER')")
@Auditable(operation = "CREATE_ORDER", entityType = "Order", logParameters = true, logResult = true)
@ExecutionTime(operation = "Create Order", warningThreshold = 3000, detailed = true)
@ValidateParameters(notNull = true, notEmpty = true, message = "Order data cannot be null or empty")
public ResponseEntity<AppResponse<OrderDTO>> createOrder(@RequestBody Order orderToCreate, Authentication authentication) {
    // Ensure products are synchronized before creating orders
    if (!productSyncService.isProductDatabaseSynchronized()) {
        productSyncService.requestProductSynchronization();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new AppResponse<>("Service temporarily unavailable. Product synchronization required.", null));
    }

    // Async validation and order creation logic
    OrderValidationService.OrderValidationResult validationResult =
        orderValidationService.validateOrderAsync(orderToCreate).get();

    if (!validationResult.isValid()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new AppResponse<>(validationResult.getErrorMessage(), null));
    }

    // Create and save order
    if (orderService.save(newOrder)) {
        OrderDTO orderDTO = orderMapper.toDTO(newOrder);
        return ResponseEntity.ok(new AppResponse<>("Order created successfully", orderDTO));
    }
    // ...error handling
}
```

- **Order retrieval** by user (authenticated) or all orders (admin)

```java
@GetMapping("/me")
@PreAuthorize("hasRole('USER')")
@Auditable(operation = "GET_USER_ORDERS", entityType = "Order", logResult = true)
@ExecutionTime(operation = "Get User Orders", warningThreshold = 1500)
public ResponseEntity<AppResponse<List<OrderDTO>>> getOrdersByUser(Authentication authentication) {
    // Check if products are synchronized before processing orders
    if (!productSyncService.isProductDatabaseSynchronized()) {
        productSyncService.requestProductSynchronization();
    }

    String email = ((Jwt) (authentication.getPrincipal())).getClaimAsString("sub");
    User user = userService.getUserByEmail(email);

    if (user == null) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new AppResponse<>("User does not have any orders", new ArrayList<>()));
    }

    List<Order> orders = orderService.findByUserId(user.getId());
    List<OrderDTO> orderDTOs = orderMapper.toDTOList(orders);
    return ResponseEntity.ok(new AppResponse<>("Orders retrieved for user: " + email, orderDTOs));
}
```

- **Order update** with orphan item handling

```java
@PutMapping("/me")
@PreAuthorize("hasRole('USER')")
@Transactional
@Auditable(operation = "UPDATE_ORDER", entityType = "Order", logParameters = true, logResult = true)
@ExecutionTime(operation = "Update Order", warningThreshold = 2500, detailed = true)
@ValidateParameters(notNull = true, message = "Order data cannot be null for update")
public ResponseEntity<AppResponse<OrderDTO>> updateOrder(@RequestBody Order orderToUpdate, Authentication authentication) {
    String email = ((Jwt) (authentication.getPrincipal())).getClaimAsString("sub");
    User user = userService.getUserByEmail(email);

    Order existingOrder = orderService.findById(orderToUpdate.getId()).orElse(null);
    if (existingOrder == null || !existingOrder.getUser().getId().equals(user.getId())) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AppResponse<>("Order not found", null));
    }

    // Validate order asynchronously
    OrderValidationService.OrderValidationResult validationResult =
        orderValidationService.validateOrderAsync(orderToUpdate).get();

    if (!validationResult.isValid()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AppResponse<>(validationResult.getErrorMessage(), null));
    }

    // Properly update items to avoid orphan removal issues
    existingOrder.getItems().clear();
    for (Item newItem : orderToUpdate.getItems()) {
        newItem.setId(null); // Clear ID to ensure it's treated as a new item
        newItem.setOrder(existingOrder);
        existingOrder.getItems().add(newItem);
    }

    if (orderService.update(existingOrder)) {
        OrderDTO orderDTO = orderMapper.toDTO(existingOrder);
        return ResponseEntity.ok(new AppResponse<>("Order updated successfully", orderDTO));
    }
    // ...error handling
}
```

- **Order deletion** with ownership validation

```java
@DeleteMapping("/me")
@PreAuthorize("hasRole('USER')")
@Auditable(operation = "DELETE_ORDER", entityType = "Order", logParameters = true, logResult = true)
@ExecutionTime(operation = "Delete Order", warningThreshold = 2000)
@ValidateParameters(notNull = true, message = "Order ID cannot be null for deletion")
public ResponseEntity<AppResponse<Boolean>> deleteOrder(@RequestParam Long id, Authentication authentication) {
    String email = ((Jwt) (authentication.getPrincipal())).getClaimAsString("sub");
    User user = userService.getUserByEmail(email);
    Order order = orderService.findById(id).orElse(null);

    if (user == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AppResponse<>("User not found", false));
    }
    if (order == null || !order.getUser().getId().equals(user.getId())) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AppResponse<>("Order not found", false));
    }

    try {
        orderService.deleteById(id);
        return ResponseEntity.ok(new AppResponse<>("Order deleted successfully", true));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AppResponse<>("Error deleting order: " + e.getMessage(), false));
    }
}
```

#### 3. Business Services

- **[OrderService.java](src/main/java/com/aspiresys/fp_micro_orderservice/order/OrderService.java)** – Order service interface
- **[OrderServiceImpl.java](src/main/java/com/aspiresys/fp_micro_orderservice/order/OrderServiceImpl.java)** – Order service implementation
- **[OrderValidationService.java](src/main/java/com/aspiresys/fp_micro_orderservice/order/OrderValidationService.java)** – Asynchronous order validation
- **[ProductSyncService.java](src/main/java/com/aspiresys/fp_micro_orderservice/product/ProductSyncService.java)** – Kafka-based product synchronization
- **[UserService.java](src/main/java/com/aspiresys/fp_micro_orderservice/user/UserService.java)** – User management

#### 4. OAuth2 Security

**[SecurityConfig.java](src/main/java/com/aspiresys/fp_micro_orderservice/config/security/SecurityConfig.java)** – OAuth2 Resource Server setup

- **JWT Token Validation** using the Authorization Server

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/actuator/**").permitAll()

                    // User-specific endpoints (must come BEFORE general ones)
                    .requestMatchers(HttpMethod.GET, "/orders/me").hasRole("USER")
                    .requestMatchers(HttpMethod.POST, "/orders/me").hasRole("USER")
                    .requestMatchers(HttpMethod.PUT, "/orders/me").hasRole("USER")
                    .requestMatchers(HttpMethod.DELETE, "/orders/me").hasRole("USER")

                    // Admin endpoints (AFTER specific ones)
                    .requestMatchers(HttpMethod.GET, "/orders/**").hasRole("ADMIN")

                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .build();
}
```

- **Role-based Access Control** (USER/ADMIN) and **Method-level Security** using @PreAuthorize

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        Collection<String> authorities = null;

        if (jwt.hasClaim("authorities")) {
            authorities = jwt.getClaimAsStringList("authorities");
        } else if (jwt.hasClaim("roles")) {
            authorities = jwt.getClaimAsStringList("roles");
        } else if (jwt.hasClaim("scope")) {
            String scope = jwt.getClaimAsString("scope");
            authorities = Arrays.asList(scope.split(" "));
        }

        if (authorities != null) {
            return authorities.stream()
                    .map(authority -> authority.startsWith("ROLE_") ? authority : "ROLE_" + authority)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        return Arrays.asList();
    });

    return converter;
}

// Usage in controllers:
@PreAuthorize("hasRole('USER')")
@PreAuthorize("hasRole('ADMIN')")
```

- **CORS Configuration** for frontend and gateway

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Allow frontend and gateway origins
    configuration.setAllowedOrigins(Arrays.asList(
        frontendUrl, // Frontend React
        gatewayUrl  // Gateway
    ));

    // Allow all necessary HTTP methods
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    // Allow all headers
    configuration.setAllowedHeaders(Arrays.asList("*"));

    // Allow cookies and credentials
    configuration.setAllowCredentials(true);

    // Configure for all routes
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
}
```

#### 5. Kafka Integration - Data Synchronization

**[KafkaConsumerConfig.java](src/main/java/com/aspiresys/fp_micro_orderservice/kafka/config/KafkaConsumerConfig.java)** – Kafka consumer configuration

- **Kafka Integration** for receiving product and user events

```java
@Configuration
@EnableKafka
@Log
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, ProductMessage> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        // Resilience configurations
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductMessage> productKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ProductMessage> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Error handling with retries
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            new FixedBackOff(1000L, 3) // 3 retries with 1 second interval
        );

        factory.setCommonErrorHandler(errorHandler);
        factory.setConcurrency(1);

        return factory;
    }
}
```

- **Async Product Sync** with validation

**[ProductConsumerService.java](src/main/java/com/aspiresys/fp_micro_orderservice/kafka/consumer/ProductConsumerService.java)** – Product events consumer

```java
@Service
@Log
public class ProductConsumerService {

    @Autowired
    private ProductSyncService productSyncService;

    @KafkaListener(topics = "${kafka.topic.product:product}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeProductMessage(
            @Payload ProductMessage productMessage,
            @Header(value = KafkaHeaders.KEY, required = false) String key,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition) {

        try {
            log.info("KAFKA: Received message from topic: " + topic +
                    ", partition: " + partition + ", key: " + key);
            log.info("KAFKA: Message details - Event: " + productMessage.getEventType() +
                    ", Product: " + productMessage.getName() +
                    " (ID: " + productMessage.getId() + ")");

            // Process message based on event type
            processProductMessage(productMessage);

            log.info("KAFKA: Successfully processed message for product ID: " + productMessage.getId());

        } catch (Exception e) {
            log.severe("KAFKA ERROR: Failed to process product message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processProductMessage(ProductMessage productMessage) {
        switch (productMessage.getEventType()) {
            case "CREATED":
            case "UPDATED":
            case "INITIAL_LOAD":
                productSyncService.saveOrUpdateProduct(productMessage);
                break;
            case "DELETED":
                productSyncService.deleteProduct(productMessage.getId());
                break;
            default:
                log.warning("Unknown event type: " + productMessage.getEventType());
        }
    }
}
```

- **Event-driven Architecture** for consistency

Event types supported: **CREATED**, **UPDATED**, **DELETED**, **INITIAL_LOAD** for both products and users.

#### 6. Aspect-Oriented Programming (AOP)

**[AOP Annotations](src/main/java/com/aspiresys/fp_micro_orderservice/aop/annotation/)** – Aspect-Oriented Programming features

- **AOP Auditing** for critical operation traceability

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String operation() default "";
    String entityType() default "";
    boolean logParameters() default true;
    boolean logResult() default false;
}

// Usage example in OrderController:
@Auditable(operation = "CREATE_ORDER", entityType = "Order", logParameters = true, logResult = true)
public ResponseEntity<AppResponse<OrderDTO>> createOrder(@RequestBody Order order) {
    // Method implementation - auditing handled automatically by AOP
}
```

- **Performance Monitoring** with execution time metrics

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecutionTime {
    String operation() default "";
    long warningThreshold() default 5000;
    boolean detailed() default false;
}

// Usage example:
@ExecutionTime(operation = "Create Order", warningThreshold = 3000, detailed = true)
public ResponseEntity<AppResponse<OrderDTO>> createOrder(@RequestBody Order order) {
    // Method execution time is automatically measured and logged
}
```

- **Async Processing** for complex validations

```java
@Async
public CompletableFuture<OrderValidationResult> validateOrderAsync(Order order) {
    // Validation paralela de usuario y productos
    CompletableFuture<User> userValidation = validateUserAsync(order.getUser());
    CompletableFuture<Boolean> productValidation = validateProductsAsync(order.getItems());

    return CompletableFuture.allOf(userValidation, productValidation)
            .thenApply(v -> new OrderValidationResult(userValidation.join(), productValidation.join()));
}
```

- **Transaction Management** for data operations

```java
@PutMapping("/me")
@PreAuthorize("hasRole('USER')")
@Transactional  // Ensures data consistency
@Auditable(operation = "UPDATE_ORDER", entityType = "Order")
public ResponseEntity<AppResponse<OrderDTO>> updateOrder(@RequestBody Order orderToUpdate) {
    // All database operations within this method are transactional
    // Automatic rollback on exceptions
}
```

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

The service listens to the `product` topic to synchronize product information:

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

The service listens to the `user` topic to synchronize user information:

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

### Order Entity Business Methods

The Order entity includes sophisticated business logic for order management:

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

The service uses asynchronous validation to improve performance:

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

### Health Checks

```http
GET /actuator/health
GET /actuator/info
```

---
