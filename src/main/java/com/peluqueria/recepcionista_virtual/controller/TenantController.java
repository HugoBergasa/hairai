package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.model.Tenant;
import com.peluqueria.recepcionista_virtual.repository.TenantRepository;
import com.peluqueria.recepcionista_virtual.service.TenantService;
import com.peluqueria.recepcionista_virtual.service.ServicioService;
import com.peluqueria.recepcionista_virtual.service.EmpleadoService;
import com.peluqueria.recepcionista_virtual.dto.ServicioDTO;
import com.peluqueria.recepcionista_virtual.dto.EmpleadoDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ServicioService servicioService;

    @Autowired
    private EmpleadoService empleadoService;


    @Autowired
    private TenantRepository tenantRepository;

    @GetMapping("/info")
    public ResponseEntity<?> getTenantInfo(@RequestAttribute(required = false) String tenantId) {
        if (tenantId == null) {
            // Devolver tenant de demo si no hay autenticación
            Tenant demo = new Tenant();
            demo.setNombrePeluqueria("Peluquería Demo");
            demo.setTelefono("+34900000000");
            demo.setEmail("demo@peluqueria.com");
            demo.setDireccion("Calle Principal 123");
            demo.setHoraApertura("09:00");
            demo.setHoraCierre("20:00");
            demo.setDiasLaborables("L,M,X,J,V,S");
            return ResponseEntity.ok(demo);
        }

        Tenant tenant = tenantService.findById(tenantId);
        return ResponseEntity.ok(tenant);
    }

    @GetMapping("/servicios")
    public ResponseEntity<?> getServicios(@RequestAttribute(required = false) String tenantId) {
        if (tenantId == null) {
            return ResponseEntity.badRequest().body("Tenant ID requerido");
        }

        try {
            List<ServicioDTO> servicios = servicioService.getServiciosByTenantId(tenantId);
            return ResponseEntity.ok(servicios);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al obtener servicios: " + e.getMessage());
        }
    }

    @GetMapping("/empleados")
    public ResponseEntity<?> getEmpleados(@RequestAttribute(required = false) String tenantId) {
        if (tenantId == null) {
            return ResponseEntity.badRequest().body("Tenant ID requerido");
        }

        try {
            List<EmpleadoDTO> empleados = empleadoService.getEmpleadosByTenantId(tenantId);
            return ResponseEntity.ok(empleados);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al obtener empleados: " + e.getMessage());
        }
    }

    /**
     * 🏢 OBTENER CONFIGURACIÓN DEL TENANT ACTUAL - MULTITENANT
     */
    @GetMapping("/config")
    public ResponseEntity<?> getConfiguracionTenant(HttpServletRequest request) {
        try {
            // Obtener tenant ID del header (multitenant)
            String tenantId = (String) request.getAttribute("tenantId");

            if (tenantId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No autorizado - tenantId requerido"));
            }

            // Buscar tenant
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

            // Crear respuesta con configuración necesaria para validaciones
            Map<String, Object> config = new HashMap<>();
            config.put("id", tenant.getId());
            config.put("nombrePeluqueria", tenant.getNombrePeluqueria());
            config.put("horaApertura", tenant.getHoraApertura());
            config.put("horaCierre", tenant.getHoraCierre());
            config.put("diasLaborables", tenant.getDiasLaborables());
            config.put("duracionCitaMinutos", tenant.getDuracionCitaMinutos());
            config.put("telefono", tenant.getTelefono());
            config.put("direccion", tenant.getDireccion());

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error obteniendo configuración: " + e.getMessage()));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateTenant(
            @RequestAttribute(required = true) String tenantId,
            @RequestBody Tenant tenantData
    ) {
        try {
            Tenant updated = tenantService.updateTenant(tenantId, tenantData);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al actualizar tenant: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTenant(@RequestBody Tenant tenant) {
        Tenant saved = tenantService.save(tenant);
        tenantService.createDefaultServices(saved.getId());
        return ResponseEntity.ok(saved);
    }
}
