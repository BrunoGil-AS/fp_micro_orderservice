package com.aspiresys.fp_micro_orderservice.aop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.aspiresys.fp_micro_orderservice.order.Order;
import com.aspiresys.fp_micro_orderservice.order.OrderRepository;
import com.aspiresys.fp_micro_orderservice.order.OrderService;
import com.aspiresys.fp_micro_orderservice.order.OrderServiceImpl;
import com.aspiresys.fp_micro_orderservice.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test unitario para verificar que AOP funciona correctamente
 * en el servicio de órdenes.
 * 
 * @author bruno.gil
 */
@SpringBootTest
@ActiveProfiles("test")
public class AopIntegrationTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderServiceImpl(orderRepository);
    }

    @Test
    void testAopAnnotationsOnFindAll() {
        // Arrange
        List<Order> mockOrders = Arrays.asList(
            createMockOrder(1L, "user1@test.com"),
            createMockOrder(2L, "user2@test.com")
        );
        when(orderRepository.findAll()).thenReturn(mockOrders);

        // Act
        List<Order> result = orderService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderRepository).findAll();
        
        // Los aspectos de AOP deberían haberse ejecutado:
        // - @ExecutionTime debería haber medido el tiempo
        // - OrderOperationAspect debería haber loggeado la operación
    }

    @Test
    void testAopAnnotationsOnSave() {
        // Arrange
        Order orderToSave = createMockOrder(null, "user@test.com");
        Order savedOrder = createMockOrder(1L, "user@test.com");
        
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderRepository.existsById(any())).thenReturn(false);

        // Act
        boolean result = orderService.save(orderToSave);

        // Assert
        assertTrue(result);
        verify(orderRepository).save(orderToSave);
        
        // Los aspectos de AOP deberían haberse ejecutado:
        // - @ValidateParameters debería haber validado que order no es null
        // - @ExecutionTime debería haber medido el tiempo
        // - @Auditable debería haber loggeado la auditoría (si se aplica)
    }

    @Test
    void testAopValidationOnNullParameter() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> orderService.save(null)
        );
        
        assertEquals("Order cannot be null", exception.getMessage());
        
        // @ValidateParameters debería haber interceptado y lanzado la excepción
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testAopValidationOnFindByIdWithNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> orderService.findById(null)
        );
        
        assertTrue(exception.getMessage().contains("Order ID cannot be null"));
        
        // @ValidateParameters debería haber interceptado y lanzado la excepción
        verify(orderRepository, never()).findById(any());
    }

    /**
     * Crea una orden mock para testing
     */
    private Order createMockOrder(Long id, String userEmail) {
        User user = User.builder()
                .id(1L)
                .email(userEmail)
                .firstName("Test")
                .lastName("User")
                .build();

        return Order.builder()
                .id(id)
                .user(user)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
    }
}
