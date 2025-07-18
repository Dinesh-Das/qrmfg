package com.cqs.qrmfg.service;

import com.cqs.qrmfg.model.Role;
import java.util.List;
import java.util.Optional;

public interface RoleService {
    Role save(Role role);
    Role update(Role role);
    void delete(Long id);
    Optional<Role> findById(Long id);
    Optional<Role> findByName(String name);
    List<Role> findAll();
} 