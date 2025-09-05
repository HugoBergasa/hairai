package com.peluqueria.recepcionista_virtual.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.peluqueria.recepcionista_virtual.service.StatsService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin
public class StatsController {

    @Autowired
    private StatsService statsService;

    /**
     * MULTI-TENANT: Obtener estadísticas generales - CORREGIDO
     */
    @GetMapping
    public ResponseEntity<?> getStats(HttpServletRequest request) {
        try {
            // CORRECCIÓN: Extraer tenantId del request attribute (establecido por JWT Filter)
            String tenantId = (String) request.getAttribute("tenantId");

            System.out.println("DEBUG StatsController.getStats - TenantId: " + tenantId);

            if (tenantId == null) {
                System.out.println("WARN: TenantId es null en /api/stats - devolviendo stats por defecto");
                // Devolver estadísticas por defecto en lugar de error
                Map<String, Object> defaultStats = createDefaultStats();
                return ResponseEntity.ok(defaultStats);
            }

            // MULTI-TENANT: Obtener stats filtradas por tenantId
            Map<String, Object> stats = statsService.getStatsByTenant(tenantId);
            System.out.println("DEBUG: Stats obtenidas para tenant " + tenantId + " - citasHoy: " + stats.get("citasHoy"));

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            System.err.println("ERROR en getStats: " + e.getMessage());
            e.printStackTrace();

            // En caso de error, devolver estadísticas por defecto para no romper el frontend
            Map<String, Object> defaultStats = createDefaultStats();
            return ResponseEntity.ok(defaultStats);
        }
    }

    /**
     * NUEVO: Estadísticas extendidas para dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats(HttpServletRequest request) {
        try {
            // CORRECCIÓN: Extraer tenantId del request
            String tenantId = (String) request.getAttribute("tenantId");

            System.out.println("DEBUG StatsController.getDashboardStats - TenantId: " + tenantId);

            if (tenantId == null) {
                return ResponseEntity.ok(createDefaultDashboardStats());
            }

            // MULTI-TENANT: Stats extendidas para el dashboard
            Map<String, Object> dashboardStats = statsService.getDashboardStats(tenantId);

            return ResponseEntity.ok(dashboardStats);

        } catch (Exception e) {
            System.err.println("ERROR en getDashboardStats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(createDefaultDashboardStats());
        }
    }

    /**
     * NUEVO: Endpoint de prueba para verificar conectividad
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck(HttpServletRequest request) {
        try {
            String tenantId = (String) request.getAttribute("tenantId");

            Map<String, Object> health = new HashMap<>();
            health.put("status", "OK");
            health.put("timestamp", System.currentTimeMillis());
            health.put("tenantId", tenantId);
            health.put("service", "StatsController");

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * MÉTODOS AUXILIARES: Crear estadísticas por defecto
     */
    private Map<String, Object> createDefaultStats() {
        Map<String, Object> defaultStats = new HashMap<>();
        defaultStats.put("citasHoy", 0);
        defaultStats.put("ingresosMes", 0);
        defaultStats.put("clientesNuevos", 0);
        defaultStats.put("tasaCancelacion", 0);
        defaultStats.put("message", "No hay datos disponibles");
        defaultStats.put("timestamp", System.currentTimeMillis());
        return defaultStats;
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
        stats.put("message", "Dashboard sin datos");
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }
}