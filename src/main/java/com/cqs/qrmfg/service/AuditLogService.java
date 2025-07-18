package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {
    AuditLog save(AuditLog log);
    void delete(Long id);
    AuditLog findById(Long id);
    List<AuditLog> findAll();
    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByEntityType(String entityType);
    List<AuditLog> findBySeverity(String severity);
    List<AuditLog> findByEventTimeBetween(LocalDateTime start, LocalDateTime end);
} 