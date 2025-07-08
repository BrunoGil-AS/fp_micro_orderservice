package com.aspiresys.fp_micro_orderservice.order;

import com.aspiresys.fp_micro_orderservice.common.dto.AppResponse;
import com.aspiresys.fp_micro_orderservice.order.Item.ItemService;
import com.aspiresys.fp_micro_orderservice.product.ProductService;
import com.aspiresys.fp_micro_orderservice.user.UserService;
import com.aspiresys.fp_micro_orderservice.order.dto.OrderDTO;
import com.aspiresys.fp_micro_orderservice.order.dto.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import com.aspiresys.fp_micro_orderservice.product.Product;
import com.aspiresys.fp_micro_orderservice.user.User;
import com.aspiresys.fp_micro_orderservice.order.Item.Item;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;




class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @Mock
    private ItemService itemService;

    @Mock
    private OrderValidationService orderValidationService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private OrderController orderController;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @SuppressWarnings("rawtypes")
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
        
        // Setup authentication mock
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsString("sub")).thenReturn("test@example.com");
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
    void createOrder_whenUserAndProductsExist_returnsSuccessResponse() throws Exception {
        // Arrange
        Long productId = 1L;
        String userEmail = "test@example.com";
        Order order = createOrderWithUserAndItems(userEmail, Arrays.asList(createItemWithProductId(productId)));

        // Mock user and validation result
        User user = mock(User.class);
        when(user.getEmail()).thenReturn(userEmail);
        
        OrderValidationService.OrderValidationResult validationResult = mock(OrderValidationService.OrderValidationResult.class);
        when(validationResult.isValid()).thenReturn(true);
        when(validationResult.getUser()).thenReturn(user);
        
        CompletableFuture<OrderValidationService.OrderValidationResult> validationFuture = CompletableFuture.completedFuture(validationResult);
        when(orderValidationService.validateOrderAsync(any(Order.class))).thenReturn(validationFuture);

        // Mock services
        when(orderService.save(any(Order.class))).thenReturn(true);
        
        // Mock mapper
        OrderDTO orderDTO = mock(OrderDTO.class);
        when(orderMapper.toDTO(any(Order.class))).thenReturn(orderDTO);

        // Act
        ResponseEntity<AppResponse<OrderDTO>> response = orderController.createOrder(order, authentication);

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
        List<OrderDTO> orderDTOs = Arrays.asList(
            mock(OrderDTO.class),
            mock(OrderDTO.class)
        );
        when(orderService.findAll()).thenReturn(orders);
        when(orderMapper.toDTOList(orders)).thenReturn(orderDTOs);

        // Act
        ResponseEntity<AppResponse<List<OrderDTO>>> response = orderController.getAllOrders();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Orders retrieved:", response.getBody().getMessage());
        assertEquals(orderDTOs, response.getBody().getData());
        verify(orderService).findAll();
        verify(orderMapper).toDTOList(orders);
    }

    @Test
    void getOrderById_whenOrderExists_returnsOrder() {
        // Arrange
        Long orderId = 1L;
        Order order = mock(Order.class);
        OrderDTO orderDTO = mock(OrderDTO.class);
        when(orderService.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toDTO(order)).thenReturn(orderDTO);

        // Act
        ResponseEntity<AppResponse<OrderDTO>> response = orderController.getOrderById(orderId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order found", response.getBody().getMessage());
        assertEquals(orderDTO, response.getBody().getData());
        verify(orderService).findById(orderId);
        verify(orderMapper).toDTO(order);
    }

    @Test
    void getOrderById_whenOrderDoesNotExist_returnsNotFound() {
        // Arrange
        Long orderId = 1L;
        when(orderService.findById(orderId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<AppResponse<OrderDTO>> response = orderController.getOrderById(orderId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order not found", response.getBody().getMessage());
        assertNull(response.getBody().getData());
        verify(orderService).findById(orderId);
        verify(orderMapper, never()).toDTO(any(Order.class));
    }

    @Test
    void createOrder_whenValidationFails_returnsBadRequest() throws Exception {
        // Arrange
        String userEmail = "test@example.com";
        Order order = createOrderWithUserAndItems(userEmail, Arrays.asList(createItemWithProductId(1L)));
        
        OrderValidationService.OrderValidationResult validationResult = mock(OrderValidationService.OrderValidationResult.class);
        when(validationResult.isValid()).thenReturn(false);
        when(validationResult.getErrorMessage()).thenReturn("User does not exist");
        
        CompletableFuture<OrderValidationService.OrderValidationResult> validationFuture = CompletableFuture.completedFuture(validationResult);
        when(orderValidationService.validateOrderAsync(any(Order.class))).thenReturn(validationFuture);

        // Act
        ResponseEntity<AppResponse<OrderDTO>> response = orderController.createOrder(order, authentication);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User does not exist", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void createOrder_whenUserEmailMismatch_returnsBadRequest() throws Exception {
        // Arrange
        String orderUserEmail = "other@example.com";
        String authenticatedUserEmail = "test@example.com";
        Order order = createOrderWithUserAndItems(orderUserEmail, Arrays.asList(createItemWithProductId(1L)));
        
        // Mock different email in JWT
        when(jwt.getClaimAsString("sub")).thenReturn(authenticatedUserEmail);
        
        User user = mock(User.class);
        when(user.getEmail()).thenReturn(orderUserEmail);
        
        OrderValidationService.OrderValidationResult validationResult = mock(OrderValidationService.OrderValidationResult.class);
        when(validationResult.isValid()).thenReturn(true);
        when(validationResult.getUser()).thenReturn(user);
        
        CompletableFuture<OrderValidationService.OrderValidationResult> validationFuture = CompletableFuture.completedFuture(validationResult);
        when(orderValidationService.validateOrderAsync(any(Order.class))).thenReturn(validationFuture);

        // Act
        ResponseEntity<AppResponse<OrderDTO>> response = orderController.createOrder(order, authentication);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("You are not allowed to create orders for others", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void createOrder_whenOrderSaveFails_returnsInternalServerError() throws Exception {
        // Arrange
        String userEmail = "test@example.com";
        Order order = createOrderWithUserAndItems(userEmail, Arrays.asList(createItemWithProductId(1L)));

        User user = mock(User.class);
        when(user.getEmail()).thenReturn(userEmail);
        
        OrderValidationService.OrderValidationResult validationResult = mock(OrderValidationService.OrderValidationResult.class);
        when(validationResult.isValid()).thenReturn(true);
        when(validationResult.getUser()).thenReturn(user);
        
        CompletableFuture<OrderValidationService.OrderValidationResult> validationFuture = CompletableFuture.completedFuture(validationResult);
        when(orderValidationService.validateOrderAsync(any(Order.class))).thenReturn(validationFuture);

        // Mock save fails
        when(orderService.save(any(Order.class))).thenReturn(false);

        // Act
        ResponseEntity<AppResponse<OrderDTO>> response = orderController.createOrder(order, authentication);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Failed to create order", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void deleteOrder_whenOrderExistsAndBelongsToUser_returnsSuccessResponse() {
        // Arrange
        Long orderId = 1L;
        String userEmail = "test@example.com";
        
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn(userEmail);
        when(userService.getUserByEmail(userEmail)).thenReturn(user);
        
        Order order = mock(Order.class);
        when(order.getUser()).thenReturn(user);
        when(orderService.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        ResponseEntity<AppResponse<Boolean>> response = orderController.deleteOrder(orderId, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order deleted successfully", response.getBody().getMessage());
        assertTrue(response.getBody().getData());
        verify(orderService).deleteById(orderId);
    }

    @Test
    void deleteOrder_whenOrderDoesNotExist_returnsNotFoundResponse() {
        // Arrange
        Long orderId = 2L;
        String userEmail = "test@example.com";
        
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn(userEmail);
        when(userService.getUserByEmail(userEmail)).thenReturn(user);
        
        when(orderService.findById(orderId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<AppResponse<Boolean>> response = orderController.deleteOrder(orderId, authentication);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order not found", response.getBody().getMessage());
        assertFalse(response.getBody().getData());
        verify(orderService, never()).deleteById(orderId);
    }

    @Test
    void deleteOrder_whenUserDoesNotExist_returnsNotFoundResponse() {
        // Arrange
        Long orderId = 1L;
        String userEmail = "test@example.com";
        
        when(userService.getUserByEmail(userEmail)).thenReturn(null);

        // Act
        ResponseEntity<AppResponse<Boolean>> response = orderController.deleteOrder(orderId, authentication);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().getMessage());
        assertFalse(response.getBody().getData());
        verify(orderService, never()).deleteById(orderId);
    }

    @Test
    void deleteOrder_whenOrderDoesNotBelongToUser_returnsNotFoundResponse() {
        // Arrange
        Long orderId = 1L;
        String userEmail = "test@example.com";
        
        User requestingUser = mock(User.class);
        when(requestingUser.getId()).thenReturn(1L);
        when(requestingUser.getEmail()).thenReturn(userEmail);
        when(userService.getUserByEmail(userEmail)).thenReturn(requestingUser);
        
        User orderOwner = mock(User.class);
        when(orderOwner.getId()).thenReturn(2L); // Different user ID
        
        Order order = mock(Order.class);
        when(order.getUser()).thenReturn(orderOwner);
        when(orderService.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        ResponseEntity<AppResponse<Boolean>> response = orderController.deleteOrder(orderId, authentication);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Order not found", response.getBody().getMessage());
        assertFalse(response.getBody().getData());
        verify(orderService, never()).deleteById(orderId);
    }
}