package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.model.Tenant;
import com.peluqueria.recepcionista_virtual.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant")
@CrossOrigin
public class TenantController {

    @Autowired
    private TenantService tenantService;

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

    @PutMapping("/update")
    public ResponseEntity<?> updateTenant(
            @RequestAttribute(required = false) String tenantId,
            @RequestBody Tenant tenantData
    ) {
        if (tenantId == null) {
            return ResponseEntity.badRequest().body("No autorizado");
        }

        Tenant updated = tenantService.updateTenant(tenantId, tenantData);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTenant(@RequestBody Tenant tenant) {
        Tenant saved = tenantService.save(tenant);
        tenantService.createDefaultServices(saved.getId());
        return ResponseEntity.ok(saved);
    }
}