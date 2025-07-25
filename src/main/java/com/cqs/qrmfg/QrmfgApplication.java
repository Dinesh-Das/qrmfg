// Legacy main application. New RBAC backend will be under com.cqs.qrmfg.rbac_app
package com.cqs.qrmfg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.repository.UserRepository;
import com.cqs.qrmfg.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;
import com.cqs.qrmfg.model.ScreenRoleMapping;
import com.cqs.qrmfg.repository.ScreenRoleMappingRepository;
import java.util.Arrays;
import java.util.List;

// Security configuration will be implemented in com.cqs.qrmfg.config according to Spring Security and JWT best practices.
@SpringBootApplication
@EnableScheduling
public class QrmfgApplication {

	@Autowired
	private PasswordEncoder passwordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(QrmfgApplication.class, args);
	}

	@Bean
	public CommandLineRunner ensureAdminUser(
        UserRepository userRepository,
        RoleRepository roleRepository,
        ScreenRoleMappingRepository screenRoleMappingRepository,
        PasswordEncoder passwordEncoder) {
    return args -> {
        try {
            initializeAdminUser(userRepository, roleRepository, screenRoleMappingRepository, passwordEncoder);
        } catch (Exception e) {
            System.err.println("Error during admin user initialization: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the application startup for this
        }
    };
}

@Transactional
private void initializeAdminUser(
        UserRepository userRepository,
        RoleRepository roleRepository,
        ScreenRoleMappingRepository screenRoleMappingRepository,
        PasswordEncoder passwordEncoder) {
    
    String adminUsername = "admin";
    String adminPassword = "admin";
    String adminEmail = "admin@example.com";

    // Ensure ADMIN role exists
    Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
        Role r = new Role();
        r.setName("ADMIN");
        r.setDescription("System administrator role");
        r.setEnabled(true);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return roleRepository.save(r);
    });

    // Ensure other required roles exist
    createRoleIfNotExists(roleRepository, "JVC_USER", "JVC User role");
    createRoleIfNotExists(roleRepository, "PLANT_USER", "Plant User role");
    createRoleIfNotExists(roleRepository, "CQS_USER", "CQS User role");
    createRoleIfNotExists(roleRepository, "TECH_USER", "Tech User role");

    // Get JVC_USER role for admin
    Role jvcRole = roleRepository.findByName("JVC_USER").orElse(null);

    // Check if admin user exists
    if (!userRepository.existsByUsername(adminUsername)) {
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEmail(adminEmail);
        admin.setStatus("ACTIVE");
        admin.setEnabled(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        
        // Add both ADMIN and JVC_USER roles
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        if (jvcRole != null) {
            roles.add(jvcRole);
        }
        admin.setRoles(roles);
        
        userRepository.save(admin);
        System.out.println("Default admin user created: username=admin, password=admin");
    } else {
        // Update existing admin user to have JVC_USER role if missing
        User existingAdmin = userRepository.findByUsername(adminUsername).orElse(null);
        if (existingAdmin != null && jvcRole != null) {
            boolean hasJvcRole = existingAdmin.getRoles().stream()
                .anyMatch(role -> "JVC_USER".equals(role.getName()));
            if (!hasJvcRole) {
                existingAdmin.getRoles().add(jvcRole);
                userRepository.save(existingAdmin);
                System.out.println("Added JVC_USER role to existing admin user");
            }
        }
    }

    // Create screen mappings for admin role
    List<String> DEFAULT_SCREENS = Arrays.asList(
        // Admin Panel and its sub-routes
        "/qrmfg/admin",
        "/qrmfg/admin/users",
        "/qrmfg/admin/roles",
        "/qrmfg/admin/screens",
        "/qrmfg/admin/sessions",
        "/qrmfg/admin/auditlogs",
        
        // Main routes
        "/qrmfg/dashboard",
        "/qrmfg/reports",
        "/qrmfg/settings",
        "/qrmfg/pendingtasks",
        "/qrmfg/systemdashboard",
        
        // View routes
        "/qrmfg/jvc",
        "/qrmfg/cqs",
        "/qrmfg/tech",
        "/qrmfg/plant",
        "/qrmfg/home",
        "/qrmfg"
    );

    for (String screen : DEFAULT_SCREENS) {
        List<ScreenRoleMapping> existingMappings = screenRoleMappingRepository.findByRoute(screen);
        boolean mappingExists = existingMappings.stream()
            .anyMatch(mapping -> mapping.getRole().equals(adminRole));

        if (!mappingExists) {
            ScreenRoleMapping mapping = new ScreenRoleMapping(screen, adminRole);
            screenRoleMappingRepository.save(mapping);
            System.out.println("Created screen mapping: " + screen + " -> ADMIN role");
        }
    }
}

private void createRoleIfNotExists(RoleRepository roleRepository, String roleName, String description) {
    roleRepository.findByName(roleName).orElseGet(() -> {
        Role r = new Role();
        r.setName(roleName);
        r.setDescription(description);
        r.setEnabled(true);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return roleRepository.save(r);
    });
}
}
