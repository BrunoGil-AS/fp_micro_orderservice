package com.aspiresys.fp_micro_orderservice.aop.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.aspiresys.fp_micro_orderservice.order.Order;

import lombok.extern.java.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Aspecto específico para operaciones de órdenes.
 * Proporciona logging especializado, validaciones de seguridad y métricas de negocio.
 * 
 * @author bruno.gil
 */
@Aspect
@Component
@Log
public class OrderOperationAspect {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Pointcut para todos los métodos del OrderController
     */
    @Pointcut("execution(* com.aspiresys.fp_micro_orderservice.order.OrderController.*(..))")
    public void orderControllerMethods() {}
    
    /**
     * Pointcut para todos los métodos del OrderService
     */
    @Pointcut("execution(* com.aspiresys.fp_micro_orderservice.order.OrderService.*(..))")
    public void orderServiceMethods() {}
    
    /**
     * Pointcut para métodos que modifican órdenes (create, update, delete)
     */
    @Pointcut("execution(* com.aspiresys.fp_micro_orderservice.order.OrderController.createOrder(..)) || " +
              "execution(* com.aspiresys.fp_micro_orderservice.order.OrderController.updateOrder(..)) || " +
              "execution(* com.aspiresys.fp_micro_orderservice.order.OrderController.deleteOrder(..))")
    public void orderModificationMethods() {}
    
    /**
     * Log antes de operaciones en el controller
     */
    @Before("orderControllerMethods()")
    public void logBeforeOrderController(JoinPoint joinPoint) {
        String userEmail = getCurrentUserEmail();
        String methodName = joinPoint.getSignature().getName();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        
        log.info(String.format("[ORDER-CONTROLLER] %s - User: %s - Method: %s - Args: %d", 
                timestamp, userEmail, methodName, joinPoint.getArgs().length));
    }
    
    /**
     * Log y validaciones adicionales para operaciones que modifican órdenes
     */
    @Around("orderModificationMethods()")
    public Object logOrderModifications(ProceedingJoinPoint joinPoint) throws Throwable {
        String userEmail = getCurrentUserEmail();
        String methodName = joinPoint.getSignature().getName();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        long startTime = System.currentTimeMillis();
        
        // Log inicio de operación crítica
        log.info(String.format("\n[ORDER-MODIFICATION-START] %s\n├─ User: %s\n├─ Operation: %s\n└─ Critical operation initiated", 
                timestamp, userEmail, methodName));
        
        try {
            // Validaciones adicionales para órdenes
            validateOrderOperation(joinPoint);
            
            // Ejecutar método original
            Object result = joinPoint.proceed();
            
            // Log éxito
            long executionTime = System.currentTimeMillis() - startTime;
            log.info(String.format("\n[ORDER-MODIFICATION-SUCCESS] %s\n├─ User: %s\n├─ Operation: %s\n├─ Duration: %d ms\n└─ Operation completed successfully", 
                    LocalDateTime.now().format(TIMESTAMP_FORMAT), userEmail, methodName, executionTime));
            
            return result;
            
        } catch (Exception e) {
            // Log error
            long executionTime = System.currentTimeMillis() - startTime;
            log.severe(String.format("\n[ORDER-MODIFICATION-ERROR] %s\n├─ User: %s\n├─ Operation: %s\n├─ Duration: %d ms\n├─ Error: %s\n└─ Message: %s", 
                    LocalDateTime.now().format(TIMESTAMP_FORMAT), userEmail, methodName, executionTime, 
                    e.getClass().getSimpleName(), e.getMessage()));
            
            throw e;
        }
    }
    
    /**
     * Log después de operaciones exitosas en el service
     */
    @AfterReturning(pointcut = "orderServiceMethods()", returning = "result")
    public void logAfterOrderService(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String resultInfo = getResultInfo(result);
        
        log.info(String.format("[ORDER-SERVICE] Method: %s completed - Result: %s", 
                methodName, resultInfo));
    }
    
    /**
     * Log cuando ocurren errores en el service
     */
    @AfterThrowing(pointcut = "orderServiceMethods()", throwing = "exception")
    public void logOrderServiceErrors(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().getName();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        
        log.warning(String.format("\n[ORDER-SERVICE-ERROR] %s\n├─ Method: %s\n├─ Exception: %s\n└─ Message: %s", 
                timestamp, methodName, exception.getClass().getSimpleName(), exception.getMessage()));
    }
    
    /**
     * Validaciones adicionales específicas para operaciones de órdenes
     */
    private void validateOrderOperation(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        
        // Validar si hay una orden en los parámetros
        for (Object arg : args) {
            if (arg instanceof Order) {
                Order order = (Order) arg;
                
                // Validaciones específicas para órdenes
                if (methodName.equals("createOrder") && order.getId() != null) {
                    log.warning("Attempting to create order with existing ID: " + order.getId());
                }
                
                if (methodName.equals("updateOrder") && order.getId() == null) {
                    throw new IllegalArgumentException("Cannot update order without ID");
                }
                
                // Validar que la orden tenga items
                if (order.getItems() == null || order.getItems().isEmpty()) {
                    throw new IllegalArgumentException("Order must have at least one item");
                }
                
                log.fine(String.format("Order validation passed for %s - Order ID: %s, Items: %d", 
                        methodName, order.getId(), order.getItems() != null ? order.getItems().size() : 0));
            }
        }
    }
    
    /**
     * Obtiene información segura sobre el resultado de la operación
     */
    private String getResultInfo(Object result) {
        if (result == null) {
            return "null";
        }
        
        String className = result.getClass().getSimpleName();
        
        // Para ResponseEntity, extraer información del body
        if (className.equals("ResponseEntity")) {
            return "ResponseEntity[" + result.toString().length() + " chars]";
        }
        
        // Para colecciones, mostrar tamaño
        if (result instanceof java.util.Collection) {
            return "Collection[" + ((java.util.Collection<?>) result).size() + " items]";
        }
        
        return className;
    }
    
    /**
     * Obtiene el email del usuario actual
     */
    private String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                return jwt.getClaimAsString("sub");
            }
            return "SYSTEM";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
