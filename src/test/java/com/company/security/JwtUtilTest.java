package com.company.security;

import com.company.model.Role;
import com.company.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        Field secret = JwtUtil.class.getDeclaredField("secret");
        secret.setAccessible(true);
        secret.set(jwtUtil, "test-secret-that-is-at-least-32-characters-long");
        Field expiration = JwtUtil.class.getDeclaredField("expiration");
        expiration.setAccessible(true);
        expiration.set(jwtUtil, 60_000L);
    }

    @Test
    void generatedTokenContainsUsernameAndValidates() {
        String token = jwtUtil.generateToken("alice");
        UserDetails user = new User("alice", "password", Role.USER);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtUtil.extractExpiration(token)).isAfter(java.util.Date.from(java.time.Instant.now()));
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
        assertThat(jwtUtil.validateToken(token, user)).isTrue();
        assertThat(jwtUtil.validateToken(token, new User("bob", "password", Role.USER))).isFalse();
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> jwtUtil.extractUsername("not-a-jwt")).isInstanceOf(RuntimeException.class);
    }
}
