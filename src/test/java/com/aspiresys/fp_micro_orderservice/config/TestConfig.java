package com.aspiresys.fp_micro_orderservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;

/**
 * Configuración de test que incluye solo los componentes necesarios para AOP.
 * 
 * @author bruno.gil
 */
@TestConfiguration
@EnableAspectJAutoProxy
@ComponentScan(
    basePackages = {
        "com.aspiresys.fp_micro_orderservice.order",
        "com.aspiresys.fp_micro_orderservice.user",
        "com.aspiresys.fp_micro_orderservice.aop"
    },
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*kafka.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*security.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*config.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*FpMicroOrderserviceApplication.*")
    }
)
public class TestConfig {
    // Esta clase sirve para configurar el contexto de Spring para tests
    // incluyendo solo los componentes de AOP, orden y usuario
}
