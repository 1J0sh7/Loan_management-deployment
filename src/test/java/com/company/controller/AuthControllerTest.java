package com.company.controller;

import com.company.dto.*;
import com.company.model.Role;
import com.company.model.User;
import com.company.repository.UserRepository;
import com.company.security.JwtUtil;
import com.company.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock AuthenticationManager authenticationManager;
    @Mock CustomUserDetailsService userDetailsService;
    @Mock JwtUtil jwtUtil;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthController controller;

    @Test
    void login_returnsTokenAndRoleOnSuccess() {
        User user = new User("alice", "encoded", Role.USER);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);
        when(jwtUtil.generateToken("alice")).thenReturn("token");
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("password");

        ResponseEntity<?> response = controller.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        AuthResponse body = (AuthResponse) response.getBody();
        assertThat(body.getToken()).isEqualTo("token");
        assertThat(body.getUsername()).isEqualTo("alice");
        assertThat(body.getRole()).isEqualTo("ROLE_USER");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_mapsAuthenticationFailuresToUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("bad");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        ResponseEntity<?> response = controller.login(request);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("Invalid username or password");

        reset(authenticationManager);
        when(authenticationManager.authenticate(any())).thenThrow(new UsernameNotFoundException("missing"));
        response = controller.login(request);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("User is not found in the system.");
    }

    @Test
    void register_rejectsDuplicateAndMismatchedPasswords() {
        RegisterRequest request = registration("alice", "one", "one", null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new User()));
        ResponseEntity<String> response = controller.register(request);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Username already exists");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        request.setConfirmPassword("two");
        response = controller.register(request);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Passwords do not match");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_defaultsToUserAndHonorsAdminRole() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        RegisterRequest request = registration("alice", "secret", "secret", null);
        assertThat(controller.register(request).getStatusCode().value()).isEqualTo(201);
        verify(userRepository).save(argThat(u -> u.getRole() == Role.USER
                && u.getPassword().equals("encoded")));

        request = registration("admin", "secret", "secret", "admin");
        assertThat(controller.register(request).getStatusCode().value()).isEqualTo(201);
        verify(userRepository).save(argThat(u -> u.getRole() == Role.ADMIN));
    }

    private RegisterRequest registration(String username, String password, String confirm, String role) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setConfirmPassword(confirm);
        request.setRole(role);
        return request;
    }
}
