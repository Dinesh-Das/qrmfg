package com.cqs.qrmfg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.service.ScreenRoleMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired
    private ScreenRoleMappingService screenRoleMappingService;
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
                .antMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .antMatchers("/api/v1/auth/**").permitAll()
                .antMatchers("/api/v1/admin/screen-role-mapping/my-screens").authenticated()
                .antMatchers("/api/v1/admin/**").authenticated()
                .antMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new ScreenRoleAccessFilter(screenRoleMappingService), FilterSecurityInterceptor.class);
        return http.build();
    }

    public static class ScreenRoleAccessFilter implements Filter {
        private final ScreenRoleMappingService screenRoleMappingService;
        public ScreenRoleAccessFilter(ScreenRoleMappingService screenRoleMappingService) {
            this.screenRoleMappingService = screenRoleMappingService;
        }
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;
            String path = req.getRequestURI();
            if (path.equals("/api/v1/admin/screen-role-mapping/my-screens")) {
                // Allow all authenticated users
                chain.doFilter(request, response);
                return;
            }
            if (path.startsWith("/api/v1/admin/")) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !(auth.getPrincipal() instanceof User)) {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                User user = (User) auth.getPrincipal();
                // Debug: print authorities
                logger.info("User '{}' authorities: {}", user.getUsername(), user.getAuthorities());
                // Always allow ADMIN role (case-insensitive, flexible)
                boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> {
                    String authority = a.getAuthority().toLowerCase();
                    return authority.equals("admin") || authority.equals("role_admin") || authority.startsWith("admin");
                });
                if (isAdmin) {
                    chain.doFilter(request, response);
                    return;
                }
                List<Long> roleIds = user.getRoles().stream().map(r -> r.getId()).collect(Collectors.toList());
                List<String> allowedRoutes = screenRoleMappingService.getAllowedRoutesForRoles(roleIds);
                // Remove /api/v1/admin prefix for matching
                String route = path.replaceFirst("^/api/v1/admin", "");
                if (!allowedRoutes.contains(route)) {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }
            chain.doFilter(request, response);
        }
    }
} 