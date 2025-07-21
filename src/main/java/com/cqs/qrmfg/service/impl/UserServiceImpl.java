package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.dto.UserRoleAssignmentDto;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setEnabled(rs.getBoolean("enabled"));
            return user;
        }
    };

    @Override
    public List<UserRoleAssignmentDto> getAllUserRoleAssignments() {
        String sql = "SELECT DISTINCT " +
                     "u.id as user_id, " +
                     "u.username, " +
                     "u.email " +
                     "FROM qrmfg_users u " +
                     "ORDER BY u.username";
        
        List<UserRoleAssignmentDto> assignments = new ArrayList<>();
        Map<Long, UserRoleAssignmentDto> userMap = new HashMap<>();
        
        // Get all users
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long userId = rs.getLong("user_id");
            UserRoleAssignmentDto dto = new UserRoleAssignmentDto();
            dto.setUserId(userId);
            dto.setUsername(rs.getString("username"));
            dto.setEmail(rs.getString("email"));
            dto.setAssignedRoles(new ArrayList<>());
            dto.setAvailableRoles(new ArrayList<>());
            
            userMap.put(userId, dto);
            assignments.add(dto);
            return null;
        });
        
        // Get assigned roles for each user
        String rolesSql = "SELECT " +
                          "ur.user_id, " +
                          "r.id as role_id, " +
                          "r.name as role_name, " +
                          "r.description as role_description " +
                          "FROM qrmfg_user_roles ur " +
                          "JOIN qrmfg_roles r ON ur.role_id = r.id " +
                          "ORDER BY ur.user_id, r.name";
        
        jdbcTemplate.query(rolesSql, (rs, rowNum) -> {
            Long userId = rs.getLong("user_id");
            UserRoleAssignmentDto user = userMap.get(userId);
            if (user != null) {
                UserRoleAssignmentDto.RoleDto role = new UserRoleAssignmentDto.RoleDto();
                role.setId(rs.getLong("role_id"));
                role.setName(rs.getString("role_name"));
                role.setDescription(rs.getString("role_description"));
                user.getAssignedRoles().add(role);
            }
            return null;
        });
        
        // Get all available roles
        String allRolesSql = "SELECT " +
                             "r.id as role_id, " +
                             "r.name as role_name, " +
                             "r.description as role_description " +
                             "FROM qrmfg_roles r " +
                             "ORDER BY r.name";
        
        List<UserRoleAssignmentDto.RoleDto> allRoles = new ArrayList<>();
        jdbcTemplate.query(allRolesSql, (rs, rowNum) -> {
            UserRoleAssignmentDto.RoleDto role = new UserRoleAssignmentDto.RoleDto();
            role.setId(rs.getLong("role_id"));
            role.setName(rs.getString("role_name"));
            role.setDescription(rs.getString("role_description"));
            allRoles.add(role);
            return null;
        });
        
        // Set available roles for each user (all roles not currently assigned)
        for (UserRoleAssignmentDto user : assignments) {
            List<Long> assignedRoleIds = user.getAssignedRoles().stream()
                    .map(UserRoleAssignmentDto.RoleDto::getId)
                    .collect(Collectors.toList());
            
            List<UserRoleAssignmentDto.RoleDto> availableRoles = allRoles.stream()
                    .filter(role -> !assignedRoleIds.contains(role.getId()))
                    .collect(Collectors.toList());
            
            user.setAvailableRoles(availableRoles);
        }
        
        return assignments;
    }

    @Override
    public UserRoleAssignmentDto updateUserRoles(Long userId, List<Long> roleIds) {
        // Remove existing role assignments
        String deleteSql = "DELETE FROM qrmfg_user_roles WHERE user_id = ?";
        jdbcTemplate.update(deleteSql, userId);
        
        // Add new role assignments
        if (roleIds != null && !roleIds.isEmpty()) {
            String insertSql = "INSERT INTO qrmfg_user_roles (user_id, role_id) VALUES (?, ?)";
            for (Long roleId : roleIds) {
                jdbcTemplate.update(insertSql, userId, roleId);
            }
        }
        
        // Return updated user
        return getUserById(userId);
    }

    @Override
    public UserRoleAssignmentDto getUserById(Long userId) {
        String sql = "SELECT " +
                     "u.id as user_id, " +
                     "u.username, " +
                     "u.email " +
                     "FROM qrmfg_users u " +
                     "WHERE u.id = ?";
        
        UserRoleAssignmentDto dto = jdbcTemplate.queryForObject(sql, new Object[]{userId}, (rs, rowNum) -> {
            UserRoleAssignmentDto user = new UserRoleAssignmentDto();
            user.setUserId(rs.getLong("user_id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setAssignedRoles(new ArrayList<>());
            user.setAvailableRoles(new ArrayList<>());
            return user;
        });
        
        if (dto != null) {
            // Get assigned roles
            String rolesSql = "SELECT " +
                              "r.id as role_id, " +
                              "r.name as role_name, " +
                              "r.description as role_description " +
                              "FROM qrmfg_user_roles ur " +
                              "JOIN qrmfg_roles r ON ur.role_id = r.id " +
                              "WHERE ur.user_id = ? " +
                              "ORDER BY r.name";
            
            List<UserRoleAssignmentDto.RoleDto> assignedRoles = new ArrayList<>();
            jdbcTemplate.query(rolesSql, new Object[]{userId}, (rs, rowNum) -> {
                UserRoleAssignmentDto.RoleDto role = new UserRoleAssignmentDto.RoleDto();
                role.setId(rs.getLong("role_id"));
                role.setName(rs.getString("role_name"));
                role.setDescription(rs.getString("role_description"));
                assignedRoles.add(role);
                return null;
            });
            dto.setAssignedRoles(assignedRoles);
            
            // Get all available roles
            String allRolesSql = "SELECT " +
                                 "r.id as role_id, " +
                                 "r.name as role_name, " +
                                 "r.description as role_description " +
                                 "FROM qrmfg_roles r " +
                                 "ORDER BY r.name";
            
            List<UserRoleAssignmentDto.RoleDto> allRoles = new ArrayList<>();
            jdbcTemplate.query(allRolesSql, (rs, rowNum) -> {
                UserRoleAssignmentDto.RoleDto role = new UserRoleAssignmentDto.RoleDto();
                role.setId(rs.getLong("role_id"));
                role.setName(rs.getString("role_name"));
                role.setDescription(rs.getString("role_description"));
                allRoles.add(role);
                return null;
            });
            
            List<Long> assignedRoleIds = assignedRoles.stream()
                    .map(UserRoleAssignmentDto.RoleDto::getId)
                    .collect(Collectors.toList());
            
            List<UserRoleAssignmentDto.RoleDto> availableRoles = allRoles.stream()
                    .filter(role -> !assignedRoleIds.contains(role.getId()))
                    .collect(Collectors.toList());
            
            dto.setAvailableRoles(availableRoles);
        }
        
        return dto;
    }

    @Override
    public UserRoleAssignmentDto getUserByUsername(String username) {
        String sql = "SELECT " +
                     "u.id as user_id, " +
                     "u.username, " +
                     "u.email " +
                     "FROM qrmfg_users u " +
                     "WHERE u.username = ?";
        
        UserRoleAssignmentDto dto = jdbcTemplate.queryForObject(sql, new Object[]{username}, (rs, rowNum) -> {
            UserRoleAssignmentDto user = new UserRoleAssignmentDto();
            user.setUserId(rs.getLong("user_id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setAssignedRoles(new ArrayList<>());
            user.setAvailableRoles(new ArrayList<>());
            return user;
        });
        
        if (dto != null) {
            return getUserById(dto.getUserId());
        }
        
        return null;
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, username, email, password, enabled FROM qrmfg_users";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, email, password, enabled FROM qrmfg_users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, new Object[]{id}, userRowMapper);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO qrmfg_users (username, email, password, enabled) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql,
            user.getUsername(),
            user.getEmail(),
            user.getPassword(),
            user.isEnabled()
        );

        // Get the newly created user
        String findSql = "SELECT id, username, email, password, enabled FROM qrmfg_users WHERE username = ?";
        List<User> users = jdbcTemplate.query(findSql, new Object[]{user.getUsername()}, userRowMapper);
        return users.get(0);
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE qrmfg_users SET username = ?, email = ?, password = ?, enabled = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql,
            user.getUsername(),
            user.getEmail(),
            user.getPassword(),
            user.isEnabled(),
            user.getId()
        );

        if (updated == 0) {
            throw new RuntimeException("User not found with id: " + user.getId());
        }

        return findById(user.getId()).orElseThrow(() -> new RuntimeException("User not found after update"));
    }

    @Override
    public void delete(Long id) {
        // First delete user roles
        String deleteRolesSql = "DELETE FROM qrmfg_user_roles WHERE user_id = ?";
        jdbcTemplate.update(deleteRolesSql, id);

        // Then delete the user
        String deleteUserSql = "DELETE FROM qrmfg_users WHERE id = ?";
        int deleted = jdbcTemplate.update(deleteUserSql, id);

        if (deleted == 0) {
            throw new RuntimeException("User not found with id: " + id);
        }
    }
}