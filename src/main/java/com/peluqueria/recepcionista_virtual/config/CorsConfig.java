package com.peluqueria.recepcionista_virtual.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // SOLO allowedOriginPatterns (más flexible)
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://hairai.netlify.app",
                "https://*.netlify.app",
                "http://localhost:*"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // CRÍTICO: Headers multi-tenant
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

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}