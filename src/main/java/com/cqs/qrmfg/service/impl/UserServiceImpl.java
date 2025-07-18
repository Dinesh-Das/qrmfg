package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.repository.UserRepository;
import com.cqs.qrmfg.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User save(User user) {
        // Only encode if not already encoded
        if (!user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public User update(User user) {
        Optional<User> existingOpt = userRepository.findById(user.getId());
        if (!existingOpt.isPresent()) {
            throw new RuntimeException("User not found");
        }
        User existing = existingOpt.get();
        // Update mutable fields only
        existing.setUsername(user.getUsername());
        existing.setEmail(user.getEmail());
        existing.setStatus(user.getStatus());
        existing.setEnabled(user.isEnabled());
        existing.setEmailVerified(user.isEmailVerified());
        existing.setPhoneVerified(user.isPhoneVerified());
        existing.setAccountExpiresAt(user.getAccountExpiresAt());
        existing.setPasswordExpiresAt(user.getPasswordExpiresAt());
        existing.setLastLoginAt(user.getLastLoginAt());
        existing.setPasswordChangedAt(user.getPasswordChangedAt());
        existing.setUpdatedBy(user.getUpdatedBy());
        existing.setRoles(user.getRoles());
        // Only update password if changed
        if (user.getPassword() != null && !user.getPassword().isEmpty() && !user.getPassword().equals(existing.getPassword())) {
            if (!user.getPassword().startsWith("$2a$")) {
                existing.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                existing.setPassword(user.getPassword());
            }
        }
        // Do NOT update createdAt or createdBy
        return userRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }
} 