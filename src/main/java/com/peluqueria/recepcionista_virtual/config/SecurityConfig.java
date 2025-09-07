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
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.HttpMethod;
import com.peluqueria.recepcionista_virtual.security.JwtRequestFilter;
import com.peluqueria.recepcionista_virtual.security.JwtAuthenticationEntryPoint;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private Environment env;

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
                // CRÍTICO: CORS inline sin hardcoding - usa variables de entorno
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();

                    // SEGURIDAD: Orígenes desde variable de entorno (sin hardcoding)
                    String allowedOrigins = env.getProperty("cors.allowed.origins",
                            "https://hairai.netlify.app,http://localhost:3000");
                    config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

                    // DESARROLLO: Patterns seguros para subdominios
                    config.setAllowedOriginPatterns(Arrays.asList("https://*.netlify.app", "http://localhost:*"));

                    // SEGURIDAD: Métodos HTTP específicos necesarios
                    config.setAllowedMethods(Arrays.asList(
                            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
                    ));

                    // MULTI-TENANT: Headers específicos sin comprometer seguridad
                    config.setAllowedHeaders(Arrays.asList(
                            "Authorization",
                            "Content-Type",
                            "x-tenant-id",          // Header multi-tenant crítico
                            "X-Tenant-ID",          // Variante capitalizada
                            "Accept",
                            "Origin",
                            "X-Requested-With",
                            "Cache-Control"
                    ));

                    // Headers expuestos al cliente
                    config.setExposedHeaders(Arrays.asList(
                            "Authorization",
                            "x-tenant-id",
                            "Content-Type"
                    ));

                    // JWT: Permitir credenciales
                    config.setAllowCredentials(true);

                    // Cache preflight conservador
                    config.setMaxAge(3600L);

                    return config;
                }))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(authz -> authz
                        // CRÍTICO: OPTIONS para CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Endpoints públicos
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/twilio/**").permitAll()
                        .requestMatchers("/health", "/actuator/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        // MULTI-TENANT: Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // JWT Filter después de CORS
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}