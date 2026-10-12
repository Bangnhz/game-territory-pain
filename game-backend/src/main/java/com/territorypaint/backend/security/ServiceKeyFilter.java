package com.territorypaint.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Service key authentication filter for Game Server requests.
 * Requirements: 9.4
 * 
 * Authenticates internal service-to-service requests using X-Service-Key header.
 * This is used by the Game Server to call internal Backend API endpoints.
 */
@Component
public class ServiceKeyFilter extends OncePerRequestFilter {
    
    private final String serviceKey;
    
    public ServiceKeyFilter(@Value("${app.service.key}") String serviceKey) {
        this.serviceKey = serviceKey;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Only check service key for internal API endpoints
        String requestUri = request.getRequestURI();
        
        if (requestUri.startsWith("/api/internal/") || requestUri.startsWith("/api/matches/save")) {
            String providedKey = request.getHeader("X-Service-Key");
            
            if (StringUtils.hasText(providedKey) && providedKey.equals(serviceKey)) {
                // Valid service key - set authentication with SERVICE role
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                "game-server",
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE"))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // Invalid or missing service key for internal endpoint
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid or missing service key\"}");
                response.setContentType("application/json");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
