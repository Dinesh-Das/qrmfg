package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.cqs.qrmfg.model.AuditLog;
import com.cqs.qrmfg.model.User;

@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> getUserActivitySummary(LocalDateTime startDate) {
        String sql = "SELECT " +
                     "u.username, " +
                     "COUNT(CASE WHEN a.action = 'CREATE_WORKFLOW' THEN 1 END) as workflows_created, " +
                     "COUNT(CASE WHEN a.action = 'RESOLVE_QUERY' THEN 1 END) as queries_resolved, " +
                     "COUNT(CASE WHEN a.action = 'RAISE_QUERY' THEN 1 END) as queries_raised, " +
                     "COUNT(CASE WHEN a.action = 'COMPLETE_WORKFLOW' THEN 1 END) as workflows_completed, " +
                     "COUNT(*) as total_actions, " +
                     "MAX(a.timestamp) as last_activity " +
                     "FROM qrmfg_users u " +
                     "LEFT JOIN qrmfg_audit_logs a ON u.username = a.username AND a.timestamp >= ? " +
                     "GROUP BY u.username " +
                     "ORDER BY total_actions DESC";
        
        List<Map<String, Object>> results = new ArrayList<>();
        jdbcTemplate.query(sql, new Object[]{startDate}, (rs, rowNum) -> {
            Map<String, Object> activity = new HashMap<>();
            activity.put("username", rs.getString("username"));
            activity.put("workflowsCreated", rs.getLong("workflows_created"));
            activity.put("queriesResolved", rs.getLong("queries_resolved"));
            activity.put("queriesRaised", rs.getLong("queries_raised"));
            activity.put("workflowsCompleted", rs.getLong("workflows_completed"));
            activity.put("totalActions", rs.getLong("total_actions"));
            activity.put("lastActivity", rs.getTimestamp("last_activity"));
            results.add(activity);
            return null;
        });
        
        return results;
    }

    @Override
    public List<Map<String, Object>> getFilteredAuditLogs(LocalDateTime startDate, LocalDateTime endDate, String entityType, String action) {
        StringBuilder sql = new StringBuilder("SELECT " +
                     "a.timestamp, " +
                     "a.username as user, " +
                     "a.action, " +
                     "a.entity_type, " +
                     "a.entity_id, " +
                     "a.details " +
                     "FROM qrmfg_audit_logs a " +
                     "WHERE 1=1");
        
        List<Object> params = new ArrayList<>();
        
        if (startDate != null) {
            sql.append(" AND a.timestamp >= ?");
            params.add(startDate);
        }
        
        if (endDate != null) {
            sql.append(" AND a.timestamp <= ?");
            params.add(endDate);
        }
        
        if (entityType != null && !entityType.trim().isEmpty()) {
            sql.append(" AND a.entity_type = ?");
            params.add(entityType);
        }
        
        if (action != null && !action.trim().isEmpty()) {
            sql.append(" AND a.action = ?");
            params.add(action);
        }
        
        sql.append(" ORDER BY a.timestamp DESC");
        
        List<Map<String, Object>> results = new ArrayList<>();
        jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Map<String, Object> log = new HashMap<>();
            log.put("timestamp", rs.getTimestamp("timestamp"));
            log.put("user", rs.getString("user"));
            log.put("action", rs.getString("action"));
            log.put("entityType", rs.getString("entity_type"));
            log.put("entityId", rs.getString("entity_id"));
            log.put("details", rs.getString("details"));
            results.add(log);
            return null;
        });
        
        return results;
    }

    @Override
    public void logAuditEvent(String user, String action, String entityType, String entityId, String details) {
        String sql = "INSERT INTO qrmfg_audit_logs (timestamp, username, action, entity_type, entity_id, details) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql, LocalDateTime.now(), user, action, entityType, entityId, details);
    }

    @Override
    public List<AuditLog> findAll() {
        String sql = "SELECT id, user_id, action, entity_type, entity_id, details, event_timestamp, severity FROM qrmfg_audit_logs";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            AuditLog log = new AuditLog();
            log.setId(rs.getLong("id"));
            // Set user as null or fetch if needed; here, just set userId as a User with only id
            Long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                User user = new User();
                user.setId(userId);
                log.setUser(user);
            }
            log.setAction(rs.getString("action"));
            log.setEntityType(rs.getString("entity_type"));
            log.setEntityId(rs.getString("entity_id"));
            log.setDetails(rs.getString("details"));
            log.setEventTime(rs.getTimestamp("event_timestamp").toLocalDateTime());
            log.setSeverity(rs.getString("severity"));
            return log;
        });
    }

    @Override
    public List<AuditLog> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, action, entity_type, entity_id, details, event_timestamp, severity FROM qrmfg_audit_logs WHERE user_id = ?";
        return jdbcTemplate.query(sql, new Object[]{userId}, (rs, rowNum) -> {
            AuditLog log = new AuditLog();
            log.setId(rs.getLong("id"));
            Long uid = rs.getLong("user_id");
            if (!rs.wasNull()) {
                User user = new User();
                user.setId(uid);
                log.setUser(user);
            }
            log.setAction(rs.getString("action"));
            log.setEntityType(rs.getString("entity_type"));
            log.setEntityId(rs.getString("entity_id"));
            log.setDetails(rs.getString("details"));
            log.setEventTime(rs.getTimestamp("event_timestamp").toLocalDateTime());
            log.setSeverity(rs.getString("severity"));
            return log;
        });
    }

    @Override
    public void save(AuditLog log) {
        String sql = "INSERT INTO qrmfg_audit_logs (user_id, action, entity_type, entity_id, description, ip_address, user_agent, session_id, event_timestamp, severity, category, event_type, event_category, details, resource_path, result, errorMessage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Long userId = log.getUser() != null ? log.getUser().getId() : null;
        jdbcTemplate.update(sql,
            userId,
            log.getAction(),
            log.getEntityType(),
            log.getEntityId(),
            log.getDescription(),
            log.getIpAddress(),
            log.getUserAgent(),
            log.getSessionId(),
            log.getEventTime(),
            log.getSeverity(),
            log.getCategory(),
            log.getEventType(),
            log.getEventCategory(),
            log.getDetails(),
            log.getResource(),
            log.getResult(),
            log.getErrorMessage()
        );
    }

    @Override
    public List<AuditLog> findByAction(String action) {
        String sql = "SELECT id, user_id, action, entity_type, entity_id, details, event_timestamp, severity FROM qrmfg_audit_logs WHERE action = ?";
        return jdbcTemplate.query(sql, new Object[]{action}, (rs, rowNum) -> {
            AuditLog log = new AuditLog();
            log.setId(rs.getLong("id"));
            Long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                User user = new User();
                user.setId(userId);
                log.setUser(user);
            }
            log.setAction(rs.getString("action"));
            log.setEntityType(rs.getString("entity_type"));
            log.setEntityId(rs.getString("entity_id"));
            log.setDetails(rs.getString("details"));
            log.setEventTime(rs.getTimestamp("event_timestamp").toLocalDateTime());
            log.setSeverity(rs.getString("severity"));
            return log;
        });
    }

    @Override
    public List<AuditLog> findByEntityType(String entityType) {
        String sql = "SELECT id, user_id, action, entity_type, entity_id, details, event_timestamp, severity FROM qrmfg_audit_logs WHERE entity_type = ?";
        return jdbcTemplate.query(sql, new Object[]{entityType}, (rs, rowNum) -> {
            AuditLog log = new AuditLog();
            log.setId(rs.getLong("id"));
            Long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                User user = new User();
                user.setId(userId);
                log.setUser(user);
            }
            log.setAction(rs.getString("action"));
            log.setEntityType(rs.getString("entity_type"));
            log.setEntityId(rs.getString("entity_id"));
            log.setDetails(rs.getString("details"));
            log.setEventTime(rs.getTimestamp("event_timestamp").toLocalDateTime());
            log.setSeverity(rs.getString("severity"));
            return log;
        });
    }

    @Override
    public List<AuditLog> findBySeverity(String severity) {
        String sql = "SELECT id, user_id, action, entity_type, entity_id, details, event_timestamp, severity FROM qrmfg_audit_logs WHERE severity = ?";
        return jdbcTemplate.query(sql, new Object[]{severity}, (rs, rowNum) -> {
            AuditLog log = new AuditLog();
            log.setId(rs.getLong("id"));
            Long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                User user = new User();
                user.setId(userId);
                log.setUser(user);
            }
            log.setAction(rs.getString("action"));
            log.setEntityType(rs.getString("entity_type"));
            log.setEntityId(rs.getString("entity_id"));
            log.setDetails(rs.getString("details"));
            log.setEventTime(rs.getTimestamp("event_timestamp").toLocalDateTime());
            log.setSeverity(rs.getString("severity"));
            return log;
        });
    }

    @Override
    public List<AuditLog> findByEventTimeBetween(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        String sql = "SELECT id, user_id, action, entity_type, entity_id, details, event_timestamp, severity FROM qrmfg_audit_logs WHERE event_timestamp BETWEEN ? AND ?";
        return jdbcTemplate.query(sql, new Object[]{start, end}, (rs, rowNum) -> {
            AuditLog log = new AuditLog();
            log.setId(rs.getLong("id"));
            Long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                User user = new User();
                user.setId(userId);
                log.setUser(user);
            }
            log.setAction(rs.getString("action"));
            log.setEntityType(rs.getString("entity_type"));
            log.setEntityId(rs.getString("entity_id"));
            log.setDetails(rs.getString("details"));
            log.setEventTime(rs.getTimestamp("event_timestamp").toLocalDateTime());
            log.setSeverity(rs.getString("severity"));
            return log;
        });
    }

    @Override
    public AuditLog findById(Long id) {
        String sql = "SELECT id, user_id, action, entity_type, entity_id, details, event_timestamp, severity FROM qrmfg_audit_logs WHERE id = ?";
        return jdbcTemplate.query(sql, new Object[]{id}, (rs, rowNum) -> {
            AuditLog log = new AuditLog();
            log.setId(rs.getLong("id"));
            Long userId = rs.getLong("user_id");
            if (!rs.wasNull()) {
                User user = new User();
                user.setId(userId);
                log.setUser(user);
            }
            log.setAction(rs.getString("action"));
            log.setEntityType(rs.getString("entity_type"));
            log.setEntityId(rs.getString("entity_id"));
            log.setDetails(rs.getString("details"));
            log.setEventTime(rs.getTimestamp("event_timestamp").toLocalDateTime());
            log.setSeverity(rs.getString("severity"));
            return log;
        }).stream().findFirst().orElse(null);
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM qrmfg_audit_logs WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}