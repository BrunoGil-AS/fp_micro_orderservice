# Solución a los Warning de Anidamiento JSON - DTOs Implementation

## Problema Identificado

El error que estabas experimentando era:

```
Document nesting depth (1001) exceeds the maximum allowed (1000, from `StreamWriteConstraints.getMaxNestingDepth()`)
```

Este problema ocurre debido a referencias circulares infinitas entre las entidades JPA:

- `Order` -> `List<Item>` -> `Item.order` -> `Order` -> `List<Item>` (ciclo infinito)
- `User` -> `List<Order>` -> `Order.user` -> `User` -> `List<Order>` (ciclo infinito)

Aunque se usaban `@JsonManagedReference` y `@JsonBackReference`, aún había problemas de profundidad de anidamiento.

## Solución Implementada

### 1. Creación de DTOs (Data Transfer Objects)

#### `OrderDTO.java`

- Contiene solo la información esencial de una orden
- Incluye únicamente el email del usuario para identificación
- Lista de ItemDTO en lugar de entidades Item
- Campo `total` calculado
- Enfoque en información de la orden y productos

#### `ItemDTO.java`

- Contiene información esencial del item
- Incluye información del producto de forma plana
- Campo `subtotal` calculado
- Sin referencias circulares

#### `CreateOrderDTO.java`

- DTO específico para la creación de órdenes
- Contiene solo `productId` y `quantity` por item
- Estructura simplificada para entrada de datos

### 2. Mapper para Conversión

#### `OrderMapper.java`

- Convierte entre entidades y DTOs
- Maneja las conversiones de forma segura
- Evita referencias circulares
- Calcula campos derivados (total, subtotal)

### 3. Actualización del Controller

#### Cambios en `OrderController.java`

- **GET endpoints**: Ahora devuelven `OrderDTO` en lugar de `Order`
- **POST /me**: Acepta `CreateOrderDTO` y devuelve `OrderDTO`
- **PUT /me**: Acepta `CreateOrderDTO` con `@RequestParam Long orderId` y devuelve `OrderDTO`
- **DELETE /me**: Sin cambios (no devuelve Order entity)

## Beneficios de esta Solución

### 1. **Elimina Referencias Circulares**

- Los DTOs no contienen referencias a otras entidades
- Información del usuario y productos se incluye de forma plana

### 2. **Reduce el Tamaño de la Respuesta**

- Solo se envía la información necesaria
- Elimina metadatos de JPA innecesarios

### 3. **Mejora la Seguridad**

- Control total sobre qué información se expone
- Evita la exposición accidental de datos sensibles

### 4. **Facilita el Versionado de API**

- Los DTOs pueden evolucionar independientemente de las entidades
- Facilita el mantenimiento de compatibilidad hacia atrás

### 5. **Mejor Performance**

- Menos datos transferidos por la red
- Serialización/deserialización más eficiente

## Estructura de Datos

### Antes (con entidades):

```json
{
  "id": 1,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "orders": [
      {
        "id": 1,
        "user": { ... } // Referencia circular
      }
    ]
  },
  "items": [
    {
      "id": 1,
      "order": { ... }, // Referencia circular
      "product": {
        "id": 1,
        "items": [ ... ] // Referencia circular
      }
    }
  ]
}
```

### Después (con DTOs):

```json
{
  "id": 1,
  "userEmail": "user@example.com",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Product Name",
      "productPrice": 29.99,
      "quantity": 2,
      "subtotal": 59.98
    }
  ],
  "total": 59.98,
  "createdAt": "2025-07-07T10:30:00"
}
```

## Formato de Request para Crear Órdenes

### Antes:

```json
{
  "items": [
    {
      "product": {
        "id": 1,
        "name": "Product Name",
        "price": 29.99
      },
      "quantity": 2
    }
  ]
}
```

### Después:

```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

## Próximos Pasos Recomendados

1. **Validación de DTOs**: Añadir anotaciones de validación (`@Valid`, `@NotNull`, etc.)
2. **Documentación API**: Actualizar la documentación Swagger/OpenAPI
3. **Tests**: Actualizar los tests existentes para usar los nuevos DTOs
4. **Frontend**: Actualizar el frontend para usar la nueva estructura de datos

Esta solución elimina completamente los warnings de anidamiento JSON y mejora significativamente la arquitectura de tu API.
