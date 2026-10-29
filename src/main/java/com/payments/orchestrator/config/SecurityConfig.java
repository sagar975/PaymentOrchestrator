package com.payments.orchestrator.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 * JWT-based stateless authentication.
 * Two-step login: credentials → OTP → JWT token.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401," + "\"error\":\"UNAUTHORIZED\"," + "\"message\":" + "\"Authentication required\"}");
        })).authorizeHttpRequests(auth -> auth
                // Public endpoints — no token needed
                .requestMatchers("/api/v1/merchants/register", "/api/v1/auth/login", "/api/v1/auth/verify-otp", "/webhooks/**").permitAll()
                // Dashboard — authenticated merchants only
                .requestMatchers("/api/v1/dashboard/**").authenticated()
                // Transactions — authenticated only
                .requestMatchers("/api/v1/transactions/**").authenticated()
                // Everything else — authenticated
                .anyRequest().authenticated()).addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}