package com.company.config;

import com.company.security.*;
import com.company.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {
    @Test
    void passwordEncoderUsesOneWayHashing() {
        SecurityConfig config = new SecurityConfig(mock(CustomUserDetailsService.class),
                mock(JwtAuthenticationFilter.class), mock(CustomAuthenticationEntryPoint.class),
                mock(CustomAccessDeniedHandler.class));
        PasswordEncoder encoder = config.passwordEncoder();
        String encoded = encoder.encode("secret");
        assertThat(encoded).isNotEqualTo("secret");
        assertThat(encoder.matches("secret", encoded)).isTrue();
        assertThat(encoder.matches("wrong", encoded)).isFalse();
    }
}
