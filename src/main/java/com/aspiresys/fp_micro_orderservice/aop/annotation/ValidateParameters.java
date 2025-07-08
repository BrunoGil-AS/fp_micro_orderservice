package com.aspiresys.fp_micro_orderservice.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para validar parámetros de entrada de métodos.
 * Permite realizar validaciones personalizadas antes de la ejecución del método.
 * 
 * @author bruno.gil
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateParameters {
    
    /**
     * Si se debe validar que los parámetros no sean null
     */
    boolean notNull() default true;
    
    /**
     * Si se debe validar colecciones que no estén vacías
     */
    boolean notEmpty() default false;
    
    /**
     * Mensaje personalizado para errores de validación
     */
    String message() default "Parameter validation failed";
}
