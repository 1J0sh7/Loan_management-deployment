package com.company.config;

import com.company.security.CustomAccessDeniedHandler;
import com.company.security.CustomAuthenticationEntryPoint;
import com.company.security.JwtAuthenticationFilter;
import com.company.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthFilter,
                          CustomAuthenticationEntryPoint authenticationEntryPoint,
                          CustomAccessDeniedHandler accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // ✅ CORS enabled
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // ==================== PUBLIC ENDPOINTS ====================
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // ==================== CUSTOMER ENDPOINTS ====================
                        .requestMatchers(HttpMethod.POST, "/api/v1/customers").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/{id}").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/customers/{id}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/customers/{id}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/customers/{id}").hasRole("ADMIN")

                        // ==================== LOAN PRODUCT ENDPOINTS ====================
                        .requestMatchers(HttpMethod.POST, "/api/v1/loan-products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/loan-products/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/loan-products/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/loan-products").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/loan-products/{id}").authenticated()

                        // ==================== LOAN APPLICATION ENDPOINTS ====================
                        .requestMatchers(HttpMethod.POST, "/api/v1/loan-applications").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/loan-applications/{id}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/loan-applications/customer/{customerId}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/loan-applications/admin/all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/loan-applications/{id}/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/loan-applications/{id}/reject").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/loan-applications/{id}/disburse").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/loan-applications/{id}/repayments").authenticated()

                        // ==================== ALL OTHER REQUESTS ====================
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
