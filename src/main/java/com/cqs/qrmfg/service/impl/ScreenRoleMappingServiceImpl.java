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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ScreenRoleMappingServiceImpl implements ScreenRoleMappingService {
    private static final Logger logger = LoggerFactory.getLogger(ScreenRoleMappingServiceImpl.class);

    @Autowired
    private ScreenRoleMappingRepository mappingRepository;
    
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public Map<String, List<Long>> getAllMappings() {
        Map<String, List<Long>> result = new HashMap<>();
        List<ScreenRoleMapping> allMappings = mappingRepository.findAll();
        
        for (ScreenRoleMapping mapping : allMappings) {
            result.computeIfAbsent(mapping.getRoute(), k -> new ArrayList<>())
                  .add(mapping.getRole().getId());
        }
        
        logger.debug("Retrieved all mappings: {}", result);
        return result;
    }

    @Override
    @Transactional
    public void updateMapping(String route, List<Long> roleIds) {
        logger.debug("Updating mapping for route: {} with roles: {}", route, roleIds);
        
        // Delete existing mappings for this route
        mappingRepository.deleteByRoute(route);

        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                Optional<Role> roleOpt = roleRepository.findById(roleId);
                if (roleOpt.isPresent()) {
                    Role role = roleOpt.get();
                    ScreenRoleMapping mapping = new ScreenRoleMapping(route, role);
                    mappingRepository.save(mapping);
                    logger.debug("Created mapping: {} -> {}", route, role.getName());
                } else {
                    logger.warn("Role not found for ID: {}", roleId);
                }
            }
        }
    }

    @Override
    public List<String> getAllowedRoutesForRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            logger.debug("No roles provided, returning empty list");
            return Collections.emptyList();
        }

        // Check if any of the roles is ADMIN
        List<Role> roles = roleRepository.findAllById(roleIds);
        boolean isAdmin = roles.stream()
            .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));

        if (isAdmin) {
            logger.debug("User has ADMIN role, returning all routes");
            return mappingRepository.findAll().stream()
                .map(ScreenRoleMapping::getRoute)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        }

        // For non-admin users, get their specific routes
        Set<String> allowedRoutes = new HashSet<>();
        List<ScreenRoleMapping> allMappings = mappingRepository.findAll();
        
        for (ScreenRoleMapping mapping : allMappings) {
            if (roleIds.contains(mapping.getRole().getId())) {
                allowedRoutes.add(mapping.getRoute());
            }
        }

        logger.debug("Allowed routes for roles {}: {}", roleIds, allowedRoutes);
        return new ArrayList<>(allowedRoutes);
    }
} 