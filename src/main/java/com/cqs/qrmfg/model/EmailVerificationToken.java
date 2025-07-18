package com.cqs.qrmfg.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qrmfg_email_verification_tokens")
public class EmailVerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_verification_token_seq")
    @SequenceGenerator(name = "email_verification_token_seq", sequenceName = "RBAC_EMAIL_VERIFICATION_TOKEN_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public EmailVerificationToken() {}

    public EmailVerificationToken(String token, User user, LocalDateTime expiryDate) {
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
} 