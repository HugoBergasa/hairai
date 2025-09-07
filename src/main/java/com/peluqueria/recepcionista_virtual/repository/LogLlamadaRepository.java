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
public interface LogLlamadaRepository extends JpaRepository<LogLlamada, String> {  // CORREGIDO: String ID

    // ===== MÉTODOS BÁSICOS MULTITENANT =====

    /**
     * Buscar llamada por CallSid (sin restricción de tenant)
     */
    Optional<LogLlamada> findByCallSid(String callSid);

    /**
     * Buscar llamada por CallSid y TenantId (seguridad multitenant)
     */
    LogLlamada findByCallSidAndTenantId(String callSid, String tenantId);

    /**
     * Buscar llamada por ID y TenantId (seguridad multitenant) - CORREGIDO: String ID
     */
    LogLlamada findByIdAndTenantId(String id, String tenantId);

    /**
     * Obtener todas las llamadas de un tenant
     */
    List<LogLlamada> findByTenantId(String tenantId);

    /**
     * Obtener llamadas de un tenant ordenadas por fecha (más recientes primero)
     */
    List<LogLlamada> findByTenantIdOrderByFechaInicioDesc(String tenantId);

    /**
     * Obtener llamadas de un tenant con paginación
     */
    Page<LogLlamada> findByTenantIdOrderByFechaInicioDesc(String tenantId, Pageable pageable);

    // ===== MÉTODOS POR ESTADO - CORREGIDO: usar String =====

    /**
     * Buscar llamadas por tenant y estado - CORREGIDO: String estado
     */
    List<LogLlamada> findByTenantIdAndEstadoOrderByFechaInicioDesc(String tenantId, String estado);

    /**
     * Buscar llamadas por múltiples estados - CORREGIDO: String estados
     */
    List<LogLlamada> findByTenantIdAndEstadoInOrderByFechaInicioDesc(
            String tenantId,
            List<String> estados
    );

    // ===== MÉTODOS POR DIRECCIÓN - CORREGIDO: usar String =====

    /**
     * Buscar llamadas por tenant y dirección - CORREGIDO: String direccion
     */
    List<LogLlamada> findByTenantIdAndDireccionOrderByFechaInicioDesc(String tenantId, String direccion);

    // ===== MÉTODOS POR RANGO DE FECHAS =====

    /**
     * Buscar llamadas en un rango de fechas
     */
    List<LogLlamada> findByTenantIdAndFechaInicioBetweenOrderByFechaInicioDesc(
            String tenantId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );

    /**
     * Query personalizada para llamadas por período
     */
    @Query("SELECT l FROM LogLlamada l WHERE l.tenantId = :tenantId " +
            "AND l.fechaInicio BETWEEN :fechaInicio AND :fechaFin " +
            "ORDER BY l.fechaInicio DESC")
    List<LogLlamada> findLlamadasPorPeriodo(
            @Param("tenantId") String tenantId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    // ===== MÉTODOS POR NÚMERO DE ORIGEN =====

    /**
     * Buscar llamadas por número de origen
     */
    List<LogLlamada> findByTenantIdAndNumeroOrigenOrderByFechaInicioDesc(String tenantId, String numeroOrigen);

    /**
     * Query personalizada para llamadas de un cliente específico
     */
    @Query("SELECT l FROM LogLlamada l WHERE l.tenantId = :tenantId " +
            "AND l.numeroOrigen = :numeroOrigen ORDER BY l.fechaInicio DESC")
    List<LogLlamada> findLlamadasDeCliente(
            @Param("tenantId") String tenantId,
            @Param("numeroOrigen") String numeroOrigen
    );

    // ===== MÉTODOS DE CONTEO - CORREGIDO: usar String =====

    /**
     * Contar total de llamadas por tenant
     */
    Long countByTenantId(String tenantId);

    /**
     * Contar llamadas por dirección - CORREGIDO: String direccion
     */
    Long countByTenantIdAndDireccion(String tenantId, String direccion);

    /**
     * Contar llamadas por estado - CORREGIDO: String estado
     */
    Long countByTenantIdAndEstado(String tenantId, String estado);

    /**
     * Contar llamadas que generaron citas
     */
    Long countByTenantIdAndCitaCreadaIdIsNotNull(String tenantId);

    /**
     * Contar llamadas por número de origen
     */
    Long countByTenantIdAndNumeroOrigen(String tenantId, String numeroOrigen);

    // ===== MÉTODOS DE ESTADÍSTICAS AVANZADAS =====

    /**
     * Obtener estadísticas generales de llamadas
     */
    @Query("SELECT COUNT(l), SUM(l.duracionSegundos), AVG(l.duracionSegundos) " +
            "FROM LogLlamada l WHERE l.tenantId = :tenantId " +
            "AND l.fechaInicio >= :desde")
    Object[] obtenerEstadisticas(
            @Param("tenantId") String tenantId,
            @Param("desde") LocalDateTime desde
    );

    /**
     * Sumar duración total de llamadas por tenant
     */
    @Query("SELECT COALESCE(SUM(l.duracionSegundos), 0) FROM LogLlamada l " +
            "WHERE l.tenantId = :tenantId AND l.duracionSegundos IS NOT NULL")
    Long sumDuracionByTenantId(@Param("tenantId") String tenantId);

    /**
     * Obtener costo total de llamadas por tenant
     */
    @Query("SELECT COALESCE(SUM(l.costo), 0) FROM LogLlamada l " +
            "WHERE l.tenantId = :tenantId AND l.costo IS NOT NULL")
    java.math.BigDecimal sumCostoByTenantId(@Param("tenantId") String tenantId);

    // ===== MÉTODOS ADICIONALES ÚTILES =====

    /**
     * Verificar si existe una llamada con CallSid específico para un tenant
     */
    boolean existsByCallSidAndTenantId(String callSid, String tenantId);

    /**
     * Obtener las 10 llamadas más recientes de un tenant
     */
    List<LogLlamada> findFirst10ByTenantIdOrderByFechaInicioDesc(String tenantId);

    /**
     * Buscar llamadas por cliente (usando clienteId)
     */
    List<LogLlamada> findByTenantIdAndClienteIdOrderByFechaInicioDesc(String tenantId, String clienteId);

    /**
     * Buscar llamadas con transcripción disponible
     */
    List<LogLlamada> findByTenantIdAndTranscripcionIsNotNullOrderByFechaInicioDesc(String tenantId);

    /**
     * Buscar llamadas con grabación disponible
     */
    List<LogLlamada> findByTenantIdAndGrabacionUrlIsNotNullOrderByFechaInicioDesc(String tenantId);

    /**
     * Buscar llamadas por empleado responsable
     */
    List<LogLlamada> findByTenantIdAndEmpleadoIdOrderByFechaInicioDesc(String tenantId, String empleadoId);

    // ===== QUERIES PARA DASHBOARD Y ANALÍTICAS =====

    /**
     * Obtener llamadas del día actual - CORREGIDO para PostgreSQL
     */
    @Query("SELECT l FROM LogLlamada l WHERE l.tenantId = :tenantId " +
            "AND l.fechaInicio >= :inicioDelDia AND l.fechaInicio < :finDelDia " +
            "ORDER BY l.fechaInicio DESC")
    List<LogLlamada> findLlamadasDelDia(
            @Param("tenantId") String tenantId,
            @Param("inicioDelDia") LocalDateTime inicioDelDia,
            @Param("finDelDia") LocalDateTime finDelDia
    );

    /**
     * Obtener llamadas de la semana actual - CORREGIDO
     */
    @Query("SELECT l FROM LogLlamada l WHERE l.tenantId = :tenantId " +
            "AND l.fechaInicio >= :inicioSemana ORDER BY l.fechaInicio DESC")
    List<LogLlamada> findLlamadasDeLaSemana(
            @Param("tenantId") String tenantId,
            @Param("inicioSemana") LocalDateTime inicioSemana
    );

    /**
     * Obtener estadísticas por estado para un período
     */
    @Query("SELECT l.estado, COUNT(l) FROM LogLlamada l " +
            "WHERE l.tenantId = :tenantId " +
            "AND l.fechaInicio BETWEEN :fechaInicio AND :fechaFin " +
            "GROUP BY l.estado")
    List<Object[]> getEstadisticasPorEstado(
            @Param("tenantId") String tenantId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Obtener estadísticas por dirección para un período
     */
    @Query("SELECT l.direccion, COUNT(l) FROM LogLlamada l " +
            "WHERE l.tenantId = :tenantId " +
            "AND l.fechaInicio BETWEEN :fechaInicio AND :fechaFin " +
            "GROUP BY l.direccion")
    List<Object[]> getEstadisticasPorDireccion(
            @Param("tenantId") String tenantId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );
}