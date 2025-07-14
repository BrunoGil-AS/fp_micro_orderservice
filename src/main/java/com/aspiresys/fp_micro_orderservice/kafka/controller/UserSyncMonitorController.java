package com.aspiresys.fp_micro_orderservice.kafka.controller;

import com.aspiresys.fp_micro_orderservice.user.UserSyncService;
import com.aspiresys.fp_micro_orderservice.user.UserRepository;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for monitoring user synchronization status.
 * Provides endpoints for administrators to check user sync status.
 * 
 * @author bruno.gil
 */
@RestController
@RequestMapping("/admin/kafka/user-sync")
@Log
public class UserSyncMonitorController {

    @Autowired
    private UserSyncService userSyncService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Gets the current user synchronization status.
     * 
     * @return Status information about user synchronization
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserSyncStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            long userCount = userSyncService.getSynchronizedUserCount();
            
            status.put("synchronizedUsers", userCount);
            status.put("status", userCount > 0 ? "SYNCHRONIZED" : "NO_USERS");
            status.put("message", userCount > 0 ? 
                "User synchronization is active with " + userCount + " users" :
                "No users synchronized yet");
            status.put("timestamp", java.time.LocalDateTime.now());
            
            log.info("📊 USER SYNC STATUS: " + userCount + " users synchronized");
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.warning("❌ Error getting user sync status: " + e.getMessage());
            
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("status", "ERROR");
            errorStatus.put("message", "Error retrieving sync status: " + e.getMessage());
            errorStatus.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(errorStatus);
        }
    }

    /**
     * Checks if a specific user is synchronized.
     * 
     * @param email User email to check
     * @return Synchronization status for the specific user
     */
    @GetMapping("/user/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkUserSync(@PathVariable String email) {
        try {
            boolean isSynchronized = userSyncService.isUserSynchronized(email);
            
            Map<String, Object> result = new HashMap<>();
            result.put("email", email);
            result.put("synchronized", isSynchronized);
            result.put("message", isSynchronized ? 
                "User is synchronized" : 
                "User not found in local database");
            result.put("timestamp", java.time.LocalDateTime.now());
            
            log.info("📧 USER SYNC CHECK: " + email + " - " + 
                    (isSynchronized ? "SYNCHRONIZED" : "NOT_FOUND"));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.warning("❌ Error checking user sync for " + email + ": " + e.getMessage());
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("email", email);
            errorResult.put("status", "ERROR");
            errorResult.put("message", "Error checking sync status: " + e.getMessage());
            errorResult.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(errorResult);
        }
    }

    /**
     * Gets detailed user synchronization metrics.
     * 
     * @return Detailed metrics about user synchronization
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserSyncMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            long totalUsers = userRepository.count();
            
            metrics.put("totalUsers", totalUsers);
            metrics.put("lastSyncCheck", java.time.LocalDateTime.now());
            metrics.put("kafkaConsumerStatus", "ACTIVE");
            metrics.put("message", "User synchronization metrics retrieved successfully");
            
            log.info("📈 USER SYNC METRICS: Total users: " + totalUsers);
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            log.warning("❌ Error getting user sync metrics: " + e.getMessage());
            
            Map<String, Object> errorMetrics = new HashMap<>();
            errorMetrics.put("status", "ERROR");
            errorMetrics.put("message", "Error retrieving metrics: " + e.getMessage());
            errorMetrics.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(errorMetrics);
        }
    }
}
