package com.peluqueria.recepcionista_virtual.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.HttpMethod;
import com.peluqueria.recepcionista_virtual.security.JwtRequestFilter;
import com.peluqueria.recepcionista_virtual.security.JwtAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS directo - sin beans externos
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration configuration = new CorsConfiguration();

                    configuration.setAllowedOriginPatterns(Arrays.asList(
                            "https://hairai.netlify.app",
                            "https://*.netlify.app",
                            "http://localhost:*"
                    ));

                    configuration.setAllowedMethods(Arrays.asList(
                            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
                    ));

                    // CRITICO: Headers multi-tenant
                    configuration.setAllowedHeaders(Arrays.asList(
                            "Authorization", "Content-Type", "X-Requested-With", "Accept",
                            "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers",
                            "x-tenant-id", "X-Tenant-ID", "Cache-Control"
                    ));

                    configuration.setExposedHeaders(Arrays.asList(
                            "Authorization", "Content-Type", "x-tenant-id"
                    ));

                    configuration.setAllowCredentials(true);
                    configuration.setMaxAge(7200L);

                    return configuration;
                }))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/twilio/**").permitAll()
                        .requestMatchers("/health", "/actuator/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}