package com.aspiresys.fp_micro_orderservice.aop.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.aspiresys.fp_micro_orderservice.aop.annotation.ExecutionTime;

import lombok.extern.java.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Aspecto para medir el tiempo de ejecución de métodos.
 * Proporciona métricas de rendimiento y alertas de performance.
 * 
 * @author bruno.gil
 */
@Aspect
@Component
@Log
public class ExecutionTimeAspect {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Envuelve la ejecución del método para medir el tiempo
     */
    @Around("@annotation(executionTime)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint, ExecutionTime executionTime) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String operationName = executionTime.operation().isEmpty() ? methodName : executionTime.operation();
        
        // Registrar inicio
        long startTime = System.currentTimeMillis();
        String startTimestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        
        if (executionTime.detailed()) {
            log.info(String.format("[PERFORMANCE-START] %s - Operation: %s (%s.%s)", 
                    startTimestamp, operationName, className, methodName));
        }
        
        Object result = null;
        boolean success = true;
        Throwable exception = null;
        
        try {
            // Ejecutar el método original
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            success = false;
            exception = throwable;
            throw throwable;
        } finally {
            // Calcular tiempo de ejecución
            long endTime = System.currentTimeMillis();
            long executionTimeMs = endTime - startTime;
            String endTimestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            
            // Crear log de performance
            StringBuilder perfLog = new StringBuilder();
            perfLog.append("\n[PERFORMANCE-REPORT] ").append(endTimestamp);
            perfLog.append("\n├─ Operation: ").append(operationName);
            perfLog.append("\n├─ Class: ").append(className);
            perfLog.append("\n├─ Method: ").append(methodName);
            perfLog.append("\n├─ Execution Time: ").append(executionTimeMs).append(" ms");
            perfLog.append("\n├─ Status: ").append(success ? "SUCCESS" : "ERROR");
            
            if (executionTime.detailed()) {
                perfLog.append("\n├─ Start Time: ").append(startTimestamp);
                perfLog.append("\n├─ End Time: ").append(endTimestamp);
                if (!success && exception != null) {
                    perfLog.append("\n├─ Exception: ").append(exception.getClass().getSimpleName());
                }
            }
            
            // Determinar el nivel de log basado en el tiempo de ejecución
            if (executionTimeMs > executionTime.warningThreshold()) {
                perfLog.append("\n└─ WARNING: Execution time exceeded threshold (")
                       .append(executionTime.warningThreshold()).append(" ms)");
                log.warning(perfLog.toString());
            } else {
                perfLog.append("\n└─ Performance: NORMAL");
                log.info(perfLog.toString());
            }
            
            // Log adicional para métricas (podría integrarse con sistemas de monitoreo)
            logMetrics(operationName, className, methodName, executionTimeMs, success);
        }
    }
    
    /**
     * Registra métricas en formato estructurado para integración con sistemas de monitoreo
     */
    private void logMetrics(String operation, String className, String methodName, 
                           long executionTime, boolean success) {
        // Este método podría enviar métricas a sistemas como Prometheus, Micrometer, etc.
        String metricsLog = String.format(
            "METRICS|operation=%s|class=%s|method=%s|execution_time_ms=%d|success=%s|timestamp=%s",
            operation, className, methodName, executionTime, success, 
            LocalDateTime.now().format(TIMESTAMP_FORMAT)
        );
        
        // Log separado para métricas (podría dirigirse a un appender específico)
        log.info("[METRICS] " + metricsLog);
    }
}
