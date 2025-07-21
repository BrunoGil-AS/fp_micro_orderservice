package com.aspiresys.fp_micro_orderservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for setting up WebClient beans.
 * Provides a primary WebClient for making HTTP requests without service discovery,
 * and an additional WebClient with LoadBalancer for service discovery.
 */
@Configuration
public class AppConfiguration {
    
    /**
     * Web client builder for making HTTP requests.
     * This is the primary WebClient used for communication with other services.
     *
     * When this bean is used, it will not use service discovery.
     * 
     * @return WebClient.Builder instance for creating WebClient instances
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    /**
     * Additional bean for WebClient.Builder with LoadBalancer
     * for communication using service discovery (if needed in the future).
     * 
     * This bean is annotated with @LoadBalanced to enable client-side load balancing.
     * 
     * @return WebClient.Builder instance with LoadBalancer enabled
     */
    @Bean("loadBalancedWebClientBuilder")
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
