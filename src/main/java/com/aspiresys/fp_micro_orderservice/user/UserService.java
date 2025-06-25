package com.aspiresys.fp_micro_orderservice.user;

import java.util.List;

public interface UserService {
    /**
     * Saves a user to the repository.
     *
     * @param user the user to save
     * @return the saved user
     */
    User saveUser(User user);

    /**
     * Retrieves a user by their email.
     *
     * @param email the email of the user to retrieve
     * @return the user with the specified email, or null if not found
     */
    User getUserByEmail(String email);

    /**
     * Retrieves a user by their ID.
     *
     * @param id the ID of the user to retrieve
     * @return the user with the specified ID, or null if not found
     */
    User getUserById(Long id);

    /**
     * Retrieves all users from the repository.
     *
     * @return a list of all users
     */
    List<User> getAllUsers();

    /**
     * Deletes a user by their ID.
     *
     * @param id the ID of the user to delete
     * @return 
     */
    boolean deleteUserById(Long id);
    /**
     * Deletes a user by their email.
     *
     * @param email the email of the user to delete
     */
    boolean deleteUserByEmail(String email);
    /**
     * Updates an existing user.
     *
     * @param user the user with updated information
     * @return the updated user
     */
    User updateUser(User user);

    /**
     * Checks if a user exists by their email.
     *
     * @param email the email of the user to check
     * @return true if the user exists, false otherwise
     */
    boolean userExistsByEmail(String email);

    
}