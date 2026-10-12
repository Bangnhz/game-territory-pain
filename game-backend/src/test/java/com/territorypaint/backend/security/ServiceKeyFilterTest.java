package com.territorypaint.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ServiceKeyFilter.
 * Tests service key authentication for internal API endpoints.
 */
@ExtendWith(MockitoExtension.class)
class ServiceKeyFilterTest {
    
    @Mock
    private FilterChain filterChain;
    
    private ServiceKeyFilter filter;
    private static final String SERVICE_KEY = "test-service-key";
    
    @BeforeEach
    void setUp() {
        filter = new ServiceKeyFilter(SERVICE_KEY);
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void doFilterInternal_shouldAuthenticateWithValidServiceKey_forInternalEndpoint() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/internal/sessions/validate");
        request.addHeader("X-Service-Key", SERVICE_KEY);
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("game-server", auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SERVICE")));
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_shouldAuthenticateWithValidServiceKey_forMatchesSaveEndpoint() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/matches/save");
        request.addHeader("X-Service-Key", SERVICE_KEY);
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("game-server", auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SERVICE")));
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_shouldRejectWithInvalidServiceKey() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/internal/sessions/validate");
        request.addHeader("X-Service-Key", "wrong-key");
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid or missing service key"));
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        
        verify(filterChain, never()).doFilter(any(), any());
    }
    
    @Test
    void doFilterInternal_shouldRejectWithMissingServiceKey() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/internal/sessions/validate");
        // No X-Service-Key header
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid or missing service key"));
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        
        verify(filterChain, never()).doFilter(any(), any());
    }
    
    @Test
    void doFilterInternal_shouldNotCheckServiceKey_forPublicEndpoints() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/auth/login");
        // No X-Service-Key header
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        assertEquals(200, response.getStatus());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_shouldNotCheckServiceKey_forUserEndpoints() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/users/profile");
        // No X-Service-Key header
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        assertEquals(200, response.getStatus());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_shouldRejectEmptyServiceKey() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/internal/sessions/validate");
        request.addHeader("X-Service-Key", "");
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        assertEquals(401, response.getStatus());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        
        verify(filterChain, never()).doFilter(any(), any());
    }
    
    @Test
    void doFilterInternal_shouldRejectWhitespaceServiceKey() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setRequestURI("/api/internal/sessions/validate");
        request.addHeader("X-Service-Key", "   ");
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        assertEquals(401, response.getStatus());
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        
        verify(filterChain, never()).doFilter(any(), any());
    }
}
