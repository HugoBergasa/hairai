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

    @GetMapping
    public ResponseEntity<?> getStats(HttpServletRequest request) {

        // LOGGING SÚPER AGRESIVO para diagnosticar
        System.out.println("=================================================================");
        System.out.println("STATS CONTROLLER - VERSIÓN SÚPER DEBUG - TIMESTAMP: " + System.currentTimeMillis());
        System.out.println("Método getStats() INICIADO");
        System.out.println("=================================================================");

        try {
            String tenantId = (String) request.getAttribute("tenantId");

            System.out.println("DIAGNÓSTICO: TenantId extraído = " + tenantId);
            System.out.println("DIAGNÓSTICO: TenantId es null = " + (tenantId == null));

            if (tenantId == null) {
                System.out.println("RAMA: TenantId es null - devolviendo stats por defecto");
                Map<String, Object> defaultStats = createDefaultStats();
                defaultStats.put("debug_info", "tenantId_was_null");
                defaultStats.put("controller_version", "super_debug_v1");
                System.out.println("RESPUESTA NULL: " + defaultStats);
                return ResponseEntity.ok(defaultStats);
            }

            System.out.println("RAMA: TenantId válido - llamando a statsService");
            Map<String, Object> stats = statsService.getStatsByTenant(tenantId);

            stats.put("debug_info", "success_with_tenant");
            stats.put("controller_version", "super_debug_v1");
            stats.put("debug_tenant_received", tenantId);

            System.out.println("RESPUESTA ÉXITO: " + stats);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            System.err.println("EXCEPCIÓN CAPTURADA EN STATS CONTROLLER:");
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println("Clase: " + e.getClass().getName());
            e.printStackTrace();

            Map<String, Object> errorStats = createDefaultStats();
            errorStats.put("debug_info", "exception_caught");
            errorStats.put("controller_version", "super_debug_v1");
            errorStats.put("error_message", e.getMessage());
            errorStats.put("error_class", e.getClass().getName());

            return ResponseEntity.ok(errorStats);

        } finally {
            System.out.println("=================================================================");
            System.out.println("STATS CONTROLLER - FINALIZANDO MÉTODO getStats()");
            System.out.println("=================================================================");
        }
    }

    private Map<String, Object> createDefaultStats() {
        Map<String, Object> defaultStats = new HashMap<>();
        defaultStats.put("citasHoy", 0);
        defaultStats.put("ingresosMes", 0);
        defaultStats.put("clientesNuevos", 0);
        defaultStats.put("tasaCancelacion", 0);
        defaultStats.put("message", "Controller súper debug funcionando");
        defaultStats.put("timestamp", System.currentTimeMillis());
        return defaultStats;
    }
}