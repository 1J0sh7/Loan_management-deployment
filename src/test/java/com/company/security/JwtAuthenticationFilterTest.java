package com.company.security;

import com.company.model.Role;
import com.company.model.User;
import com.company.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock JwtUtil jwtUtil;
    @Mock CustomUserDetailsService detailsService;
    @Mock FilterChain chain;

    private JwtAuthenticationFilter filter() {
        return new JwtAuthenticationFilter(jwtUtil, detailsService);
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void requestWithoutBearerTokenContinuesUnchanged() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        filter().doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtUtil, detailsService);
    }

    @Test
    void validBearerTokenSetsAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        User user = new User("alice", "password", Role.USER);
        when(request.getHeader("Authorization")).thenReturn("Bearer abc");
        when(jwtUtil.extractUsername("abc")).thenReturn("alice");
        when(detailsService.loadUserByUsername("alice")).thenReturn(user);
        when(jwtUtil.validateToken("abc", user)).thenReturn(true);

        filter().doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidTokenDoesNotAuthenticate() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        User user = new User("alice", "password", Role.USER);
        when(request.getHeader("Authorization")).thenReturn("Bearer abc");
        when(jwtUtil.extractUsername("abc")).thenReturn("alice");
        when(detailsService.loadUserByUsername("alice")).thenReturn(user);
        when(jwtUtil.validateToken("abc", user)).thenReturn(false);

        filter().doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
