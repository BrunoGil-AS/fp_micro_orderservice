package com.aspiresys.fp_micro_orderservice.order;

import com.aspiresys.fp_micro_orderservice.common.dto.AppResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;
import com.aspiresys.fp_micro_orderservice.product.Product;
import com.aspiresys.fp_micro_orderservice.user.User;
import com.aspiresys.fp_micro_orderservice.order.Item.Item;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import reactor.core.publisher.Mono;




class OrderControllerTest {

    @Mock
    private OrderService orderService;


    @InjectMocks
    private OrderController orderController;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(webClientBuilder.build()).thenReturn(webClient);
    }
    // Helper para crear una orden de prueba
    private Order createOrderWithUserAndItems(String userEmail, List<Item> items) {
        Order order = mock(Order.class);
        User user = mock(User.class);
        when(user.getEmail()).thenReturn(userEmail);
        when(order.getUser()).thenReturn(user);
        when(order.getItems()).thenReturn(items);
        return order;
    }

    private Item createItemWithProductId(Long productId) {
        Item item = mock(Item.class);
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);
        when(item.getProduct()).thenReturn(product);
        return item;
    }
    @Test
    void createOrder_whenUserAndProductsExist_returnsSuccessResponse() {
        // Arrange
        Long productId = 1L;
        String userEmail = "test@example.com";
        Order order = createOrderWithUserAndItems(userEmail, Arrays.asList(createItemWithProductId(productId)));
        String userUrl = "http://localhost:8080/user-service/users/find?email=" + userEmail;
        String productUrl = "http://localhost:8080/product-service/products";

        // Mock user exists
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(userUrl)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        User user = mock(User.class);
        AppResponse<User> userAppResponse = new AppResponse<>("ok", user);
        @SuppressWarnings("unchecked")
        Mono<AppResponse<User>> userMono = mock(Mono.class);
        @SuppressWarnings("unchecked")
        Mono<AppResponse<List<Product>>> bodyToMono = mock(Mono.class);
        // Primero devuelve el mono de usuario, luego el de productos
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(userMono)
            .thenReturn(bodyToMono);
        when(userMono.block()).thenReturn(userAppResponse);

        // Mock products exist
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(productUrl)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);
        AppResponse<List<Product>> appResponse = new AppResponse<>("ok", Arrays.asList(product));
        when(bodyToMono.block()).thenReturn(appResponse);

        when(orderService.save(order)).thenReturn(true);

        // Act
        ResponseEntity<AppResponse<Order>> response = orderController.createOrder(order);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order created successfully", response.getBody().getMessage());
        assertEquals(order, response.getBody().getData());
    }

    @Test
    void createOrder_whenUserDoesNotExist_returnsBadRequest() {
        String userEmail = "nouser@example.com";
        Order order = createOrderWithUserAndItems(userEmail, Arrays.asList(createItemWithProductId(1L)));
        String userUrl = "http://localhost:8080/user-service/users/find?email=" + userEmail;

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(userUrl)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        @SuppressWarnings("unchecked")
        Mono<AppResponse<User>> userMono = mock(Mono.class);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(userMono);
        when(userMono.block()).thenReturn(null); // Simula usuario no encontrado

        ResponseEntity<AppResponse<Order>> response = orderController.createOrder(order);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User does not exist", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void createOrder_whenProductDoesNotExist_returnsBadRequest() {
        String userEmail = "test@example.com";
        Long missingProductId = 99L;
        Order order = createOrderWithUserAndItems(userEmail, Arrays.asList(createItemWithProductId(missingProductId)));
        String userUrl = "http://localhost:8080/user-service/users/find?email=" + userEmail;
        String productUrl = "http://localhost:8080/product-service/products";

        // Mock user exists
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(userUrl)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        User user = mock(User.class);

        AppResponse<User> userAppResponse = new AppResponse<>("ok", user);
        @SuppressWarnings("unchecked")
        Mono<AppResponse<User>> userMono = mock(Mono.class);
        @SuppressWarnings("unchecked")
        Mono<AppResponse<List<Product>>> bodyToMono = mock(Mono.class);
        // Primero devuelve el mono de usuario, luego el de productos
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(userMono)
            .thenReturn(bodyToMono);
        when(userMono.block()).thenReturn(userAppResponse);

        // Mock products response (empty list)
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(productUrl)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        AppResponse<List<Product>> appResponse = new AppResponse<>("ok", Collections.emptyList());
        when(bodyToMono.block()).thenReturn(appResponse);

        ResponseEntity<AppResponse<Order>> response = orderController.createOrder(order);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Product does not exist", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void deleteOrder_whenOrderExists_returnsSuccessResponse() {
        Long orderId = 1L;
        when(orderService.deleteById(orderId)).thenReturn(true);

        ResponseEntity<AppResponse<Boolean>> response = orderController.deleteOrder(orderId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order deleted successfully", response.getBody().getMessage());
        assertTrue(response.getBody().getData());
    }

    @Test
    void deleteOrder_whenOrderDoesNotExist_returnsNotFoundResponse() {
        Long orderId = 2L;
        when(orderService.deleteById(orderId)).thenReturn(false);

        ResponseEntity<AppResponse<Boolean>> response = orderController.deleteOrder(orderId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order not found", response.getBody().getMessage());
        assertFalse(response.getBody().getData());
    }
}