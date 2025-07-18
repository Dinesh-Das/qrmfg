package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.PasswordResetToken;
import com.cqs.qrmfg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
} 