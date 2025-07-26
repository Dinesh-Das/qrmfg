package com.cqs.qrmfg.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "QRMFG_SESSIONS")
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sessions_seq")
    @SequenceGenerator(name = "sessions_seq", sequenceName = "QRMFG_SESSIONS_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime endedAt;

    @Column
    private LocalDateTime lastActivityAt;

    @Column
    private LocalDateTime expiresAt;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 1000)
    private String sessionData;

    @Column(length = 20)
    private String deviceType;

    @Column(length = 100)
    private String location;

    @Column(length = 20)
    private String browser;

    @Column(length = 20)
    private String os;

    public UserSession() {}

    public UserSession(User user, String sessionId) {
        this.user = user;
        this.sessionId = sessionId;
    }
    public UserSession(User user, String sessionId, String ipAddress, String userAgent) {
        this.user = user;
        this.sessionId = sessionId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        lastActivityAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(24);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastActivityAt = LocalDateTime.now();
    }

    // Getters and setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isActive() { return active && !isExpired() && "ACTIVE".equals(status); }
    public void setActive(boolean active) { this.active = active; }
    public String getSessionData() { return sessionData; }
    public void setSessionData(String sessionData) { this.sessionData = sessionData; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }

    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    public void terminate() {
        this.active = false;
        this.status = "TERMINATED";
        this.endedAt = LocalDateTime.now();
    }
    public void extendSession(int hours) {
        this.expiresAt = LocalDateTime.now().plusHours(hours);
        this.lastActivityAt = LocalDateTime.now();
    }
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
    public long getSessionDurationMinutes() {
        LocalDateTime endTime = endedAt != null ? endedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).toMinutes();
    }
    public boolean isIdle(int idleMinutes) {
        if (lastActivityAt == null) return true;
        return LocalDateTime.now().isAfter(lastActivityAt.plusMinutes(idleMinutes));
    }
} 