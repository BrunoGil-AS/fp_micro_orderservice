package com.aspiresys.fp_micro_orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Propiedades de configuración para AOP
 * 
 * @author bruno.gil
 */
@Configuration
@ConfigurationProperties(prefix = "aop")
@Data
public class AopProperties {
    
    /**
     * Configuración para auditoría
     */
    private Audit audit = new Audit();
    
    /**
     * Configuración para métricas de performance
     */
    private Performance performance = new Performance();
    
    /**
     * Configuración para validación
     */
    private Validation validation = new Validation();
    
    @Data
    public static class Audit {
        /**
         * Si la auditoría está habilitada
         */
        private boolean enabled = true;
        
        /**
         * Si se deben loguear los parámetros por defecto
         */
        private boolean logParameters = true;
        
        /**
         * Si se deben loguear los resultados por defecto
         */
        private boolean logResults = false;
    }
    
    @Data
    public static class Performance {
        /**
         * Si las métricas de performance están habilitadas
         */
        private boolean enabled = true;
        
        /**
         * Umbral por defecto para advertencias (en milisegundos)
         */
        private long defaultWarningThreshold = 1000;
        
        /**
         * Si se debe usar logging detallado por defecto
         */
        private boolean detailedLogging = false;
    }
    
    @Data
    public static class Validation {
        /**
         * Si la validación está habilitada
         */
        private boolean enabled = true;
        
        /**
         * Si se debe fallar rápido en validaciones
         */
        private boolean failFast = true;
    }
}
