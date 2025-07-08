package com.aspiresys.fp_micro_orderservice.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para medir el tiempo de ejecución de métodos.
 * Útil para monitoreo de performance y identificación de cuellos de botella.
 * 
 * @author bruno.gil
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecutionTime {
    
    /**
     * Nombre descriptivo de la operación para los logs
     */
    String operation() default "";
    
    /**
     * Umbral en milisegundos para alertas de performance.
     * Si el método tarda más que este umbral, se registra una advertencia.
     */
    long warningThreshold() default 1000;
    
    /**
     * Si se debe incluir información detallada en el log
     */
    boolean detailed() default false;
}
