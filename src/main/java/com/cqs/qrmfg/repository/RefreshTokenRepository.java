package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.RefreshToken;
import com.cqs.qrmfg.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
} 