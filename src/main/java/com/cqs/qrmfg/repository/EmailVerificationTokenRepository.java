package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.EmailVerificationToken;
import com.cqs.qrmfg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByUser(User user);
} 