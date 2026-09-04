package com.company.security;

import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SecurityHandlerTest {
    @Test
    void authenticationEntryPointWrites401Json() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));
        new CustomAuthenticationEntryPoint().commence(request, response, new BadCredentialsException("bad"));
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        assertThat(output.toString()).contains("\"status\":401", "Unauthorized");
    }

    @Test
    void accessDeniedHandlerWrites403Json() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter output = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(output));
        new CustomAccessDeniedHandler().handle(request, response, new AccessDeniedException("no"));
        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        assertThat(output.toString()).contains("\"status\":403", "Forbidden");
    }
}
