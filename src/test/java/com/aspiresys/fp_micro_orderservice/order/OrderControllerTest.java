package com.aspiresys.fp_micro_orderservice.order;

import com.aspiresys.fp_micro_orderservice.common.dto.AppResponse;
import com.aspiresys.fp_micro_orderservice.order.Item.ItemService;
import com.aspiresys.fp_micro_orderservice.product.ProductService;
import com.aspiresys.fp_micro_orderservice.user.UserService;
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
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import reactor.core.publisher.Mono;




class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @Mock
    private ItemService itemService;

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

        // Mock user exists
        User user = mock(User.class);
        when(user.getEmail()).thenReturn(userEmail);
        AppResponse<User> userAppResponse = new AppResponse<>("ok", user);

        // Mock products exist
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);
        AppResponse<List<Product>> productAppResponse = new AppResponse<>("ok", Arrays.asList(product));

        // Mock the WebClient calls for first the user, then the products
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Use doReturn to handle the generic type issues
        doReturn(Mono.just(userAppResponse))
            .doReturn(Mono.just(productAppResponse))
            .when(responseSpec).bodyToMono(any(ParameterizedTypeReference.class));

        // Mock services
        when(productService.getProductById(productId)).thenReturn(product);
        when(orderService.save(any(Order.class))).thenReturn(true);

        // Act
        ResponseEntity<AppResponse<Order>> response = orderController.createOrder(order);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order created successfully", response.getBody().getMessage());
        verify(userService).saveUser(user);
        verify(orderService).save(any(Order.class));
    }

    @Test
    void getAllOrders_returnsAllOrders() {
        // Arrange
        List<Order> orders = Arrays.asList(
            mock(Order.class),
            mock(Order.class)
        );
        when(orderService.findAll()).thenReturn(orders);

        // Act
        ResponseEntity<AppResponse<List<Order>>> response = orderController.getAllOrders();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Orders retrieved:", response.getBody().getMessage());
        assertEquals(orders, response.getBody().getData());
        verify(orderService).findAll();
    }

    @Test
    void getOrderById_whenOrderExists_returnsOrder() {
        // Arrange
        Long orderId = 1L;
        Order order = mock(Order.class);
        when(orderService.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        ResponseEntity<AppResponse<Order>> response = orderController.getOrderById(orderId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order found", response.getBody().getMessage());
        assertEquals(order, response.getBody().getData());
        verify(orderService).findById(orderId);
    }

    @Test
    void getOrderById_whenOrderDoesNotExist_returnsNotFound() {
        // Arrange
        Long orderId = 1L;
        when(orderService.findById(orderId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<AppResponse<Order>> response = orderController.getOrderById(orderId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(orderService).findById(orderId);
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
        when(responseSpec.bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<AppResponse<User>>>any()))
            .thenReturn(userMono);
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

        // Mock user exists first
        User user = mock(User.class);
        when(user.getEmail()).thenReturn(userEmail);
        AppResponse<User> userAppResponse = new AppResponse<>("ok", user);

        // Mock products response (empty list - no matching products)
        AppResponse<List<Product>> productAppResponse = new AppResponse<>("ok", Collections.emptyList());

        // Mock WebClient behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // First call returns user response, second call returns empty product list
        doReturn(Mono.just(userAppResponse))
            .doReturn(Mono.just(productAppResponse))
            .when(responseSpec).bodyToMono(any(ParameterizedTypeReference.class));

        ResponseEntity<AppResponse<Order>> response = orderController.createOrder(order);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Product does not exist", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void createOrder_whenCommunicationErrorWithUserService_returnsBadRequest() {
        String userEmail = "test@example.com";
        Order order = createOrderWithUserAndItems(userEmail, Arrays.asList(createItemWithProductId(1L)));
        String userUrl = "http://localhost:8080/user-service/users/find?email=" + userEmail;

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(userUrl)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        @SuppressWarnings("unchecked")
        Mono<AppResponse<User>> userMono = mock(Mono.class);
        when(responseSpec.bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<AppResponse<User>>>any()))
            .thenReturn(userMono);
        when(userMono.block()).thenThrow(new RuntimeException("Connection error"));

        ResponseEntity<AppResponse<Order>> response = orderController.createOrder(order);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Communication error with user service", response.getBody().getMessage());
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
        verify(orderService).deleteById(orderId);
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
        verify(orderService).deleteById(orderId);
    }
}