package com.peluqueria.recepcionista_virtual.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed.origins:https://hairai.netlify.app,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // SEGURIDAD: Orígenes específicos desde variable de entorno
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        // SEGURIDAD: Métodos HTTP específicos necesarios
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // SEGURIDAD MEJORADA: Headers específicos en lugar de "*"
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-Tenant-ID",           // ← CRÍTICO para multi-tenant
                "X-Tenant-Id",           // ← Variantes del header
                "x-tenant-id"            // ← Case insensitive
        ));

        // Headers expuestos al cliente
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "X-Tenant-ID"
        ));

        // Permitir credenciales para JWT
        configuration.setAllowCredentials(true);

        // Cache conservador (1 hora es más seguro que 24 horas)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}