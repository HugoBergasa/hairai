package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.ConfiguracionTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ConfiguracionTenantRepository extends JpaRepository<ConfiguracionTenant, Long> {

    List<ConfiguracionTenant> findByTenantId(String tenantId);

    Optional<ConfiguracionTenant> findByTenantIdAndClave(String tenantId, String clave);

    List<ConfiguracionTenant> findByTenantIdAndCategoria(String tenantId, String categoria);

    @Query("SELECT c FROM ConfiguracionTenant c WHERE c.tenantId = :tenantId AND c.editable = true")
    List<ConfiguracionTenant> findConfiguracionesEditables(@Param("tenantId") String tenantId);

    @Query("SELECT c.clave, c.valor FROM ConfiguracionTenant c WHERE c.tenantId = :tenantId")
    List<Object[]> obtenerConfiguracionComoMapa(@Param("tenantId") String tenantId);

    // Método útil para obtener valor específico
    default String obtenerValor(String tenantId, String clave, String valorPorDefecto) {
        Optional<ConfiguracionTenant> config = findByTenantIdAndClave(tenantId, clave);
        return config.map(ConfiguracionTenant::getValor).orElse(valorPorDefecto);
    }
}