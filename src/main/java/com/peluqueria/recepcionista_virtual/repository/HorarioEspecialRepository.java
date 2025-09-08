package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.HorarioEspecial;
import com.peluqueria.recepcionista_virtual.model.TipoCierre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository para gestionar horarios especiales y cierres del salon
 *
 * MULTITENANT: TODAS las queries incluyen tenant_id - aislamiento perfecto
 * ZERO HARDCODING: Solo consultas tecnicas, sin textos fijos
 * OpenAI CEREBRO: Consultas optimizadas para verificacion en tiempo real de la IA
 */
@Repository
public interface HorarioEspecialRepository extends JpaRepository<HorarioEspecial, String> {

    /**
     * QUERY MAS IMPORTANTE: Verificar si hay cierres para una fecha especifica
     * La IA llama esta consulta en CADA solicitud de cita antes de responder
     */
    @Query("SELECT h FROM HorarioEspecial h WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND :fecha BETWEEN h.fechaInicio AND h.fechaFin " +
            "ORDER BY h.tipoCierre")
    List<HorarioEspecial> findCierresParaFecha(@Param("tenantId") String tenantId,
                                               @Param("fecha") LocalDate fecha);

    /**
     * Obtener todos los cierres en un rango de fechas
     */
    @Query("SELECT h FROM HorarioEspecial h WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND ((h.fechaInicio BETWEEN :inicio AND :fin) OR " +
            "(h.fechaFin BETWEEN :inicio AND :fin) OR " +
            "(h.fechaInicio <= :inicio AND h.fechaFin >= :fin)) " +
            "ORDER BY h.fechaInicio, h.tipoCierre")
    List<HorarioEspecial> findCierresEnRango(@Param("tenantId") String tenantId,
                                             @Param("inicio") LocalDate inicio,
                                             @Param("fin") LocalDate fin);

    /**
     * Obtener cierres proximos
     */
    @Query("SELECT h FROM HorarioEspecial h WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND h.fechaInicio >= CURRENT_DATE " +
            "AND h.fechaInicio <= :fechaLimite " +
            "ORDER BY h.fechaInicio")
    List<HorarioEspecial> findCierresProximos(@Param("tenantId") String tenantId,
                                              @Param("fechaLimite") LocalDate fechaLimite);

    /**
     * Verificar disponibilidad de empleado especifico
     */
    @Query("SELECT h FROM HorarioEspecial h WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND :fecha BETWEEN h.fechaInicio AND h.fechaFin " +
            "AND (h.tipoCierre = 'EMPLEADO_AUSENTE' OR h.tipoCierre = 'CERRADO_COMPLETO') " +
            "AND (h.empleadosAfectados IS NULL OR h.empleadosAfectados LIKE %:empleadoId%)")
    List<HorarioEspecial> findCierresParaEmpleado(@Param("tenantId") String tenantId,
                                                  @Param("fecha") LocalDate fecha,
                                                  @Param("empleadoId") String empleadoId);

    /**
     * Verificar disponibilidad de servicio especifico
     */
    @Query("SELECT h FROM HorarioEspecial h WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND :fecha BETWEEN h.fechaInicio AND h.fechaFin " +
            "AND (h.tipoCierre = 'SERVICIO_NO_DISPONIBLE' OR h.tipoCierre = 'CERRADO_COMPLETO') " +
            "AND (h.serviciosAfectados IS NULL OR h.serviciosAfectados LIKE %:servicioId%)")
    List<HorarioEspecial> findCierresParaServicio(@Param("tenantId") String tenantId,
                                                  @Param("fecha") LocalDate fecha,
                                                  @Param("servicioId") String servicioId);

    /**
     * Contar cierres por tipo para estadisticas
     */
    @Query("SELECT h.tipoCierre, COUNT(h) FROM HorarioEspecial h " +
            "WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND h.fechaInicio >= :desde " +
            "AND h.fechaFin <= :hasta " +
            "GROUP BY h.tipoCierre")
    List<Object[]> countCierresPorTipo(@Param("tenantId") String tenantId,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);

    /**
     * Obtener estadisticas de cierres por mes
     */
    @Query("SELECT EXTRACT(MONTH FROM h.fechaInicio) as mes, " +
            "EXTRACT(YEAR FROM h.fechaInicio) as ano, " +
            "COUNT(h) as total, " +
            "h.tipoCierre " +
            "FROM HorarioEspecial h " +
            "WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND h.fechaInicio >= :desde " +
            "GROUP BY EXTRACT(YEAR FROM h.fechaInicio), EXTRACT(MONTH FROM h.fechaInicio), h.tipoCierre " +
            "ORDER BY ano, mes")
    List<Object[]> getEstadisticasPorMes(@Param("tenantId") String tenantId,
                                         @Param("desde") LocalDate desde);

    /**
     * Encontrar motivos mas frecuentes
     */
    @Query("SELECT h.motivo, COUNT(h) as frecuencia " +
            "FROM HorarioEspecial h " +
            "WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND h.motivo IS NOT NULL " +
            "AND h.fechaInicio >= :desde " +
            "GROUP BY h.motivo " +
            "ORDER BY frecuencia DESC")
    List<Object[]> findMotivosMasFrecuentes(@Param("tenantId") String tenantId,
                                            @Param("desde") LocalDate desde);

    /**
     * Verificar solapamientos antes de crear nuevos cierres
     */
    @Query("SELECT h FROM HorarioEspecial h WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND h.id != :excludeId " +
            "AND ((h.fechaInicio BETWEEN :inicio AND :fin) OR " +
            "(h.fechaFin BETWEEN :inicio AND :fin) OR " +
            "(h.fechaInicio <= :inicio AND h.fechaFin >= :fin))")
    List<HorarioEspecial> findSolapamientos(@Param("tenantId") String tenantId,
                                            @Param("inicio") LocalDate inicio,
                                            @Param("fin") LocalDate fin,
                                            @Param("excludeId") String excludeId);

    /**
     * Query optimizada para la IA: verificacion SUPER RAPIDA de disponibilidad
     */
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN false ELSE true END " +
            "FROM HorarioEspecial h WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND :fecha BETWEEN h.fechaInicio AND h.fechaFin " +
            "AND h.tipoCierre = 'CERRADO_COMPLETO'")
    Boolean isFechaDisponible(@Param("tenantId") String tenantId,
                              @Param("fecha") LocalDate fecha);

    /**
     * Verificar si hay cierres de emergencia activos HOY
     */
    @Query("SELECT h FROM HorarioEspecial h WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND CURRENT_DATE BETWEEN h.fechaInicio AND h.fechaFin " +
            "AND h.tipoCierre = 'CERRADO_COMPLETO' " +
            "AND h.creadoPor = 'cierre_rapido'")
    List<HorarioEspecial> findCierresEmergenciaActivos(@Param("tenantId") String tenantId);

    /**
     * Buscar cierres por motivo
     */
    @Query("SELECT h FROM HorarioEspecial h WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND LOWER(h.motivo) LIKE LOWER(CONCAT('%', :motivo, '%')) " +
            "ORDER BY h.fechaInicio DESC")
    List<HorarioEspecial> findByMotivoContaining(@Param("tenantId") String tenantId,
                                                 @Param("motivo") String motivo);

    /**
     * Buscar por tipo de cierre especifico
     */
    @Query("SELECT h FROM HorarioEspecial h WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND h.tipoCierre = :tipoCierre " +
            "ORDER BY h.fechaInicio DESC")
    List<HorarioEspecial> findByTenantAndTipoCierre(@Param("tenantId") String tenantId,
                                                    @Param("tipoCierre") TipoCierre tipoCierre);

    /**
     * Obtener cierres completos posteriores a una fecha (para calcular siguiente disponible)
     */
    @Query("SELECT h FROM HorarioEspecial h WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND h.fechaInicio > :fechaInicio " +
            "AND h.tipoCierre = 'CERRADO_COMPLETO' " +
            "ORDER BY h.fechaInicio ASC")
    List<HorarioEspecial> findCierresCompletosPosteriores(@Param("tenantId") String tenantId,
                                                          @Param("fechaInicio") LocalDate fechaInicio);

    /**
     * Calcular cierres en un periodo
     */
    @Query("SELECT COUNT(h) FROM HorarioEspecial h " +
            "WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND h.tipoCierre = 'CERRADO_COMPLETO' " +
            "AND h.fechaInicio BETWEEN :inicio AND :fin")
    Integer countDiasCerradosEnPeriodo(@Param("tenantId") String tenantId,
                                       @Param("inicio") LocalDate inicio,
                                       @Param("fin") LocalDate fin);

    /**
     * Encontrar periodos de mayor actividad de cierres
     */
    @Query("SELECT EXTRACT(YEAR FROM h.fechaInicio) as ano, " +
            "EXTRACT(MONTH FROM h.fechaInicio) as mes, " +
            "COUNT(h) as total_cierres " +
            "FROM HorarioEspecial h " +
            "WHERE h.tenantId = :tenantId " +
            "AND h.activo = true " +
            "AND h.fechaInicio >= :desde " +
            "GROUP BY EXTRACT(YEAR FROM h.fechaInicio), EXTRACT(MONTH FROM h.fechaInicio) " +
            "ORDER BY total_cierres DESC")
    List<Object[]> findPeriodosMayorActividad(@Param("tenantId") String tenantId,
                                              @Param("desde") LocalDate desde);

    // Consultas basicas JPA
    List<HorarioEspecial> findByTenantIdAndActivoTrueOrderByFechaInicio(String tenantId);

    List<HorarioEspecial> findByTenantIdAndTipoCierreAndActivoTrue(String tenantId, TipoCierre tipoCierre);

    Optional<HorarioEspecial> findByTenantIdAndIdAndActivo(String tenantId, String id, Boolean activo);

    Long countByTenantIdAndActivoTrue(String tenantId);

    Boolean existsByTenantIdAndFechaInicioLessThanEqualAndFechaFinGreaterThanEqualAndActivo(
            String tenantId, LocalDate fechaFin, LocalDate fechaInicio, Boolean activo);
}