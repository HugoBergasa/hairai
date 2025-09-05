package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.LogLlamada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LogLlamadaRepository extends JpaRepository<LogLlamada, Long> {

    Optional<LogLlamada> findByCallSid(String callSid);

    List<LogLlamada> findByTenantId(String tenantId);

    List<LogLlamada> findByTenantIdAndEstado(String tenantId, LogLlamada.EstadoLlamada estado);

    @Query("SELECT l FROM LogLlamada l WHERE l.tenantId = :tenantId " +
            "AND l.fechaInicio BETWEEN :fechaInicio AND :fechaFin")
    List<LogLlamada> findLlamadasPorPeriodo(
            @Param("tenantId") String tenantId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    @Query("SELECT COUNT(l), SUM(l.duracionSegundos), AVG(l.duracionSegundos) " +
            "FROM LogLlamada l WHERE l.tenantId = :tenantId " +
            "AND l.fechaInicio >= :desde")
    Object[] obtenerEstadisticas(
            @Param("tenantId") String tenantId,
            @Param("desde") LocalDateTime desde
    );

    @Query("SELECT l FROM LogLlamada l WHERE l.tenantId = :tenantId " +
            "AND l.numeroOrigen = :numeroOrigen ORDER BY l.fechaInicio DESC")
    List<LogLlamada> findLlamadasDeCliente(
            @Param("tenantId") String tenantId,
            @Param("numeroOrigen") String numeroOrigen
    );
}