package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.model.AuditLog;
import com.cqs.qrmfg.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/qrmfg/api/v1/audit/logs")
public class AuditLogController {
    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public List<AuditLog> getAllLogs() {
        return auditLogService.findAll();
    }

    @GetMapping("/user/{userId}")
    public List<AuditLog> getLogsByUser(@PathVariable Long userId) {
        return auditLogService.findByUserId(userId);
    }

    @GetMapping("/action/{action}")
    public List<AuditLog> getLogsByAction(@PathVariable String action) {
        return auditLogService.findByAction(action);
    }

    @GetMapping("/entity/{entityType}")
    public List<AuditLog> getLogsByEntityType(@PathVariable String entityType) {
        return auditLogService.findByEntityType(entityType);
    }

    @GetMapping("/severity/{severity}")
    public List<AuditLog> getLogsBySeverity(@PathVariable String severity) {
        return auditLogService.findBySeverity(severity);
    }

    @GetMapping("/date")
    public List<AuditLog> getLogsByDateRange(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return auditLogService.findByEventTimeBetween(start, end);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> getLogById(@PathVariable Long id) {
        AuditLog log = auditLogService.findById(id);
        return log != null ? ResponseEntity.ok(log) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLog(@PathVariable Long id) {
        auditLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
} 