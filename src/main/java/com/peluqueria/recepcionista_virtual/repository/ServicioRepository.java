package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.Servicio;
import com.peluqueria.recepcionista_virtual.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, String> { // ✅ CORREGIDO: String en lugar de Long

    // Buscar servicios por tenant
    List<Servicio> findByTenant(Tenant tenant);

    // Buscar servicios activos por tenant
    List<Servicio> findByTenantAndActivo(Tenant tenant, Boolean activo);

    // Buscar servicio por nombre y tenant
    Servicio findByNombreAndTenant(String nombre, Tenant tenant);

    /**
     * ✅ MÉTODO CLAVE - Encuentra servicios por tenant como mapas para el prompt de IA
     * Este método será usado por OpenAIService para construir prompts dinámicos
     */
    @Query(value = "SELECT s.id, s.nombre, s.descripcion, s.duracion_minutos, s.precio, s.activo " +
            "FROM servicios s " +
            "WHERE s.tenant_id = :tenantId AND s.activo = true",
            nativeQuery = true)
    List<Map<String, Object>> findServiciosByTenantId(@Param("tenantId") String tenantId);

    /**
     * ✅ MÉTODO ALTERNATIVO - JPQL más limpio
     * Será usado por OpenAIService para obtener servicios estructurados
     */
    @Query("SELECT s FROM Servicio s WHERE s.tenant.id = :tenantId AND s.activo = true")
    List<Servicio> findActivosByTenantId(@Param("tenantId") String tenantId);

    /**
     * Contar servicios activos por tenant
     */
    @Query("SELECT COUNT(s) FROM Servicio s WHERE s.tenant.id = :tenantId AND s.activo = true")
    Long countActivosByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT s FROM Servicio s WHERE LOWER(s.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) AND s.tenant.id = :tenantId")
    List<Servicio> findByNombreContainingIgnoreCaseAndTenantId(String nombre, String tenantId);
}