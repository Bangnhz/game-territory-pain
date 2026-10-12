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
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 * Tests JWT extraction, validation, and security context population.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    
    @Mock
    private JwtTokenProvider tokenProvider;
    
    @Mock
    private FilterChain filterChain;
    
    private JwtAuthenticationFilter filter;
    
    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenProvider);
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void doFilterInternal_shouldAuthenticateWithValidToken() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        
        Long userId = 123L;
        String username = "testuser";
        
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromToken(token)).thenReturn(userId);
        when(tokenProvider.getUsernameFromToken(token)).thenReturn(username);
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getPrincipal() instanceof JwtUser);
        
        JwtUser jwtUser = (JwtUser) auth.getPrincipal();
        assertEquals(userId, jwtUser.getUserId());
        assertEquals(username, jwtUser.getUsername());
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_shouldNotAuthenticateWithoutAuthorizationHeader() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        
        verify(tokenProvider, never()).validateToken(any());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_shouldNotAuthenticateWithInvalidToken() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String token = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        
        when(tokenProvider.validateToken(token)).thenReturn(false);
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        
        verify(tokenProvider).validateToken(token);
        verify(tokenProvider, never()).getUserIdFromToken(any());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_shouldNotAuthenticateWithMalformedHeader() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "InvalidFormat token");
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        
        verify(tokenProvider, never()).validateToken(any());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_shouldNotAuthenticateWithEmptyBearerToken() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer ");
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_shouldContinueFilterChainOnException() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        
        when(tokenProvider.validateToken(token)).thenThrow(new RuntimeException("Test exception"));
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void doFilterInternal_shouldHandleCaseSensitiveBearerPrefix() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "bearer token"); // lowercase
        
        // When
        filter.doFilterInternal(request, response, filterChain);
        
        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        
        verify(tokenProvider, never()).validateToken(any());
        verify(filterChain).doFilter(request, response);
    }
}
