package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.service.ScreenRoleMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.cqs.qrmfg.model.User;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/screen-role-mapping")
public class ScreenRoleMappingController {
    @Autowired
    private ScreenRoleMappingService mappingService;

    @GetMapping
    public ResponseEntity<Map<String, List<Long>>> getAllMappings() {
        return ResponseEntity.ok(mappingService.getAllMappings());
    }

    @PutMapping
    public ResponseEntity<Void> updateMapping(@RequestBody Map<String, Object> payload) {
        String route = (String) payload.get("route");
        List<Integer> rolesInt = (List<Integer>) payload.get("roles");
        List<Long> roles = new ArrayList<>();
        if (rolesInt != null) {
            for (Integer i : rolesInt) roles.add(i.longValue());
        }
        mappingService.updateMapping(route, roles);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-screens")
    public List<String> getMyAllowedScreens() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) return Collections.emptyList();
        User user = (User) auth.getPrincipal();
        List<Long> roleIds = user.getRoles().stream().map(r -> r.getId()).collect(Collectors.toList());
        return mappingService.getAllowedRoutesForRoles(roleIds);
    }
} 