package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.model.Cita;
import com.peluqueria.recepcionista_virtual.model.EstadoCita;
import com.peluqueria.recepcionista_virtual.service.CitaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/citas")
@CrossOrigin
public class CitasController {

    @Autowired
    private CitaService citaService;

    @GetMapping("/hoy")
    public ResponseEntity<?> getCitasHoy(@RequestAttribute(required = false) String tenantId) {
        if (tenantId == null) {
            // Devolver lista vacía si no hay autenticación
            return ResponseEntity.ok(List.of());
        }

        List<Cita> citas = citaService.obtenerCitasDelDia(tenantId);
        return ResponseEntity.ok(citas);
    }

    @PostMapping
    public ResponseEntity<?> crearCita(
            @RequestAttribute(required = false) String tenantId,
            @RequestBody Map<String, Object> citaData
    ) {
        if (tenantId == null) {
            return ResponseEntity.badRequest().body("No autorizado");
        }

        // TODO: Implementar creación de cita desde el frontend
        return ResponseEntity.ok(Map.of("mensaje", "Cita creada"));
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarCita(
            @PathVariable String id,
            @RequestAttribute(required = false) String tenantId
    ) {
        if (tenantId == null) {
            return ResponseEntity.badRequest().body("No autorizado");
        }

        citaService.cancelarCita(id);
        return ResponseEntity.ok(Map.of("mensaje", "Cita cancelada"));
    }
}