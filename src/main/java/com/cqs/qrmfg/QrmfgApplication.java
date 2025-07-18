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
			PasswordEncoder passwordEncoder) {
		return args -> {
			String adminUsername = "admin";
			String adminPassword = "admin";
			String adminEmail = "admin@example.com";
			String adminDisplayName = "Admin";
			// Check if admin user exists
			if (!userRepository.existsByUsername(adminUsername)) {
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
		};
	}
}
