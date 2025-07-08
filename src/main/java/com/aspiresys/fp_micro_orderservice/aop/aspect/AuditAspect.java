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
 * Aspect to audit critical system operations.
 * Automatically logs who, what, when, and the result of operations.
 * 
 * @author bruno.gil
 */
@Aspect
@Component
@Log
public class AuditAspect {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * It executes before methods annotated with @Auditable.
     * Logs the operation details including user, operation name, entity type, class, method,
     */
    @Before("@annotation(auditable)")
    public void auditBefore(JoinPoint joinPoint, Auditable auditable) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String userEmail = getCurrentUserEmail();
        
        StringBuilder auditLog = new StringBuilder();
        auditLog.append("\n[AUDIT-START] ").append(timestamp);
        auditLog.append("\n|- User: ").append(userEmail);
        auditLog.append("\n|- Operation: ").append(auditable.operation().isEmpty() ? methodName : auditable.operation());
        auditLog.append("\n|- Entity Type: ").append(auditable.entityType().isEmpty() ? "N/A" : auditable.entityType());
        auditLog.append("\n|- Class: ").append(className);
        auditLog.append("\n|- Method: ").append(methodName);
        
        if (auditable.logParameters() && joinPoint.getArgs().length > 0) {
            auditLog.append("\n|- Parameters: ");
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) auditLog.append(", ");
                auditLog.append(getSafeParameterString(args[i]));
            }
        }
        
        log.info(auditLog.toString());
    }
    
    /**
     * It executes after the successful execution of methods annotated with @Auditable.
     * Logs the operation details including operation name, status, and result.
     */
    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void auditAfterReturning(JoinPoint joinPoint, Auditable auditable, Object result) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String methodName = joinPoint.getSignature().getName();
        
        StringBuilder auditLog = new StringBuilder();
        auditLog.append("\n[AUDIT-SUCCESS] ").append(timestamp);
        auditLog.append("\n|- Operation: ").append(auditable.operation().isEmpty() ? methodName : auditable.operation());
        auditLog.append("\n|- Status: SUCCESS");
        
        if (auditable.logResult() && result != null) {
            auditLog.append("\n|_ Result: ").append(getSafeParameterString(result));
        } else {
            auditLog.append("\n|_ Result: [Not logged]");
        }
        
        log.info(auditLog.toString());
    }
    
    /**
     * It executes when an exception occurs in methods annotated with @Auditable.
     * Logs the operation details including operation name, status, and exception details.
     * 
     */
    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "exception")
    public void auditAfterThrowing(JoinPoint joinPoint, Auditable auditable, Throwable exception) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String methodName = joinPoint.getSignature().getName();
        String userEmail = getCurrentUserEmail();
        
        StringBuilder auditLog = new StringBuilder();
        auditLog.append("\n[AUDIT-ERROR] ").append(timestamp);
        auditLog.append("\n|- User: ").append(userEmail);
        auditLog.append("\n|- Operation: ").append(auditable.operation().isEmpty() ? methodName : auditable.operation());
        auditLog.append("\n|- Status: ERROR");
        auditLog.append("\n|- Exception: ").append(exception.getClass().getSimpleName());
        auditLog.append("\n|_ Message: ").append(exception.getMessage());
        
        log.warning(auditLog.toString());
    }
    
    /**
     * It obtains the email of the current user from the security context.
     * If the user is not authenticated, returns "SYSTEM".
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
     * Safely formats the parameter for logging.
     * Avoids logging sensitive information such as passwords or credentials.
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
