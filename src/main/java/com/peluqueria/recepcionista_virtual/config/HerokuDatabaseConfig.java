package com.peluqueria.recepcionista_virtual.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class HerokuDatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource(Environment env) throws URISyntaxException {
        String databaseUrl = env.getProperty("DATABASE_URL");

        if (databaseUrl == null || databaseUrl.isEmpty()) {
            databaseUrl = System.getenv("DATABASE_URL");
        }

        if (databaseUrl == null || databaseUrl.isEmpty()) {
            throw new RuntimeException("DATABASE_URL not configured");
        }

        URI dbUri = new URI(databaseUrl);

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':'
                + dbUri.getPort() + dbUri.getPath()
                + "?sslmode=require&tcpKeepAlive=true&socketTimeout=30";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // Configuración para AWS RDS con migraciones
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(60000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        config.setLeakDetectionThreshold(60000);
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        // MUY IMPORTANTE para AWS RDS
        config.addDataSourceProperty("socketTimeout", "30");
        config.addDataSourceProperty("loginTimeout", "30");
        config.addDataSourceProperty("connectTimeout", "30");
        config.addDataSourceProperty("cancelSignalTimeout", "10");
        config.addDataSourceProperty("tcpKeepAlive", "true");

        // Para prevenir el cierre durante migraciones
        config.addDataSourceProperty("autoReconnect", "true");
        config.addDataSourceProperty("maxReconnects", "3");
        config.addDataSourceProperty("initialTimeout", "2");

        return new HikariDataSource(config);
    }
}