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
 * Specific aspect for order operations.
 * Provides specialized logging, security validations and business metrics.
 * 
 * Pointcuts are annotations to define where the aspect should apply.
 * This aspect includes:
 * - Logging before and after operations in the controller
 * - Logging and additional validations for order modification operations
 * - Logging after successful operations in the service
 * - Logging when errors occur in the service
 * - Additional specific validations for order operations
 * 
 * @author bruno.gil
 */
@Aspect
@Component
@Log
public class OrderOperationAspect {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Pointcut for all OrderController methods
     * 
     */
    @Pointcut("execution(* com.aspiresys.fp_micro_orderservice.order.OrderController.*(..))")
    public void orderControllerMethods() {}
    
    /**
     * Pointcut for all OrderService methods
     * 
     */
    @Pointcut("execution(* com.aspiresys.fp_micro_orderservice.order.OrderService.*(..))")
    public void orderServiceMethods() {}
    
    /**
     * Pointcut for methods that modify orders (create, update, delete)
     * 
     */
    @Pointcut("execution(* com.aspiresys.fp_micro_orderservice.order.OrderController.createOrder(..)) || " +
              "execution(* com.aspiresys.fp_micro_orderservice.order.OrderController.updateOrder(..)) || " +
              "execution(* com.aspiresys.fp_micro_orderservice.order.OrderController.deleteOrder(..))")
    public void orderModificationMethods() {}
    
    /**
     * Log before operations in the controller
     * 
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
     * Logging and additional validations for order modification operations
     * 
     */
    @Around("orderModificationMethods()")
    public Object logOrderModifications(ProceedingJoinPoint joinPoint) throws Throwable {
        String userEmail = getCurrentUserEmail();
        String methodName = joinPoint.getSignature().getName();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        long startTime = System.currentTimeMillis();
        
        // Log critical operation start
        log.info(String.format("\n[ORDER-MODIFICATION-START] %s\n|- User: %s\n|- Operation: %s\n|_ Critical operation initiated", 
                timestamp, userEmail, methodName));
        
        try {
            // Additional validations for orders
            validateOrderOperation(joinPoint);
            
            // Execute original method
            Object result = joinPoint.proceed();
            
            // Log success
            long executionTime = System.currentTimeMillis() - startTime;
            log.info(String.format("\n[ORDER-MODIFICATION-SUCCESS] %s\n|- User: %s\n|- Operation: %s\n|- Duration: %d ms\n|_ Operation completed successfully", 
                    LocalDateTime.now().format(TIMESTAMP_FORMAT), userEmail, methodName, executionTime));
            
            return result;
            
        } catch (Exception e) {
            // Log error
            long executionTime = System.currentTimeMillis() - startTime;
            log.severe(String.format("\n[ORDER-MODIFICATION-ERROR] %s\n|- User: %s\n|- Operation: %s\n|- Duration: %d ms\n|- Error: %s\n|_ Message: %s", 
                    LocalDateTime.now().format(TIMESTAMP_FORMAT), userEmail, methodName, executionTime, 
                    e.getClass().getSimpleName(), e.getMessage()));
            
            throw e;
        }
    }
    
    /**
     * Log after successful operations in the service
     * 
     */
    @AfterReturning(pointcut = "orderServiceMethods()", returning = "result")
    public void logAfterOrderService(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String resultInfo = getResultInfo(result);
        
        log.info(String.format("[ORDER-SERVICE] Method: %s completed - Result: %s", 
                methodName, resultInfo));
    }
    
    /**
     * Log when errors occur in the service
     * 
     */
    @AfterThrowing(pointcut = "orderServiceMethods()", throwing = "exception")
    public void logOrderServiceErrors(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().getName();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        
        log.warning(String.format("\n[ORDER-SERVICE-ERROR] %s\n|- Method: %s\n|- Exception: %s\n|_ Message: %s", 
                timestamp, methodName, exception.getClass().getSimpleName(), exception.getMessage()));
    }
    
    /**
     * Additional specific validations for order operations
     * 
     */
    private void validateOrderOperation(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        
        // Validate if there's an order in the parameters
        for (Object arg : args) {
            if (arg instanceof Order) {
                Order order = (Order) arg;
                
                // Specific validations for orders
                if (methodName.equals("createOrder") && order.getId() != null) {
                    log.warning("Attempting to create order with existing ID: " + order.getId());
                }
                
                if (methodName.equals("updateOrder") && order.getId() == null) {
                    throw new IllegalArgumentException("Cannot update order without ID");
                }
                
                // Validate that the order has items
                if (order.getItems() == null || order.getItems().isEmpty()) {
                    throw new IllegalArgumentException("Order must have at least one item");
                }
                
                log.fine(String.format("Order validation passed for %s - Order ID: %s, Items: %d", 
                        methodName, order.getId(), order.getItems() != null ? order.getItems().size() : 0));
            }
        }
    }
    
    /**
     * Gets safe information about the operation result
     */
    private String getResultInfo(Object result) {
        if (result == null) {
            return "null";
        }
        
        String className = result.getClass().getSimpleName();
        
        // For ResponseEntity, extract information from body
        if (className.equals("ResponseEntity")) {
            return "ResponseEntity[" + result.toString().length() + " chars]";
        }
        
        // For collections, show size
        if (result instanceof java.util.Collection) {
            return "Collection[" + ((java.util.Collection<?>) result).size() + " items]";
        }
        
        return className;
    }
    
    /**
     * Gets the current user's email
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
