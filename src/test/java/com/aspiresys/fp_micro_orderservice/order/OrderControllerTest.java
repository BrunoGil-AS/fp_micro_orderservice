package com.aspiresys.fp_micro_orderservice.order;

import com.aspiresys.fp_micro_orderservice.common.dto.AppResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;



class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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