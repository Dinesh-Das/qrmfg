package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
} 