package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.model.Tenant;
import com.peluqueria.recepcionista_virtual.service.TenantService;
import com.peluqueria.recepcionista_virtual.service.ServicioService;
import com.peluqueria.recepcionista_virtual.service.EmpleadoService;
import com.peluqueria.recepcionista_virtual.dto.ServicioDTO;
import com.peluqueria.recepcionista_virtual.dto.EmpleadoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ServicioService servicioService;

    @Autowired
    private EmpleadoService empleadoService;

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