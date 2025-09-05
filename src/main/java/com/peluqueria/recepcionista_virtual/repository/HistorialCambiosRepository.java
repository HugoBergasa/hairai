package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.HistorialCambios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface HistorialCambiosRepository extends JpaRepository<HistorialCambios, Long> {

    List<HistorialCambios> findByTenantId(String tenantId);

    List<HistorialCambios> findByTenantIdAndEntidad(String tenantId, String entidad);

    List<HistorialCambios> findByTenantIdAndEntidadAndEntidadId(
            String tenantId, String entidad, Long entidadId
    );

    @Query("SELECT h FROM HistorialCambios h WHERE h.tenantId = :tenantId " +
            "AND h.timestamp BETWEEN :inicio AND :fin ORDER BY h.timestamp DESC")
    List<HistorialCambios> findCambiosPorPeriodo(
            @Param("tenantId") String tenantId,
            @Param("inicio") Instant inicio,
            @Param("fin") Instant fin
    );

    @Query("SELECT h FROM HistorialCambios h WHERE h.usuarioId = :usuarioId " +
            "ORDER BY h.timestamp DESC")
    List<HistorialCambios> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query(value = "SELECT entidad, accion, COUNT(*) as total " +
            "FROM historial_cambios WHERE tenant_id = :tenantId " +
            "GROUP BY entidad, accion",
            nativeQuery = true)
    List<Object[]> estadisticasAuditoria(@Param("tenantId") String tenantId);
}