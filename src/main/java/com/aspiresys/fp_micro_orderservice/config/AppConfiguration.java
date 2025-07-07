package com.aspiresys.fp_micro_orderservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuración general de la aplicación Order Service
 */
@Configuration
public class AppConfiguration {
    
    /**
     * Bean principal para WebClient.Builder para comunicación directa entre microservicios
     * Sin @LoadBalanced porque comunica directamente con localhost:puerto
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    /**
     * Bean adicional para WebClient.Builder con LoadBalancer 
     * para comunicación usando service discovery (si se necesita en el futuro)
     */
    @Bean("loadBalancedWebClientBuilder")
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
