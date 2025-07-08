package com.aspiresys.fp_micro_orderservice.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para auditar operaciones críticas del sistema.
 * Registra automáticamente la información de la operación realizada.
 * 
 * @author bruno.gil
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    
    /**
     * Descripción de la operación que se está auditando
     */
    String operation() default "";
    
    /**
     * Tipo de entidad sobre la que se realiza la operación
     */
    String entityType() default "";
    
    /**
     * Si se debe registrar los parámetros de entrada
     */
    boolean logParameters() default true;
    
    /**
     * Si se debe registrar el resultado de la operación
     */
    boolean logResult() default false;
}
