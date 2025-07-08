package com.aspiresys.fp_micro_orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Configuration properties for AOP
 * 
 * @author bruno.gil
 */
@Configuration
@ConfigurationProperties(prefix = "aop")
@Data
public class AopProperties {
    
    /**
     * Configuration for auditing
     */
    private Audit audit = new Audit();
    
    /**
     * Configuration for performance metrics
     */
    private Performance performance = new Performance();
    
    /**
     * Configuration for validation
     */
    private Validation validation = new Validation();
    
    @Data
    public static class Audit {
        /**
         * Whether auditing is enabled
         */
        private boolean enabled = true;
        
        /**
         * Whether parameters should be logged by default
         */
        private boolean logParameters = true;
        
        /**
         * Whether results should be logged by default
         */
        private boolean logResults = false;
    }
    
    @Data
    public static class Performance {
        /**
         * Whether performance metrics are enabled
         */
        private boolean enabled = true;
        
        /**
         * Default threshold for warnings (in milliseconds)
         */
        private long defaultWarningThreshold = 1000;
        
        /**
         * Whether detailed logging should be used by default
         */
        private boolean detailedLogging = false;
    }
    
    @Data
    public static class Validation {
        /**
         * Whether validation is enabled
         */
        private boolean enabled = true;
        
        /**
         * Whether to fail fast on validations
         */
        private boolean failFast = true;
    }
}
