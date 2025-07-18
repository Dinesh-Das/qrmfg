package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.model.ScreenRoleMapping;
import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.repository.ScreenRoleMappingRepository;
import com.cqs.qrmfg.repository.RoleRepository;
import com.cqs.qrmfg.service.ScreenRoleMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ScreenRoleMappingServiceImpl implements ScreenRoleMappingService {
    @Autowired
    private ScreenRoleMappingRepository mappingRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public Map<String, List<Long>> getAllMappings() {
        Map<String, List<Long>> result = new HashMap<>();
        for (ScreenRoleMapping mapping : mappingRepository.findAll()) {
            result.computeIfAbsent(mapping.getRoute(), k -> new ArrayList<>()).add(mapping.getRole().getId());
        }
        return result;
    }

    @Override
    @Transactional
    public void updateMapping(String route, List<Long> roleIds) {
        mappingRepository.deleteByRoute(route);
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                Optional<Role> roleOpt = roleRepository.findById(roleId);
                roleOpt.ifPresent(role -> mappingRepository.save(new ScreenRoleMapping(route, role)));
            }
        }
    }

    @Override
    public List<String> getAllowedRoutesForRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return Collections.emptyList();
        Set<String> allowedRoutes = new HashSet<>();
        for (ScreenRoleMapping mapping : mappingRepository.findAll()) {
            if (roleIds.contains(mapping.getRole().getId())) {
                allowedRoutes.add(mapping.getRoute());
            }
        }
        return new ArrayList<>(allowedRoutes);
    }
} 