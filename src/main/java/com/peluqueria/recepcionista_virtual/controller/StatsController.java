package com.peluqueria.recepcionista_virtual.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.peluqueria.recepcionista_virtual.service.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 🚀 STATS CONTROLLER v2.0 PREMIUM - IA-POWERED ANALYTICS
 *
 * Endpoints empresariales que justifican 350-400€/mes:
 * ✅ Analytics automáticos con 16+ métricas premium
 * ✅ Business Intelligence en tiempo real
 * ✅ Predicciones IA basadas en datos reales
 * ✅ Recomendaciones automáticas de negocio
 * ✅ Dashboard ejecutivo completo
 */
@RestController
@RequestMapping("/api/stats")
@CrossOrigin
public class StatsController {

    private static final Logger logger = LoggerFactory.getLogger(StatsController.class);

    @Autowired
    private StatsService statsService;

    // ===== ENDPOINTS BÁSICOS - COMPATIBILIDAD =====

    /**
     * 📊 STATS BÁSICAS - Endpoint legacy mantenido para compatibilidad
     */
    @GetMapping
    public ResponseEntity<?> getStats(HttpServletRequest request) {
        logger.info("StatsController v2.0: Endpoint básico /stats ejecutado");

        try {
            String tenantId = (String) request.getAttribute("tenantId");

            if (tenantId == null) {
                logger.warn("TenantId nulo en stats básicas - devolviendo respuesta por defecto");
                Map<String, Object> response = createSuccessResponse(
                        statsService.getStatsByTenant(null),
                        "Stats básicas - tenant no identificado"
                );
                return ResponseEntity.ok(response);
            }

            Map<String, Object> stats = statsService.getStatsByTenant(tenantId);
            Map<String, Object> response = createSuccessResponse(stats, "Stats básicas calculadas");

            logger.info("Stats básicas devueltas exitosamente para tenant: {}", tenantId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en endpoint básico /stats: {}", e.getMessage(), e);
            return ResponseEntity.ok(createErrorResponse("Error calculando estadísticas básicas"));
        }
    }

    /**
     * 📈 DASHBOARD PREMIUM - Métricas empresariales extendidas
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats(HttpServletRequest request) {
        logger.info("StatsController v2.0: Dashboard premium ejecutado");

        try {
            String tenantId = extractTenantId(request);

            if (tenantId == null) {
                return ResponseEntity.ok(createErrorResponse("Tenant requerido para dashboard premium"));
            }

            Map<String, Object> dashboardStats = statsService.getDashboardStats(tenantId);
            Map<String, Object> response = createSuccessResponse(
                    dashboardStats,
                    "Dashboard premium generado exitosamente"
            );

            logger.info("Dashboard premium generado para tenant: {}", tenantId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en dashboard premium: {}", e.getMessage(), e);
            return ResponseEntity.ok(createErrorResponse("Error generando dashboard premium"));
        }
    }

    // ===== ENDPOINTS PREMIUM - ANALYTICS EMPRESARIALES =====

    /**
     * 🎯 SERVICIOS MÁS POPULARES - Analytics de demanda
     */
    @GetMapping("/servicios-populares")
    public ResponseEntity<?> getServiciosPopulares(HttpServletRequest request) {
        logger.info("Endpoint servicios populares ejecutado");

        try {
            String tenantId = extractTenantId(request);

            if (tenantId == null) {
                return ResponseEntity.badRequest().body(
                        createErrorResponse("TenantId requerido para análisis de servicios")
                );
            }

            Map<String, Object> servicios = statsService.getServiciosPopulares(tenantId);
            Map<String, Object> response = createSuccessResponse(
                    servicios,
                    "Análisis de servicios populares completado"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en servicios populares: {}", e.getMessage(), e);
            return ResponseEntity.ok(createErrorResponse("Error analizando servicios populares"));
        }
    }

    /**
     * 👥 EMPLEADOS MÁS PRODUCTIVOS - Rankings de performance
     */
    @GetMapping("/empleados-productivos")
    public ResponseEntity<?> getEmpleadosProductivos(HttpServletRequest request) {
        logger.info("Endpoint empleados productivos ejecutado");

        try {
            String tenantId = extractTenantId(request);

            if (tenantId == null) {
                return ResponseEntity.badRequest().body(
                        createErrorResponse("TenantId requerido para análisis de empleados")
                );
            }

            Map<String, Object> empleados = statsService.getEmpleadosProductivos(tenantId);
            Map<String, Object> response = createSuccessResponse(
                    empleados,
                    "Análisis de productividad de empleados completado"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en empleados productivos: {}", e.getMessage(), e);
            return ResponseEntity.ok(createErrorResponse("Error analizando empleados productivos"));
        }
    }

    /**
     * ⏰ HORAS PICO - Optimización de staffing con IA
     */
    @GetMapping("/horas-pico")
    public ResponseEntity<?> getHorasPico(HttpServletRequest request) {
        logger.info("Endpoint horas pico ejecutado");

        try {
            String tenantId = extractTenantId(request);

            if (tenantId == null) {
                return ResponseEntity.badRequest().body(
                        createErrorResponse("TenantId requerido para análisis de horas pico")
                );
            }

            Map<String, Object> horasPico = statsService.getHorasPico(tenantId);
            Map<String, Object> response = createSuccessResponse(
                    horasPico,
                    "Análisis de horas pico y recomendaciones IA generadas"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en horas pico: {}", e.getMessage(), e);
            return ResponseEntity.ok(createErrorResponse("Error analizando horas pico"));
        }
    }

    /**
     * 👑 CLIENTES VIP - Análisis de valor y LTV
     */
    @GetMapping("/clientes-vip")
    public ResponseEntity<?> getClientesVIP(HttpServletRequest request) {
        logger.info("Endpoint clientes VIP ejecutado");

        try {
            String tenantId = extractTenantId(request);

            if (tenantId == null) {
                return ResponseEntity.badRequest().body(
                        createErrorResponse("TenantId requerido para análisis de clientes VIP")
                );
            }

            Map<String, Object> clientesVIP = statsService.getClientesVIP(tenantId);
            Map<String, Object> response = createSuccessResponse(
                    clientesVIP,
                    "Análisis de clientes VIP y estrategias de fidelización generadas"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en clientes VIP: {}", e.getMessage(), e);
            return ResponseEntity.ok(createErrorResponse("Error analizando clientes VIP"));
        }
    }

    /**
     * 📊 ANÁLISIS COMPLETO PREMIUM - Dashboard ejecutivo total
     */
    @GetMapping("/analisis-completo")
    public ResponseEntity<?> getAnalisisCompleto(HttpServletRequest request) {
        logger.info("Endpoint análisis completo premium ejecutado");

        try {
            String tenantId = extractTenantId(request);

            if (tenantId == null) {
                return ResponseEntity.badRequest().body(
                        createErrorResponse("TenantId requerido para análisis completo premium")
                );
            }

            Map<String, Object> analisisCompleto = statsService.getAnalisisCompleto(tenantId);
            Map<String, Object> response = createSuccessResponse(
                    analisisCompleto,
                    "Análisis empresarial completo generado con predicciones IA"
            );

            logger.info("Análisis completo premium generado exitosamente para tenant: {}", tenantId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en análisis completo: {}", e.getMessage(), e);
            return ResponseEntity.ok(createErrorResponse("Error generando análisis completo premium"));
        }
    }

    // ===== ENDPOINTS ESPECÍFICOS PARA PREDICCIONES IA =====

    /**
     * 🔮 PREDICCIONES DE INGRESOS - Forecasting automático
     */
    @GetMapping("/predicciones-ingresos")
    public ResponseEntity<?> getPrediccionesIngresos(HttpServletRequest request) {
        logger.info("Endpoint predicciones de ingresos ejecutado");

        try {
            String tenantId = extractTenantId(request);

            if (tenantId == null) {
                return ResponseEntity.badRequest().body(
                        createErrorResponse("TenantId requerido para predicciones de ingresos")
                );
            }

            // Extraer solo las predicciones del análisis completo
            Map<String, Object> analisisCompleto = statsService.getAnalisisCompleto(tenantId);
            Object predicciones = analisisCompleto.get("prediccionIngresos");

            Map<String, Object> response = createSuccessResponse(
                    predicciones,
                    "Predicciones de ingresos 30/90 días generadas por IA"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en predicciones de ingresos: {}", e.getMessage(), e);
            return ResponseEntity.ok(createErrorResponse("Error generando predicciones de ingresos"));
        }
    }

    /**
     * 🧠 RECOMENDACIONES IA - Business Intelligence automático
     */
    @GetMapping("/recomendaciones-ia")
    public ResponseEntity<?> getRecomendacionesIA(HttpServletRequest request) {
        logger.info("Endpoint recomendaciones IA ejecutado");

        try {
            String tenantId = extractTenantId(request);

            if (tenantId == null) {
                return ResponseEntity.badRequest().body(
                        createErrorResponse("TenantId requerido para recomendaciones IA")
                );
            }

            // Extraer recomendaciones del análisis completo
            Map<String, Object> analisisCompleto = statsService.getAnalisisCompleto(tenantId);
            Object recomendaciones = analisisCompleto.get("recomendacionesIA");
            Object alertas = analisisCompleto.get("alertas");

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("recomendaciones", recomendaciones);
            resultado.put("alertas", alertas);
            resultado.put("tenantId", tenantId);

            Map<String, Object> response = createSuccessResponse(
                    resultado,
                    "Recomendaciones empresariales y alertas inteligentes generadas"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en recomendaciones IA: {}", e.getMessage(), e);
            return ResponseEntity.ok(createErrorResponse("Error generando recomendaciones IA"));
        }
    }

    // ===== ENDPOINTS DE COMPATIBILIDAD CON DASHBOARD CONTROLLER =====

    /**
     * 📊 OVERVIEW - Compatibilidad con DashboardController
     */
    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(HttpServletRequest request) {
        logger.info("Endpoint overview (compatibilidad dashboard) ejecutado");

        try {
            String tenantId = extractTenantId(request);

            Map<String, Object> overview = new HashMap<>();
            overview.put("statsBasicas", statsService.getStatsByTenant(tenantId));
            overview.put("dashboardPremium", statsService.getDashboardStats(tenantId));
            overview.put("version", "v2.0_premium");
            overview.put("compatible", true);

            Map<String, Object> response = createSuccessResponse(
                    overview,
                    "Overview completo para dashboard"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en overview: {}", e.getMessage(), e);
            return ResponseEntity.ok(createErrorResponse("Error generando overview"));
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Extraer tenantId del request con validación
     */
    private String extractTenantId(HttpServletRequest request) {
        String tenantId = (String) request.getAttribute("tenantId");

        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.warn("TenantId nulo o vacío en request");
            return null;
        }

        return tenantId;
    }

    /**
     * Crear respuesta de éxito estandarizada
     */
    private Map<String, Object> createSuccessResponse(Object data, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        response.put("message", message);
        response.put("version", "stats_v2.0_premium");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Crear respuesta de error estandarizada
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("data", null);
        response.put("message", message);
        response.put("version", "stats_v2.0_premium");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}