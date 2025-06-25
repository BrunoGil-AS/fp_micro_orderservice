package com.aspiresys.fp_micro_orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfiguration {
    // Bean para WebClient.Builder (puedes moverlo a una clase @Configuration si prefieres)
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

}
