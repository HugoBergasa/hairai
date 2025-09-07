package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.LogLlamada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LogLlamadaRepository extends JpaRepository<LogLlamada, Long> {

    // ===== MÉTODOS EXISTENTES =====
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

    // ===== MÉTODOS AGREGADOS BÁSICOS (SIN QUERIES PROBLEMÁTICAS) =====

    // Métodos con ordenamiento
    List<LogLlamada> findByTenantIdOrderByFechaInicioDesc(String tenantId);

    Page<LogLlamada> findByTenantIdOrderByFechaInicioDesc(String tenantId, Pageable pageable);

    // Buscar por CallSid y TenantId (seguridad multitenant)
    LogLlamada findByCallSidAndTenantId(String callSid, String tenantId);

    // Buscar por ID y TenantId (seguridad multitenant)
    LogLlamada findByIdAndTenantId(Long id, String tenantId);

    // Métodos con estado y ordenamiento
    List<LogLlamada> findByTenantIdAndEstadoOrderByFechaInicioDesc(String tenantId, LogLlamada.EstadoLlamada estado);

    // Métodos con dirección
    List<LogLlamada> findByTenantIdAndDireccionOrderByFechaInicioDesc(String tenantId, LogLlamada.DireccionLlamada direccion);

    // Métodos con rango de fechas
    List<LogLlamada> findByTenantIdAndFechaInicioBetweenOrderByFechaInicioDesc(
            String tenantId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );

    // Métodos con número de origen
    List<LogLlamada> findByTenantIdAndNumeroOrigenOrderByFechaInicioDesc(String tenantId, String numeroOrigen);

    // ===== MÉTODOS DE CONTEO BÁSICOS =====

    Long countByTenantId(String tenantId);

    Long countByTenantIdAndDireccion(String tenantId, LogLlamada.DireccionLlamada direccion);

    Long countByTenantIdAndEstado(String tenantId, LogLlamada.EstadoLlamada estado);

    Long countByTenantIdAndCitaCreadaIdIsNotNull(String tenantId);

    // ===== MÉTODOS ADICIONALES SEGUROS =====

    boolean existsByCallSidAndTenantId(String callSid, String tenantId);

    List<LogLlamada> findFirst10ByTenantIdOrderByFechaInicioDesc(String tenantId);

    List<LogLlamada> findByTenantIdAndEstadoInOrderByFechaInicioDesc(
            String tenantId,
            List<LogLlamada.EstadoLlamada> estados
    );



    @Query("SELECT COALESCE(SUM(l.duracionSegundos), 0) FROM LogLlamada l " +
            "WHERE l.tenantId = :tenantId AND l.duracionSegundos IS NOT NULL")
    Long sumDuracionByTenantId(@Param("tenantId") String tenantId);

    Long countByTenantIdAndNumeroOrigen(String tenantId, String numeroOrigen);
}