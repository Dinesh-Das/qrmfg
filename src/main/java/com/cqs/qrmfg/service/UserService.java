package com.cqs.qrmfg.service;

import com.cqs.qrmfg.dto.UserRoleAssignmentDto;
import com.cqs.qrmfg.model.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for user management operations
 */
public interface UserService {

    /**
     * Get all user role assignments
     * @return List of UserRoleAssignmentDto objects
     */
    List<UserRoleAssignmentDto> getAllUserRoleAssignments();

    /**
     * Update user roles
     * @param userId User ID
     * @param roleIds List of role IDs to assign
     * @return Updated UserRoleAssignmentDto
     */
    UserRoleAssignmentDto updateUserRoles(Long userId, List<Long> roleIds);

    /**
     * Get user by ID
     * @param userId User ID
     * @return UserRoleAssignmentDto
     */
    UserRoleAssignmentDto getUserById(Long userId);

    /**
     * Get user by username
     * @param username Username
     * @return UserRoleAssignmentDto
     */
    UserRoleAssignmentDto getUserByUsername(String username);

    /**
     * Find all users
     * @return List of all users
     */
    List<User> findAll();

    /**
     * Find user by ID
     * @param id User ID
     * @return Optional of User
     */
    Optional<User> findById(Long id);

    /**
     * Save a new user
     * @param user User to save
     * @return Saved user
     */
    User save(User user);

    /**
     * Update an existing user
     * @param user User to update
     * @return Updated user
     */
    User update(User user);

    /**
     * Delete a user by ID
     * @param id User ID to delete
     */
    void delete(Long id);

    Map<String, Object> getNotificationPreferences(String username);
    void updateNotificationPreferences(String username, Map<String, Object> preferences);
}