package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.model.Servicio;
import com.peluqueria.recepcionista_virtual.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    @Autowired
    private ServicioRepository servicioRepository;

    /**
     * 💼 OBTENER SERVICIOS DEL TENANT ACTUAL - MULTITENANT SIN HARDCODING
     */
    @GetMapping
    public ResponseEntity<?> getServiciosByTenant(HttpServletRequest request) {
        try {
            // Obtener tenant ID del header (multitenant)
            String tenantId = (String) request.getAttribute("tenantId");

            if (tenantId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No autorizado - tenantId requerido"));
            }

            // Obtener servicios activos del tenant
            List<Servicio> servicios = servicioRepository.findByTenantIdAndActivoTrue(tenantId);

            return ResponseEntity.ok(Map.of(
                    "data", servicios,
                    "total", servicios.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error obteniendo servicios: " + e.getMessage()));
        }
    }

    /**
     * 🔍 OBTENER SERVICIO POR ID
     */
    @GetMapping("/{servicioId}")
    public ResponseEntity<?> getServicioById(@PathVariable String servicioId, HttpServletRequest request) {
        try {
            String tenantId = (String) request.getAttribute("tenantId");

            if (tenantId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No autorizado - tenantId requerido"));
            }

            Servicio servicio = servicioRepository.findById(servicioId)
                    .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

            // Verificar que el servicio pertenece al tenant
            if (!servicio.getTenant().getId().equals(tenantId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Servicio no pertenece al tenant"));
            }

            return ResponseEntity.ok(servicio);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error obteniendo servicio: " + e.getMessage()));
        }
    }
}