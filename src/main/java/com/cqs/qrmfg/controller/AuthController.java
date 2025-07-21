package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.config.JwtUtil;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.repository.UserRepository;
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
import com.cqs.qrmfg.model.UserSession;
import com.cqs.qrmfg.repository.UserSessionRepository;
import com.cqs.qrmfg.service.AuditLogService;
import com.cqs.qrmfg.model.AuditLog;
import javax.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/qrmfg/api/v1/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private AuditLogService auditLogService;
    // Removed jwtRefreshExpirationInMs field

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
            // Removed refresh token logic
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
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

 

    // Remove or comment out the getMySessions and terminateMySession methods, as they are not required for the current feature set.
} 