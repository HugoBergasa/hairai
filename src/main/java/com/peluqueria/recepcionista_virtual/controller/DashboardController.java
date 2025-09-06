package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.dto.*;
import com.peluqueria.recepcionista_virtual.service.*;
import com.peluqueria.recepcionista_virtual.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ServicioService servicioService;

    @Autowired
    private EmpleadoService empleadoService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private CitaService citaService;

    @Autowired
    private StatsService statsService;

    /**
     * 🔒 SEGURIDAD MEJORADA: Extracción segura de tenantId
     */
    private String extractTenantId(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Token de autorización requerido");
            }

            String token = authHeader.substring(7);
            String tenantId = jwtTokenUtil.extractTenantId(token);

            if (tenantId == null || tenantId.trim().isEmpty()) {
                throw new RuntimeException("TenantId no válido en token");
            }

            return tenantId;
        } catch (Exception e) {
            logger.error("Error extrayendo tenantId: {}", e.getMessage());
            throw new RuntimeException("Token no válido: " + e.getMessage());
        }
    }

    /**
     * 🔒 VALIDACIÓN: Verificar que el tenant existe y está activo
     */
    private void validateTenantAccess(String tenantId) {
        try {
            tenantService.findById(tenantId); // Lanza excepción si no existe
        } catch (Exception e) {
            logger.warn("Intento de acceso a tenant inválido: {}", tenantId);
            throw new RuntimeException("Acceso denegado al tenant");
        }
    }

    // ===== CONFIGURACIÓN DEL SALÓN =====
    @GetMapping("/salon/config")
    public ResponseEntity<?> getSalonConfig(HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            TenantDTO tenant = TenantDTO.fromTenant(tenantService.findById(tenantId));
            logger.info("Configuración del salón obtenida para tenant: {}", tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", tenant
            ));
        } catch (Exception e) {
            logger.error("Error obteniendo configuración del salón: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/salon/config")
    public ResponseEntity<?> updateSalonConfig(@Valid @RequestBody TenantDTO tenantData,
                                               HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            // 🔒 VALIDACIONES DE SEGURIDAD
            if (tenantData.getNombrePeluqueria() != null && tenantData.getNombrePeluqueria().trim().length() < 3) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El nombre debe tener al menos 3 caracteres"
                ));
            }

            // Crear objeto Tenant para actualización
            com.peluqueria.recepcionista_virtual.model.Tenant tenant =
                    new com.peluqueria.recepcionista_virtual.model.Tenant();
            tenant.setNombrePeluqueria(tenantData.getNombrePeluqueria());
            tenant.setTelefono(tenantData.getTelefono());
            tenant.setEmail(tenantData.getEmail());
            tenant.setDireccion(tenantData.getDireccion());
            tenant.setHoraApertura(tenantData.getHoraApertura());
            tenant.setHoraCierre(tenantData.getHoraCierre());
            tenant.setDiasLaborables(tenantData.getDiasLaborables());

            com.peluqueria.recepcionista_virtual.model.Tenant updated =
                    tenantService.updateTenant(tenantId, tenant);

            logger.info("Configuración actualizada para tenant: {}", tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", TenantDTO.fromTenant(updated),
                    "message", "Configuración actualizada correctamente"
            ));
        } catch (Exception e) {
            logger.error("Error actualizando configuración: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ===== GESTIÓN DE SERVICIOS =====
    @GetMapping("/servicios")
    public ResponseEntity<?> getServicios(HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            List<ServicioDTO> servicios = servicioService.getServiciosByTenantId(tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", servicios,
                    "count", servicios.size()
            ));
        } catch (Exception e) {
            logger.error("Error obteniendo servicios: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/servicios")
    public ResponseEntity<?> createServicio(@Valid @RequestBody ServicioDTO servicioDTO,
                                            HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            // 🔒 VALIDACIONES DE NEGOCIO
            if (servicioDTO.getNombre() == null || servicioDTO.getNombre().trim().length() < 2) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El nombre del servicio es requerido (mín. 2 caracteres)"
                ));
            }

            if (servicioDTO.getPrecio() == null || servicioDTO.getPrecio().doubleValue() <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El precio debe ser mayor a 0"
                ));
            }

            if (servicioDTO.getDuracionMinutos() == null || servicioDTO.getDuracionMinutos() <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "La duración debe ser mayor a 0 minutos"
                ));
            }

            ServicioDTO created = servicioService.createServicio(tenantId, servicioDTO);
            logger.info("Servicio creado: {} para tenant: {}", created.getNombre(), tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", created,
                    "message", "Servicio creado correctamente"
            ));
        } catch (Exception e) {
            logger.error("Error creando servicio: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/servicios/{servicioId}")
    public ResponseEntity<?> updateServicio(@PathVariable String servicioId,
                                            @Valid @RequestBody ServicioDTO servicioDTO,
                                            HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            // 🔒 VALIDAR QUE EL SERVICIO PERTENECE AL TENANT
            ServicioDTO existing = servicioService.getServicioById(servicioId);
            if (!existing.getTenantId().equals(tenantId)) {
                logger.warn("Intento de modificar servicio de otro tenant. ServicioId: {}, TenantId: {}",
                        servicioId, tenantId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Acceso denegado al servicio"
                ));
            }

            ServicioDTO updated = servicioService.updateServicio(servicioId, servicioDTO);
            logger.info("Servicio actualizado: {} para tenant: {}", servicioId, tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", updated,
                    "message", "Servicio actualizado correctamente"
            ));
        } catch (Exception e) {
            logger.error("Error actualizando servicio: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/servicios/{servicioId}")
    public ResponseEntity<?> deleteServicio(@PathVariable String servicioId,
                                            HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            // 🔒 VALIDAR PROPIEDAD DEL SERVICIO
            ServicioDTO existing = servicioService.getServicioById(servicioId);
            if (!existing.getTenantId().equals(tenantId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Acceso denegado al servicio"
                ));
            }

            servicioService.deleteServicio(servicioId);
            logger.info("Servicio eliminado: {} para tenant: {}", servicioId, tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Servicio eliminado correctamente"
            ));
        } catch (Exception e) {
            logger.error("Error eliminando servicio: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ===== SIMILAR PARA EMPLEADOS CON VALIDACIONES =====
    // ... (Implementación similar con validaciones de seguridad)

    // ===== ESTADÍSTICAS SEGURAS =====
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            Map<String, Object> stats = statsService.getDashboardStats(tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats
            ));
        } catch (Exception e) {
            logger.error("Error obteniendo estadísticas: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ===== RESUMEN GENERAL DEL DASHBOARD =====
    @GetMapping("/overview")
    public ResponseEntity<?> getDashboardOverview(HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            Map<String, Object> overview = new HashMap<>();

            // Configuración del salón
            TenantDTO salon = TenantDTO.fromTenant(tenantService.findById(tenantId));
            overview.put("salon", salon);

            // Estadísticas
            Map<String, Object> stats = statsService.getDashboardStats(tenantId);
            overview.put("stats", stats);

            // Contadores rápidos
            List<ServicioDTO> servicios = servicioService.getServiciosByTenantId(tenantId);
            overview.put("totalServicios", servicios.size());

            // Empleados (si tienes el service)
            try {
                List<EmpleadoDTO> empleados = empleadoService.getEmpleadosByTenantId(tenantId);
                overview.put("totalEmpleados", empleados.size());
            } catch (Exception e) {
                overview.put("totalEmpleados", 0);
            }

            // Clientes (si tienes el service)
            try {
                List<ClienteDTO> clientes = clienteService.getClientesByTenantId(tenantId);
                overview.put("totalClientes", clientes.size());
            } catch (Exception e) {
                overview.put("totalClientes", 0);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", overview
            ));
        } catch (Exception e) {
            logger.error("Error obteniendo overview: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
