package com.aspiresys.fp_micro_orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing
 * 
 * @author bruno.gil
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "orderValidationExecutor")
    public Executor orderValidationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("OrderValidation-");
        executor.initialize();
        return executor;
    }
}
