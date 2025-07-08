package com.aspiresys.fp_micro_orderservice.aop.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.aspiresys.fp_micro_orderservice.aop.annotation.Auditable;

import lombok.extern.java.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Aspecto para auditar operaciones críticas del sistema.
 * Registra automáticamente quién, qué, cuándo y el resultado de las operaciones.
 * 
 * @author bruno.gil
 */
@Aspect
@Component
@Log
public class AuditAspect {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Se ejecuta antes de métodos anotados con @Auditable
     */
    @Before("@annotation(auditable)")
    public void auditBefore(JoinPoint joinPoint, Auditable auditable) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String userEmail = getCurrentUserEmail();
        
        StringBuilder auditLog = new StringBuilder();
        auditLog.append("\n[AUDIT-START] ").append(timestamp);
        auditLog.append("\n├─ User: ").append(userEmail);
        auditLog.append("\n├─ Operation: ").append(auditable.operation().isEmpty() ? methodName : auditable.operation());
        auditLog.append("\n├─ Entity Type: ").append(auditable.entityType().isEmpty() ? "N/A" : auditable.entityType());
        auditLog.append("\n├─ Class: ").append(className);
        auditLog.append("\n├─ Method: ").append(methodName);
        
        if (auditable.logParameters() && joinPoint.getArgs().length > 0) {
            auditLog.append("\n├─ Parameters: ");
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) auditLog.append(", ");
                auditLog.append(getSafeParameterString(args[i]));
            }
        }
        
        log.info(auditLog.toString());
    }
    
    /**
     * Se ejecuta después de la ejecución exitosa de métodos anotados con @Auditable
     */
    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void auditAfterReturning(JoinPoint joinPoint, Auditable auditable, Object result) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String methodName = joinPoint.getSignature().getName();
        
        StringBuilder auditLog = new StringBuilder();
        auditLog.append("\n[AUDIT-SUCCESS] ").append(timestamp);
        auditLog.append("\n├─ Operation: ").append(auditable.operation().isEmpty() ? methodName : auditable.operation());
        auditLog.append("\n├─ Status: SUCCESS");
        
        if (auditable.logResult() && result != null) {
            auditLog.append("\n└─ Result: ").append(getSafeParameterString(result));
        } else {
            auditLog.append("\n└─ Result: [Not logged]");
        }
        
        log.info(auditLog.toString());
    }
    
    /**
     * Se ejecuta cuando ocurre una excepción en métodos anotados con @Auditable
     */
    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "exception")
    public void auditAfterThrowing(JoinPoint joinPoint, Auditable auditable, Throwable exception) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String methodName = joinPoint.getSignature().getName();
        String userEmail = getCurrentUserEmail();
        
        StringBuilder auditLog = new StringBuilder();
        auditLog.append("\n[AUDIT-ERROR] ").append(timestamp);
        auditLog.append("\n├─ User: ").append(userEmail);
        auditLog.append("\n├─ Operation: ").append(auditable.operation().isEmpty() ? methodName : auditable.operation());
        auditLog.append("\n├─ Status: ERROR");
        auditLog.append("\n├─ Exception: ").append(exception.getClass().getSimpleName());
        auditLog.append("\n└─ Message: ").append(exception.getMessage());
        
        log.warning(auditLog.toString());
    }
    
    /**
     * Obtiene el email del usuario actual desde el contexto de seguridad
     */
    private String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                return jwt.getClaimAsString("sub");
            }
            return "SYSTEM";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    /**
     * Convierte parámetros a string de forma segura, evitando información sensible
     */
    private String getSafeParameterString(Object parameter) {
        if (parameter == null) {
            return "null";
        }
        
        // No loguear objetos que puedan contener información sensible
        String className = parameter.getClass().getSimpleName();
        if (className.toLowerCase().contains("password") || 
            className.toLowerCase().contains("credential") ||
            className.toLowerCase().contains("secret")) {
            return "[SENSITIVE_DATA]";
        }
        
        // Para arrays
        if (parameter.getClass().isArray()) {
            return Arrays.toString((Object[]) parameter);
        }
        
        // Para strings largos, truncar
        String str = parameter.toString();
        if (str.length() > 200) {
            return str.substring(0, 200) + "... [TRUNCATED]";
        }
        
        return str;
    }
}
