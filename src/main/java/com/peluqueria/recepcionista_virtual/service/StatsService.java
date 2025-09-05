package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.repository.CitaRepository;
import com.peluqueria.recepcionista_virtual.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class StatsService {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    /**
     * MULTI-TENANT: Obtener estadísticas básicas filtradas por tenant
     */
    public Map<String, Object> getStatsByTenant(String tenantId) {
        try {
            Map<String, Object> stats = new HashMap<>();

            if (tenantId == null) {
                System.out.println("WARN StatsService: tenantId es null, devolviendo stats por defecto");
                return createDefaultStats();
            }

            System.out.println("DEBUG StatsService: Calculando stats para tenant " + tenantId);

            // Rangos de tiempo para hoy
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59);

            // MULTI-TENANT: Usar método existente countByTenantIdAndFechaHoraBetween
            long citasHoy = citaRepository.countByTenantIdAndFechaHoraBetween(
                    tenantId, startOfDay, endOfDay
            );

            // Estadísticas básicas
            stats.put("citasHoy", citasHoy);
            stats.put("ingresosMes", citasHoy * 25.0); // Estimación básica
            stats.put("clientesNuevos", Math.max(0, (int)(citasHoy / 2))); // Estimación
            stats.put("tasaCancelacion", 5.2); // Placeholder

            // Metadata para debug
            stats.put("tenantId", tenantId);
            stats.put("calculatedAt", now.toString());

            System.out.println("DEBUG StatsService: Stats calculadas exitosamente para tenant " + tenantId);
            return stats;

        } catch (Exception e) {
            System.err.println("ERROR en getStatsByTenant para tenant " + tenantId + ": " + e.getMessage());
            e.printStackTrace();
            return createDefaultStats();
        }
    }

    /**
     * NUEVO: Estadísticas extendidas para dashboard
     * Usa solo métodos que existen en el repository
     */
    public Map<String, Object> getDashboardStats(String tenantId) {
        try {
            if (tenantId == null) {
                return createDefaultDashboardStats();
            }

            Map<String, Object> stats = new HashMap<>();
            LocalDateTime now = LocalDateTime.now();

            // Rangos de tiempo
            LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59);
            LocalDateTime startOfWeek = now.minusDays(7);
            LocalDateTime startOfMonth = now.minusDays(30);

            // MULTI-TENANT: Usar métodos existentes del repository
            long citasHoy = citaRepository.countByTenantIdAndFechaHoraBetween(
                    tenantId, startOfDay, endOfDay
            );

            long citasSemana = citaRepository.countByTenantIdAndFechaHoraBetween(
                    tenantId, startOfWeek, endOfDay
            );

            long citasMes = citaRepository.countByTenantIdAndFechaHoraBetween(
                    tenantId, startOfMonth, endOfDay
            );

            // Estadísticas de citas
            stats.put("citasHoy", citasHoy);
            stats.put("citasSemana", citasSemana);
            stats.put("citasMes", citasMes);

            // Estimaciones de ingresos
            double precioPorCita = 25.0;
            stats.put("ingresosDia", citasHoy * precioPorCita);
            stats.put("ingresosSemana", citasSemana * precioPorCita);
            stats.put("ingresosMes", citasMes * precioPorCita);

            // Estadísticas adicionales estimadas
            stats.put("clientesNuevos", Math.max(0, (int)(citasSemana / 3)));
            stats.put("llamadasTotal", citasMes * 2);
            stats.put("llamadasHoy", citasHoy + 1);

            // Metadata
            stats.put("tenantId", tenantId);
            stats.put("type", "dashboard");

            return stats;

        } catch (Exception e) {
            System.err.println("ERROR en getDashboardStats: " + e.getMessage());
            return createDefaultDashboardStats();
        }
    }

    /**
     * MÉTODOS AUXILIARES: Crear estadísticas por defecto
     */
    private Map<String, Object> createDefaultStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("citasHoy", 0);
        stats.put("ingresosMes", 0);
        stats.put("clientesNuevos", 0);
        stats.put("tasaCancelacion", 0);
        stats.put("message", "Sin datos disponibles o tenantId nulo");
        return stats;
    }

    private Map<String, Object> createDefaultDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("citasHoy", 0);
        stats.put("citasSemana", 0);
        stats.put("citasMes", 0);
        stats.put("ingresosDia", 0.0);
        stats.put("ingresosSemana", 0.0);
        stats.put("ingresosMes", 0.0);
        stats.put("clientesNuevos", 0);
        stats.put("llamadasTotal", 0);
        stats.put("llamadasHoy", 0);
        stats.put("message", "Sin datos disponibles");
        return stats;
    }
}