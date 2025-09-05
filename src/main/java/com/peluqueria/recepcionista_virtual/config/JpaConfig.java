package com.peluqueria.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.peluqueria.recepcionista_virtual.repository")
@EntityScan(basePackages = "com.peluqueria.recepcionista_virtual.entity")
public class JpaConfig {
    // Esta configuración ayuda a Spring a encontrar las entidades más rápido
}