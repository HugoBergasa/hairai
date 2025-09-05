package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface ServicioRepository extends JpaRepository<Servicio, String> {
    List<Servicio> findByTenantIdAndActivoTrue(String tenantId);

    @Query(value = "SELECT s.id, s.nombre, s.descripcion, s.duracion, s.precio, s.activo " +
            "FROM servicios s " +
            "WHERE s.tenant_id = :tenantId AND s.activo = true",
            nativeQuery = true)
    List<Map<String, Object>> findServiciosByTenantId(@Param("tenantId") String tenantId);
}
