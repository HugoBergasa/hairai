package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.LogLlamada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LogLlamadaRepository extends JpaRepository<LogLlamada, Long> {

    Optional<LogLlamada> findByCallSid(String callSid);

    List<LogLlamada> findByTenantId(String tenantId);

    List<LogLlamada> findByTenantIdAndEstado(String tenantId, String estado);

    Long countByTenantId(String tenantId);

    Long countByTenantIdAndConsentimientoRgpd(String tenantId, Boolean consentimiento);

    @Query("SELECT l FROM LogLlamada l WHERE l.tenantId = :tenantId " +
            "AND l.timestampInicio BETWEEN :inicio AND :fin")
    List<LogLlamada> findLlamadasPorPeriodo(
            @Param("tenantId") String tenantId,
            @Param("inicio") Instant inicio,
            @Param("fin") Instant fin
    );

    @Query("SELECT SUM(l.duracion) FROM LogLlamada l WHERE l.tenantId = :tenantId " +
            "AND l.timestampInicio >= :desde")
    Long sumDuracionTotal(
            @Param("tenantId") String tenantId,
            @Param("desde") Instant desde
    );

    @Query("SELECT SUM(l.costeLlamada) FROM LogLlamada l WHERE l.tenantId = :tenantId " +
            "AND l.timestampInicio >= :desde")
    Double sumCostoTotal(
            @Param("tenantId") String tenantId,
            @Param("desde") Instant desde
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE logs_llamadas SET numero_origen = 'ANONIMIZADO', " +
            "numero_destino = 'ANONIMIZADO', grabacion_url = NULL " +
            "WHERE tenant_id = :tenantId AND timestamp_inicio < :fechaLimite",
            nativeQuery = true)
    int anonimizarLlamadasAntiguas(
            @Param("tenantId") String tenantId,
            @Param("fechaLimite") LocalDateTime fechaLimite
    );

    // Para estadísticas del dashboard
    @Query(value = "SELECT DATE(timestamp_inicio) as fecha, COUNT(*) as total, " +
            "AVG(duracion) as duracion_promedio " +
            "FROM logs_llamadas WHERE tenant_id = :tenantId " +
            "AND timestamp_inicio >= :desde " +
            "GROUP BY DATE(timestamp_inicio) ORDER BY fecha DESC",
            nativeQuery = true)
    List<Object[]> estadisticasDiarias(
            @Param("tenantId") String tenantId,
            @Param("desde") Instant desde
    );
}