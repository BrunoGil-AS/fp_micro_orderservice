package com.aspiresys.fp_micro_orderservice.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to validate method input parameters.
 * Allows performing custom validations before method execution.
 * 
 * @author bruno.gil
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateParameters {
    
    /**
     * If the parameters should be validated to not be null.
     * If true, the aspect will throw an exception if any parameter is null.
     */
    boolean notNull() default true;
    
    /**
     * If the parameters should be validated to not be empty.
     * If true, the aspect will throw an exception if any collection parameter is empty.
     */
    boolean notEmpty() default false;
    
    /**
     * Custom message to be used in validation exceptions.
     * This message will be included in the exception thrown when validation fails.
     */
    String message() default "Parameter validation failed";
}
