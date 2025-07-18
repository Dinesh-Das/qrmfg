package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.config.JwtUtil;
import com.cqs.qrmfg.model.RefreshToken;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.PasswordResetToken;
import com.cqs.qrmfg.repository.RefreshTokenRepository;
import com.cqs.qrmfg.repository.UserRepository;
import com.cqs.qrmfg.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
import com.cqs.qrmfg.model.EmailVerificationToken;
import com.cqs.qrmfg.repository.EmailVerificationTokenRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.cqs.qrmfg.model.UserSession;
import com.cqs.qrmfg.repository.UserSessionRepository;
import com.cqs.qrmfg.service.AuditLogService;
import com.cqs.qrmfg.model.AuditLog;
import javax.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private AuditLogService auditLogService;
    @Value("${jwt.refreshExpiration:604800000}")
    private long jwtRefreshExpirationInMs;
    @Value("${jwt.passwordResetExpiration:3600000}") // 1 hour default
    private long passwordResetExpirationInMs;
    @Value("${jwt.emailVerificationExpiration:86400000}") // 24 hours default
    private long emailVerificationExpirationInMs;

    @Transactional
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest, HttpServletRequest request) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            String token = jwtUtil.generateToken(username);
            String refreshToken = jwtUtil.generateRefreshToken(username);
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                refreshTokenRepository.deleteByUser(userOpt.get());
                RefreshToken refreshTokenEntity = new RefreshToken(refreshToken, userOpt.get(), LocalDateTime.now().plusNanos(jwtRefreshExpirationInMs * 1_000_000));
                refreshTokenRepository.save(refreshTokenEntity);
            }
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("refreshToken", refreshToken);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            // Log failed login as a security event
            String username = loginRequest.get("username");
            AuditLog log = new AuditLog();
            log.setAction("LOGIN_FAILED");
            log.setEntityType("User");
            log.setDescription("Failed login attempt for username: " + username);
            log.setSeverity("SECURITY");
            log.setCategory("SECURITY");
            log.setIpAddress(request.getRemoteAddr());
            log.setUserAgent(request.getHeader("User-Agent"));
            log.setResult("FAILURE");
            auditLogService.save(log);
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body("Refresh token is required");
        }
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(refreshToken);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
        RefreshToken tokenEntity = tokenOpt.get();
        if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(tokenEntity);
            return ResponseEntity.status(401).body("Refresh token expired");
        }
        String username = tokenEntity.getUser().getUsername();
        if (!jwtUtil.validateRefreshToken(refreshToken, username)) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
        String newToken = jwtUtil.generateToken(username);
        Map<String, String> response = new HashMap<>();
        response.put("token", newToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        if (token == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Token and new password are required");
        }
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(400).body("Invalid or expired token");
        }
        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            return ResponseEntity.status(400).body("Token expired");
        }
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);
        return ResponseEntity.ok("Password reset successful");
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        Optional<EmailVerificationToken> tokenOpt = emailVerificationTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(400).body("Invalid or expired token");
        }
        EmailVerificationToken verificationToken = tokenOpt.get();
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            emailVerificationTokenRepository.delete(verificationToken);
            return ResponseEntity.status(400).body("Token expired");
        }
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        emailVerificationTokenRepository.delete(verificationToken);
        return ResponseEntity.ok("Email verified successfully");
    }

    @GetMapping("/me/sessions")
    public ResponseEntity<?> getMySessions(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Optional<User> userOpt = userRepository.findByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        User user = userOpt.get();
        return ResponseEntity.ok(userSessionRepository.findByUser(user));
    }

    @DeleteMapping("/me/sessions/{sessionId}")
    public ResponseEntity<?> terminateMySession(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String sessionId) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Optional<User> userOpt = userRepository.findByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }
        User user = userOpt.get();
        Optional<UserSession> sessionOpt = userSessionRepository.findBySessionId(sessionId);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(404).body("Session not found");
        }
        UserSession session = sessionOpt.get();
        session.terminate();
        userSessionRepository.save(session);
        return ResponseEntity.ok("Session terminated");
    }
} 