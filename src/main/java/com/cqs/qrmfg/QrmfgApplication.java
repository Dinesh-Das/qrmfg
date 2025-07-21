// Legacy main application. New RBAC backend will be under com.cqs.qrmfg.rbac_app
package com.cqs.qrmfg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.Role;
import com.cqs.qrmfg.repository.UserRepository;
import com.cqs.qrmfg.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Collections;
import java.time.LocalDateTime;
import com.cqs.qrmfg.model.ScreenRoleMapping;
import com.cqs.qrmfg.repository.ScreenRoleMappingRepository;
import java.util.Arrays;
import java.util.List;

// Security configuration will be implemented in com.cqs.qrmfg.config according to Spring Security and JWT best practices.
@SpringBootApplication
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
        String adminUsername = "admin";
        String adminPassword = "admin";
        String adminEmail = "admin@example.com";
        String adminDisplayName = "Admin";

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
            admin.setRoles(Collections.singleton(adminRole));
            userRepository.save(admin);
            System.out.println("Default admin user created: username=admin, password=admin");
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
    };
}
}
