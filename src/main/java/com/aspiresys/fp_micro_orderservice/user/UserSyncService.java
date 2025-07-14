package com.aspiresys.fp_micro_orderservice.user;

import com.aspiresys.fp_micro_orderservice.kafka.dto.UserMessage;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for synchronizing user data from Kafka messages.
 * Handles user creation, updates, and deletion based on Kafka events from User Service.
 * 
 * @author bruno.gil
 */
@Service
@Log
public class UserSyncService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Processes user message from Kafka and synchronizes local user data.
     * 
     * @param userMessage User message from Kafka
     */
    @Transactional
    public void processUserMessage(UserMessage userMessage) {
        try {
            switch (userMessage.getEventType()) {
                case "USER_CREATED":
                case "INITIAL_LOAD":
                    handleUserCreated(userMessage);
                    break;
                case "USER_UPDATED":
                    handleUserUpdated(userMessage);
                    break;
                case "USER_DELETED":
                    handleUserDeleted(userMessage);
                    break;
                default:
                    log.warning("⚠️ Unknown event type: " + userMessage.getEventType());
            }
        } catch (Exception e) {
            log.warning("❌ Error processing user message: " + e.getMessage());
            throw e; // Re-throw to trigger Kafka retry mechanism
        }
    }

    /**
     * Handles user creation and initial load events.
     * 
     * @param userMessage User message containing user data
     */
    private void handleUserCreated(UserMessage userMessage) {
        try {
            // Check if user already exists (avoid duplicates during initial load)
            User existingUser = userRepository.findByEmail(userMessage.getEmail()).orElse(null);
            
            if (existingUser != null) {
                log.info("ℹ️ User already exists during " + userMessage.getEventType() + ", updating: " + userMessage.getEmail());
                updateUserFromMessage(existingUser, userMessage);
                userRepository.save(existingUser);
            } else {
                User newUser = createUserFromMessage(userMessage);
                userRepository.save(newUser);
                log.info("✅ User created successfully: " + userMessage.getEmail());
            }
        } catch (Exception e) {
            log.warning("❌ Failed to create/update user " + userMessage.getEmail() + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Handles user update events.
     * 
     * @param userMessage User message containing updated user data
     */
    private void handleUserUpdated(UserMessage userMessage) {
        try {
            User existingUser = userRepository.findById(userMessage.getId()).orElse(null);
            
            if (existingUser != null) {
                updateUserFromMessage(existingUser, userMessage);
                userRepository.save(existingUser);
                log.info("✅ User updated successfully: " + userMessage.getEmail());
            } else {
                // User doesn't exist locally, create it
                log.info("ℹ️ User not found locally during update, creating: " + userMessage.getEmail());
                User newUser = createUserFromMessage(userMessage);
                userRepository.save(newUser);
            }
        } catch (Exception e) {
            log.warning("❌ Failed to update user " + userMessage.getEmail() + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Handles user deletion events.
     * 
     * @param userMessage User message containing user data to delete
     */
    private void handleUserDeleted(UserMessage userMessage) {
        try {
            User existingUser = userRepository.findById(userMessage.getId()).orElse(null);
            
            if (existingUser != null) {
                // Check if user has orders before deleting
                if (existingUser.getOrders() != null && !existingUser.getOrders().isEmpty()) {
                    log.warning("⚠️ Cannot delete user " + userMessage.getEmail() + " - has existing orders");
                    // Optionally, you could mark the user as inactive instead of deleting
                    return;
                }
                
                userRepository.delete(existingUser);
                log.info("✅ User deleted successfully: " + userMessage.getEmail());
            } else {
                log.info("ℹ️ User not found locally during deletion: " + userMessage.getEmail());
            }
        } catch (Exception e) {
            log.warning("❌ Failed to delete user " + userMessage.getEmail() + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a new User entity from UserMessage.
     * 
     * @param userMessage User message
     * @return New User entity
     */
    private User createUserFromMessage(UserMessage userMessage) {
        return User.builder()
                .id(userMessage.getId())
                .firstName(userMessage.getFirstName())
                .lastName(userMessage.getLastName())
                .email(userMessage.getEmail())
                .address(userMessage.getAddress())
                .build();
    }

    /**
     * Updates existing User entity with data from UserMessage.
     * 
     * @param user Existing user entity
     * @param userMessage User message with updated data
     */
    private void updateUserFromMessage(User user, UserMessage userMessage) {
        user.setFirstName(userMessage.getFirstName());
        user.setLastName(userMessage.getLastName());
        user.setEmail(userMessage.getEmail());
        user.setAddress(userMessage.getAddress());
    }

    /**
     * Gets count of synchronized users.
     * 
     * @return Count of users in local database
     */
    public long getSynchronizedUserCount() {
        return userRepository.count();
    }

    /**
     * Checks if a user exists locally.
     * 
     * @param email User email
     * @return true if user exists
     */
    public boolean isUserSynchronized(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
