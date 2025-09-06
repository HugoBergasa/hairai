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

    // ===== GESTIÓN DE EMPLEADOS =====
    @GetMapping("/empleados")
    public ResponseEntity<?> getEmpleados(HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            List<EmpleadoDTO> empleados = empleadoService.getEmpleadosByTenantId(tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", empleados,
                    "count", empleados.size()
            ));
        } catch (Exception e) {
            logger.error("Error obteniendo empleados: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/empleados")
    public ResponseEntity<?> createEmpleado(@Valid @RequestBody EmpleadoDTO empleadoDTO,
                                            HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            // 🔒 VALIDACIONES DE NEGOCIO
            if (empleadoDTO.getNombre() == null || empleadoDTO.getNombre().trim().length() < 2) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El nombre del empleado es requerido (mín. 2 caracteres)"
                ));
            }

            if (empleadoDTO.getEmail() != null && !empleadoDTO.getEmail().contains("@")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El email debe tener un formato válido"
                ));
            }

            EmpleadoDTO created = empleadoService.createEmpleado(tenantId, empleadoDTO);
            logger.info("Empleado creado: {} para tenant: {}", created.getNombre(), tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", created,
                    "message", "Empleado creado correctamente"
            ));
        } catch (Exception e) {
            logger.error("Error creando empleado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/empleados/{empleadoId}")
    public ResponseEntity<?> updateEmpleado(@PathVariable String empleadoId,
                                            @Valid @RequestBody EmpleadoDTO empleadoDTO,
                                            HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            // 🔒 VALIDAR QUE EL EMPLEADO PERTENECE AL TENANT
            EmpleadoDTO existing = empleadoService.getEmpleadoById(empleadoId);
            if (!existing.getTenantId().equals(tenantId)) {
                logger.warn("Intento de modificar empleado de otro tenant. EmpleadoId: {}, TenantId: {}",
                        empleadoId, tenantId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Acceso denegado al empleado"
                ));
            }

            EmpleadoDTO updated = empleadoService.updateEmpleado(empleadoId, empleadoDTO);
            logger.info("Empleado actualizado: {} para tenant: {}", empleadoId, tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", updated,
                    "message", "Empleado actualizado correctamente"
            ));
        } catch (Exception e) {
            logger.error("Error actualizando empleado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/empleados/{empleadoId}")
    public ResponseEntity<?> deleteEmpleado(@PathVariable String empleadoId,
                                            HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            // 🔒 VALIDAR PROPIEDAD DEL EMPLEADO
            EmpleadoDTO existing = empleadoService.getEmpleadoById(empleadoId);
            if (!existing.getTenantId().equals(tenantId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Acceso denegado al empleado"
                ));
            }

            empleadoService.deleteEmpleado(empleadoId);
            logger.info("Empleado eliminado: {} para tenant: {}", empleadoId, tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Empleado eliminado correctamente"
            ));
        } catch (Exception e) {
            logger.error("Error eliminando empleado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ===== GESTIÓN DE CLIENTES =====
    @GetMapping("/clientes")
    public ResponseEntity<?> getClientes(HttpServletRequest request,
                                         @RequestParam(required = false) String search,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            List<ClienteDTO> clientes;
            if (search != null && !search.trim().isEmpty()) {
                clientes = clienteService.searchClientesByTenantId(tenantId, search);
            } else {
                clientes = clienteService.getClientesByTenantId(tenantId);
            }

            // Paginación simple
            int total = clientes.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            List<ClienteDTO> paginatedClientes = clientes.subList(fromIndex, toIndex);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", paginatedClientes,
                    "count", paginatedClientes.size(),
                    "total", total,
                    "page", page,
                    "totalPages", (int) Math.ceil((double) total / size)
            ));
        } catch (Exception e) {
            logger.error("Error obteniendo clientes: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/clientes")
    public ResponseEntity<?> createCliente(@Valid @RequestBody ClienteDTO clienteDTO,
                                           HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            // 🔒 VALIDACIONES DE NEGOCIO
            if (clienteDTO.getNombre() == null || clienteDTO.getNombre().trim().length() < 2) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El nombre del cliente es requerido (mín. 2 caracteres)"
                ));
            }

            if (clienteDTO.getTelefono() == null || clienteDTO.getTelefono().trim().length() < 9) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El teléfono es requerido (mín. 9 caracteres)"
                ));
            }

            if (clienteDTO.getEmail() != null && !clienteDTO.getEmail().contains("@")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El email debe tener un formato válido"
                ));
            }

            ClienteDTO created = clienteService.createCliente(tenantId, clienteDTO);
            logger.info("Cliente creado: {} para tenant: {}", created.getNombre(), tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", created,
                    "message", "Cliente creado correctamente"
            ));
        } catch (Exception e) {
            logger.error("Error creando cliente: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/clientes/{clienteId}")
    public ResponseEntity<?> updateCliente(@PathVariable String clienteId,
                                           @Valid @RequestBody ClienteDTO clienteDTO,
                                           HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            // 🔒 VALIDAR QUE EL CLIENTE PERTENECE AL TENANT
            ClienteDTO existing = clienteService.getClienteById(clienteId);
            if (!existing.getTenantId().equals(tenantId)) {
                logger.warn("Intento de modificar cliente de otro tenant. ClienteId: {}, TenantId: {}",
                        clienteId, tenantId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Acceso denegado al cliente"
                ));
            }

            ClienteDTO updated = clienteService.updateCliente(clienteId, clienteDTO);
            logger.info("Cliente actualizado: {} para tenant: {}", clienteId, tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", updated,
                    "message", "Cliente actualizado correctamente"
            ));
        } catch (Exception e) {
            logger.error("Error actualizando cliente: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ===== GESTIÓN DE CITAS =====
    @GetMapping("/citas")
    public ResponseEntity<?> getCitas(HttpServletRequest request,
                                      @RequestParam(required = false) String fecha,
                                      @RequestParam(required = false) String estado,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            List<CitaDTO> citas;
            if (fecha != null) {
                citas = citaService.getCitasByTenantIdAndFecha(tenantId, fecha);
            } else if (estado != null) {
                citas = citaService.getCitasByTenantIdAndEstado(tenantId, estado);
            } else {
                citas = citaService.getCitasByTenantId(tenantId);
            }

            // Paginación
            int total = citas.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            List<CitaDTO> paginatedCitas = citas.subList(fromIndex, toIndex);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", paginatedCitas,
                    "count", paginatedCitas.size(),
                    "total", total,
                    "page", page,
                    "totalPages", (int) Math.ceil((double) total / size)
            ));
        } catch (Exception e) {
            logger.error("Error obteniendo citas: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/citas")
    public ResponseEntity<?> createCita(@Valid @RequestBody CitaDTO citaDTO,
                                        HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            // 🔒 VALIDACIONES DE NEGOCIO
            if (citaDTO.getClienteId() == null || citaDTO.getClienteId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El cliente es requerido"
                ));
            }

            if (citaDTO.getServicioId() == null || citaDTO.getServicioId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "El servicio es requerido"
                ));
            }

            if (citaDTO.getFechaHora() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "La fecha y hora son requeridas"
                ));
            }

            CitaDTO created = citaService.createCita(tenantId, citaDTO);
            logger.info("Cita creada: {} para tenant: {}", created.getId(), tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", created,
                    "message", "Cita creada correctamente"
            ));
        } catch (Exception e) {
            logger.error("Error creando cita: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/citas/{citaId}")
    public ResponseEntity<?> updateCita(@PathVariable String citaId,
                                        @Valid @RequestBody CitaDTO citaDTO,
                                        HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            // 🔒 VALIDAR QUE LA CITA PERTENECE AL TENANT
            CitaDTO existing = citaService.getCitaById(citaId);
            if (!existing.getTenantId().equals(tenantId)) {
                logger.warn("Intento de modificar cita de otro tenant. CitaId: {}, TenantId: {}",
                        citaId, tenantId);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Acceso denegado a la cita"
                ));
            }

            CitaDTO updated = citaService.updateCita(citaId, citaDTO);
            logger.info("Cita actualizada: {} para tenant: {}", citaId, tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", updated,
                    "message", "Cita actualizada correctamente"
            ));
        } catch (Exception e) {
            logger.error("Error actualizando cita: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/citas/{citaId}")
    public ResponseEntity<?> deleteCita(@PathVariable String citaId,
                                        HttpServletRequest request) {
        try {
            String tenantId = extractTenantId(request);
            validateTenantAccess(tenantId);

            // 🔒 VALIDAR PROPIEDAD DE LA CITA
            CitaDTO existing = citaService.getCitaById(citaId);
            if (!existing.getTenantId().equals(tenantId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Acceso denegado a la cita"
                ));
            }

            citaService.deleteCita(citaId);
            logger.info("Cita eliminada: {} para tenant: {}", citaId, tenantId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cita eliminada correctamente"
            ));
        } catch (Exception e) {
            logger.error("Error eliminando cita: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

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

            List<EmpleadoDTO> empleados = empleadoService.getEmpleadosByTenantId(tenantId);
            overview.put("totalEmpleados", empleados.size());

            List<ClienteDTO> clientes = clienteService.getClientesByTenantId(tenantId);
            overview.put("totalClientes", clientes.size());

            List<CitaDTO> citasHoy = citaService.getCitasHoyByTenantId(tenantId);
            overview.put("citasHoy", citasHoy.size());

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