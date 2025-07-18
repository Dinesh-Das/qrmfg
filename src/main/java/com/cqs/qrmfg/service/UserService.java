package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    User save(User user);
    User update(User user);
    void delete(Long id);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findAll();
} 