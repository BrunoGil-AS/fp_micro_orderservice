package com.aspiresys.fp_micro_orderservice.aop.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import com.aspiresys.fp_micro_orderservice.aop.annotation.ValidateParameters;

import lombok.extern.java.Log;

import java.util.Collection;

/**
 * Aspect for validating method input parameters.
 * Provides automatic validations before method execution.
 * 
 * @author bruno.gil
 */
@Aspect
@Component
@Log
public class ValidationAspect {
    
    /**
     * Validates parameters before method execution
     */
    @Before("@annotation(validateParameters)")
    public void validateMethodParameters(JoinPoint joinPoint, ValidateParameters validateParameters) {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Validate null parameters
        if (validateParameters.notNull()) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    String errorMessage = String.format(
                        "Parameter validation failed in %s.%s(): Parameter at index %d is null. %s",
                        className, methodName, i, validateParameters.message()
                    );
                    log.severe(errorMessage);
                    throw new IllegalArgumentException(errorMessage);
                }
            }
        }
        
        // Validate empty collections
        if (validateParameters.notEmpty()) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg != null) {
                    if (arg instanceof Collection && ((Collection<?>) arg).isEmpty()) {
                        String errorMessage = String.format(
                            "Parameter validation failed in %s.%s(): Collection parameter at index %d is empty. %s",
                            className, methodName, i, validateParameters.message()
                        );
                        log.severe(errorMessage);
                        throw new IllegalArgumentException(errorMessage);
                    }
                    if (arg instanceof String && ((String) arg).trim().isEmpty()) {
                        String errorMessage = String.format(
                            "Parameter validation failed in %s.%s(): String parameter at index %d is empty. %s",
                            className, methodName, i, validateParameters.message()
                        );
                        log.severe(errorMessage);
                        throw new IllegalArgumentException(errorMessage);
                    }
                    if (arg.getClass().isArray() && java.lang.reflect.Array.getLength(arg) == 0) {
                        String errorMessage = String.format(
                            "Parameter validation failed in %s.%s(): Array parameter at index %d is empty. %s",
                            className, methodName, i, validateParameters.message()
                        );
                        log.severe(errorMessage);
                        throw new IllegalArgumentException(errorMessage);
                    }
                }
            }
        }
        
        // Successful validation log
        log.fine(String.format("Parameter validation passed for %s.%s() with %d parameters", 
                className, methodName, args.length));
    }
}
