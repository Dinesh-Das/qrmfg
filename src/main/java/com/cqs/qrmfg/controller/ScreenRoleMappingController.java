package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.service.ScreenRoleMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.Role;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/qrmfg/api/v1/admin/screen-role-mapping")
public class ScreenRoleMappingController {
    private static final Logger logger = LoggerFactory.getLogger(ScreenRoleMappingController.class);

    @Autowired
    private ScreenRoleMappingService mappingService;

    @GetMapping
    public ResponseEntity<Map<String, List<Long>>> getAllMappings() {
        Map<String, List<Long>> mappings = mappingService.getAllMappings();
        logger.debug("Retrieved all mappings: {}", mappings);
        return ResponseEntity.ok(mappings);
    }

    @PutMapping
    public ResponseEntity<Void> updateMapping(@RequestBody Map<String, Object> payload) {
        String route = (String) payload.get("route");
        @SuppressWarnings("unchecked")
        List<Number> rolesNumbers = (List<Number>) payload.get("roles");
        
        List<Long> roles = null;
        if (rolesNumbers != null) {
            roles = rolesNumbers.stream()
                .map(Number::longValue)
                .collect(Collectors.toList());
        }

        logger.debug("Updating mapping - Route: {}, Roles: {}", route, roles);
        mappingService.updateMapping(route, roles);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-screens")
    public ResponseEntity<List<String>> getMyAllowedScreens() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            logger.warn("No authenticated user found or invalid principal type");
            return ResponseEntity.ok(Collections.emptyList());
        }

        User user = (User) auth.getPrincipal();
        Set<Role> userRoles = user.getRoles();
        
        // Check if user has ADMIN role
        boolean isAdmin = userRoles.stream()
            .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));

        List<Long> roleIds = userRoles.stream()
            .map(Role::getId)
            .collect(Collectors.toList());

        logger.debug("Getting allowed screens for user: {} (Admin: {}), Roles: {}", 
            user.getUsername(), isAdmin, roleIds);

        List<String> allowedScreens = mappingService.getAllowedRoutesForRoles(roleIds);
        logger.debug("Allowed screens for user {}: {}", user.getUsername(), allowedScreens);
        
        return ResponseEntity.ok(allowedScreens);
    }
} 