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

        // SEGURIDAD: Orígenes específicos desde variable de entorno
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // CRÍTICO: También permitir origin patterns para desarrollo
        configuration.setAllowedOriginPatterns(Arrays.asList("https://*.netlify.app", "http://localhost:*"));

        // SEGURIDAD: Métodos HTTP específicos necesarios
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // CRÍTICO: Headers multi-tenant - SIMPLIFICADO para evitar conflictos
        configuration.addAllowedHeader("*"); // Temporal para debug

        // Headers expuestos al cliente
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "X-Tenant-ID",
                "Content-Type"
        ));

        // CRÍTICO: Permitir credenciales para JWT
        configuration.setAllowCredentials(true);

        // CRÍTICO: Cache más largo para preflight (reduce requests)
        configuration.setMaxAge(7200L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}