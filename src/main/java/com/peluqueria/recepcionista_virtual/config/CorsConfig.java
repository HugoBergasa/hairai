package com.peluqueria.recepcionista_virtual.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed.origins:https://hairai.netlify.app,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // SEGURIDAD: Origenes especificos desde variable de entorno
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // CRITICO: También permitir origins patterns para desarrollo
        configuration.setAllowedOriginPatterns(Arrays.asList("https://*.netlify.app", "http://localhost:*"));

        // SEGURIDAD: Metodos HTTP especificos necesarios
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // CRITICO: Headers multi-tenant - TODAS las variantes posibles
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-Tenant-ID",           // Formato principal
                "X-Tenant-Id",           // Variante camelCase
                "x-tenant-id",           // Variante lowercase
                "x-tenant-ID",           // Variante mixta
                "X-TENANT-ID",           // Variante uppercase
                "Cache-Control",
                "Pragma",
                "Expires"
        ));

        // Headers expuestos al cliente
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "X-Tenant-ID",
                "X-Tenant-Id",
                "x-tenant-id"
        ));

        // CRITICO: Permitir credenciales para JWT
        configuration.setAllowCredentials(true);

        // Cache conservador para preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}