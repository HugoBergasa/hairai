package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, String> {

    // ===== MÉTODOS EXISTENTES - NO TOCAR =====

    @Query("SELECT c FROM Cita c WHERE c.tenant.id = :tenantId AND c.fechaHora BETWEEN :inicio AND :fin")
    List<Cita> findByTenantIdAndFechaHoraBetween(@Param("tenantId") String tenantId,
                                                 @Param("inicio") LocalDateTime inicio,
                                                 @Param("fin") LocalDateTime fin);

    List<Cita> findByClienteTelefonoAndEstado(String telefono, EstadoCita estado);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.tenant.id = :tenantId AND c.fechaHora BETWEEN :inicio AND :fin")
    long countByTenantIdAndFechaHoraBetween(@Param("tenantId") String tenantId,
                                            @Param("inicio") LocalDateTime inicio,
                                            @Param("fin") LocalDateTime fin);

    // ===== NUEVOS MÉTODOS PARA DASHBOARD IA =====

    /**
     * 📋 OBTENER TODAS LAS CITAS DE UN TENANT ORDENADAS POR FECHA
     */
    @Query("SELECT c FROM Cita c WHERE c.tenant.id = :tenantId ORDER BY c.fechaHora DESC")
    List<Cita> findByTenantIdOrderByFechaHoraDesc(@Param("tenantId") String tenantId);

    /**
     * 🔄 OBTENER CITAS POR TENANT Y ESTADO
     */
    @Query("SELECT c FROM Cita c WHERE c.tenant.id = :tenantId AND c.estado = :estado ORDER BY c.fechaHora DESC")
    List<Cita> findByTenantIdAndEstado(@Param("tenantId") String tenantId,
                                       @Param("estado") EstadoCita estado);

    /**
     * 👤 OBTENER CITAS DE UN EMPLEADO EN RANGO DE FECHAS (PARA IA LOAD BALANCING)
     */
    @Query("SELECT c FROM Cita c WHERE c.empleado.id = :empleadoId AND c.fechaHora BETWEEN :inicio AND :fin")
    List<Cita> findByEmpleadoIdAndFechaHoraBetween(@Param("empleadoId") String empleadoId,
                                                   @Param("inicio") LocalDateTime inicio,
                                                   @Param("fin") LocalDateTime fin);

    // ===== MÉTODOS ADICIONALES PARA ESTADÍSTICAS PREMIUM =====

    /**
     * 📊 CONTAR CITAS POR ESTADO EN UN TENANT
     */
    @Query("SELECT COUNT(c) FROM Cita c WHERE c.tenant.id = :tenantId AND c.estado = :estado")
    long countByTenantIdAndEstado(@Param("tenantId") String tenantId,
                                  @Param("estado") EstadoCita estado);

    /**
     * 💰 CALCULAR INGRESOS POR TENANT EN RANGO DE FECHAS
     */
    @Query("SELECT COALESCE(SUM(c.precio), 0) FROM Cita c WHERE c.tenant.id = :tenantId AND c.estado = 'COMPLETADA' AND c.fechaHora BETWEEN :inicio AND :fin")
    Double calcularIngresosByTenantIdAndFechaHora(@Param("tenantId") String tenantId,
                                                  @Param("inicio") LocalDateTime inicio,
                                                  @Param("fin") LocalDateTime fin);

    /**
     * 📈 OBTENER CITAS COMPLETADAS DEL MES ACTUAL
     */
    @Query("SELECT c FROM Cita c WHERE c.tenant.id = :tenantId AND c.estado = 'COMPLETADA' AND MONTH(c.fechaHora) = MONTH(CURRENT_DATE) AND YEAR(c.fechaHora) = YEAR(CURRENT_DATE)")
    List<Cita> findCitasCompletadasEsteMes(@Param("tenantId") String tenantId);

    /**
     * 🎯 OBTENER SERVICIOS MÁS POPULARES POR TENANT
     */
    @Query("SELECT c.servicio.nombre, COUNT(c) as total FROM Cita c WHERE c.tenant.id = :tenantId AND c.estado = 'COMPLETADA' GROUP BY c.servicio.nombre ORDER BY total DESC")
    List<Object[]> findServiciosMasPopulares(@Param("tenantId") String tenantId);

    /**
     * 👥 OBTENER EMPLEADOS MÁS PRODUCTIVOS
     */
    @Query("SELECT c.empleado.nombre, COUNT(c) as total FROM Cita c WHERE c.tenant.id = :tenantId AND c.estado = 'COMPLETADA' AND c.empleado IS NOT NULL GROUP BY c.empleado.nombre ORDER BY total DESC")
    List<Object[]> findEmpleadosMasProductivos(@Param("tenantId") String tenantId);

    /**
     * 📅 OBTENER HORAS PICO DE RESERVAS
     */
    @Query("SELECT HOUR(c.fechaHora) as hora, COUNT(c) as total FROM Cita c WHERE c.tenant.id = :tenantId AND c.estado IN ('CONFIRMADA', 'COMPLETADA') GROUP BY HOUR(c.fechaHora) ORDER BY total DESC")
    List<Object[]> findHorasPicoReservas(@Param("tenantId") String tenantId);

    /**
     * 🔄 TASA DE CANCELACIÓN POR TENANT
     */
    @Query("SELECT " +
            "COUNT(CASE WHEN c.estado = 'CANCELADA' THEN 1 END) as canceladas, " +
            "COUNT(c) as total " +
            "FROM Cita c WHERE c.tenant.id = :tenantId AND c.fechaHora >= :desde")
    List<Object[]> calcularTasaCancelacion(@Param("tenantId") String tenantId,
                                           @Param("desde") LocalDateTime desde);

    /**
     * 🆕 CLIENTES NUEVOS QUE AGENDARON POR PRIMERA VEZ
     */
    @Query("SELECT COUNT(DISTINCT c.cliente.id) FROM Cita c WHERE c.tenant.id = :tenantId AND c.fechaHora BETWEEN :inicio AND :fin AND c.cliente.fechaRegistro BETWEEN :inicio AND :fin")
    long countClientesNuevosConCita(@Param("tenantId") String tenantId,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fin") LocalDateTime fin);

    /**
     * ⏱️ TIEMPO PROMEDIO ENTRE CITAS DE UN CLIENTE
     */
    @Query("SELECT c FROM Cita c WHERE c.cliente.id = :clienteId AND c.estado = 'COMPLETADA' ORDER BY c.fechaHora DESC")
    List<Cita> findCitasCompletadasByCliente(@Param("clienteId") String clienteId);

    /**
     * 🎯 PREDICCIÓN IA: Citas con alta probabilidad de cancelación
     */
    @Query("SELECT c FROM Cita c WHERE c.tenant.id = :tenantId AND c.estado = 'CONFIRMADA' AND c.fechaHora > CURRENT_TIMESTAMP AND c.fechaHora <= :limite")
    List<Cita> findCitasEnRiesgoDeNoShow(@Param("tenantId") String tenantId,
                                         @Param("limite") LocalDateTime limite);

    /**
     * 💡 IA INSIGHTS: Patrones de reserva por cliente
     */
    @Query("SELECT c.cliente.id, c.cliente.nombre, COUNT(c) as totalCitas, AVG(c.precio) as gastoPromedio FROM Cita c WHERE c.tenant.id = :tenantId AND c.estado = 'COMPLETADA' GROUP BY c.cliente.id, c.cliente.nombre HAVING COUNT(c) >= :minCitas ORDER BY totalCitas DESC")
    List<Object[]> findClientesFrecuentes(@Param("tenantId") String tenantId,
                                          @Param("minCitas") long minCitas);

    /**
     * 📊 INGRESOS POR EMPLEADO Y PERÍODO
     */
    @Query("SELECT c.empleado.nombre, COALESCE(SUM(c.precio), 0) as ingresos FROM Cita c WHERE c.tenant.id = :tenantId AND c.estado = 'COMPLETADA' AND c.empleado IS NOT NULL AND c.fechaHora BETWEEN :inicio AND :fin GROUP BY c.empleado.nombre ORDER BY ingresos DESC")
    List<Object[]> calcularIngresosPorEmpleado(@Param("tenantId") String tenantId,
                                               @Param("inicio") LocalDateTime inicio,
                                               @Param("fin") LocalDateTime fin);

    /**
     * 🎯 OPTIMIZACIÓN IA: Slots más demandados por día de semana
     */
    @Query("SELECT DAYOFWEEK(c.fechaHora) as diaSemana, HOUR(c.fechaHora) as hora, COUNT(c) as demanda FROM Cita c WHERE c.tenant.id = :tenantId AND c.estado IN ('CONFIRMADA', 'COMPLETADA') GROUP BY DAYOFWEEK(c.fechaHora), HOUR(c.fechaHora) ORDER BY demanda DESC")
    List<Object[]> findPatronesDemandaPorDiaYHora(@Param("tenantId") String tenantId);
}