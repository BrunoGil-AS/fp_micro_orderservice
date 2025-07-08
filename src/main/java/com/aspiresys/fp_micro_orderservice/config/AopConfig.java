package com.aspiresys.fp_micro_orderservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import lombok.extern.java.Log;

/**
 * Configuración para habilitar AOP (Aspect-Oriented Programming)
 * 
 * @EnableAspectJAutoProxy habilita el soporte para AspectJ en Spring
 * proxyTargetClass = true fuerza el uso de proxies CGLIB en lugar de JDK proxies
 * 
 * @author bruno.gil
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties(AopProperties.class)
@Log
public class AopConfig {
    
    private final AopProperties aopProperties;
    
    public AopConfig(AopProperties aopProperties) {
        this.aopProperties = aopProperties;
        logAopConfiguration();
    }
    
    /**
     * Registra la configuración de AOP al inicializar
     */
    private void logAopConfiguration() {
        log.info("=== AOP Configuration ===");
        log.info("Audit enabled: " + aopProperties.getAudit().isEnabled());
        log.info("Performance monitoring enabled: " + aopProperties.getPerformance().isEnabled());
        log.info("Validation enabled: " + aopProperties.getValidation().isEnabled());
        log.info("Default warning threshold: " + aopProperties.getPerformance().getDefaultWarningThreshold() + "ms");
        log.info("========================");
    }
}
