package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.ConfiguracionTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracionTenantRepository extends JpaRepository<ConfiguracionTenant, Long> {

    Optional<ConfiguracionTenant> findByTenantIdAndClave(String tenantId, String clave);

    List<ConfiguracionTenant> findByTenantId(String tenantId);

    List<ConfiguracionTenant> findByTenantIdAndCategoria(String tenantId, String categoria);

    @Query("SELECT c FROM ConfiguracionTenant c WHERE c.tenantId = :tenantId " +
            "AND c.clave LIKE :clavePrefix%")
    List<ConfiguracionTenant> findByTenantIdAndClaveStartingWith(
            @Param("tenantId") String tenantId,
            @Param("clavePrefix") String clavePrefix
    );

    Optional<ConfiguracionTenant> findByClaveAndValor(String clave, String valor);

    @Query("SELECT c FROM ConfiguracionTenant c WHERE c.categoria = :categoria " +
            "AND c.esSensible = false")
    List<ConfiguracionTenant> findConfiguracionesPublicas(@Param("categoria") String categoria);

    @Query("SELECT DISTINCT c.tenantId FROM ConfiguracionTenant c " +
            "WHERE c.clave = 'forwarding.enabled' AND c.valor = 'true'")
    List<String> findTenantsConForwarding();

    void deleteByTenantIdAndClave(String tenantId, String clave);

    @Modifying
    @Query("UPDATE ConfiguracionTenant c SET c.valor = :valor " +
            "WHERE c.tenantId = :tenantId AND c.clave = :clave")
    int actualizarValor(
            @Param("tenantId") String tenantId,
            @Param("clave") String clave,
            @Param("valor") String valor
    );
}