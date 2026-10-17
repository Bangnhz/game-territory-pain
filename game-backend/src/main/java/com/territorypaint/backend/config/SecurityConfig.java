package com.territorypaint.backend.config;

import com.territorypaint.backend.security.JwtAuthenticationFilter;
import com.territorypaint.backend.security.ServiceKeyFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 * Requirements: 1.9, 2.3, 9.4
 * 
 * Configures:
 * - Public endpoints: /api/auth/register, /api/auth/login
 * - Internal endpoints: /api/internal/**, /api/matches/save (authenticated with
 * X-Service-Key)
 * - Protected endpoints: /api/users/**, /api/matches/history (authenticated
 * with JWT)
 * - BCryptPasswordEncoder for password hashing
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ServiceKeyFilter serviceKeyFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
            ServiceKeyFilter serviceKeyFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.serviceKeyFilter = serviceKeyFilter;
    }

    /**
     * Configure BCryptPasswordEncoder.
     * Requirements: 1.1
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure SecurityFilterChain for HTTP security.
     * Requirements: 1.9, 2.3, 9.4
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless JWT authentication
                .csrf(csrf -> csrf.disable())

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                        // Internal endpoints - service key authentication required
                        .requestMatchers("/api/internal/**", "/api/matches/save").hasRole("SERVICE")

                        // Protected endpoints - JWT authentication required
                        .requestMatchers("/api/users/**", "/api/matches/history").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated())

                // Stateless session - don't create HTTP sessions
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Add custom filters
                // ServiceKeyFilter runs first to check X-Service-Key header
                .addFilterBefore(serviceKeyFilter, UsernamePasswordAuthenticationFilter.class)
                // JwtAuthenticationFilter runs second to check JWT tokens
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
