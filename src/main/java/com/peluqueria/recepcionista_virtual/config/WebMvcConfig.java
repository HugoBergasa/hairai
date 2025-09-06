package com.peluqueria.recepcionista_virtual.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Comentar temporalmente hasta definir filtros Hibernate
        // registry.addInterceptor(tenantInterceptor)
        //         .addPathPatterns("/api/**")
        //         .excludePathPatterns("/api/auth/**", "/api/twilio/**");
    }
}