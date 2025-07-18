package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.ScreenRoleMapping;
import com.cqs.qrmfg.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScreenRoleMappingRepository extends JpaRepository<ScreenRoleMapping, Long> {
    List<ScreenRoleMapping> findByRoute(String route);
    List<ScreenRoleMapping> findByRole(Role role);
    void deleteByRoute(String route);
} 