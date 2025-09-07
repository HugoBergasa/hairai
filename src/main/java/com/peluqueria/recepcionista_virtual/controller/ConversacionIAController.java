package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.model.ConversacionIA;
import com.peluqueria.recepcionista_virtual.service.ConversacionIAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/conversaciones")
public class ConversacionIAController {

    @Autowired
    private ConversacionIAService conversacionIAService;

    /**
     * Obtener todas las conversaciones del tenant autenticado
     */
    @GetMapping
    public ResponseEntity<List<ConversacionIA>> getConversaciones(
            @RequestAttribute(required = true) String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > 0) {
            Pageable pageable = PageRequest.of(page, size);
            Page<ConversacionIA> conversacionesPage = conversacionIAService.getConversacionesByTenantId(tenantId, pageable);
            return ResponseEntity.ok(conversacionesPage.getContent());
        } else {
            List<ConversacionIA> conversaciones = conversacionIAService.getConversacionesByTenantId(tenantId);
            return ResponseEntity.ok(conversaciones);
        }
    }

    /**
     * Obtener conversación específica por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConversacionIA> getConversacionById(
            @PathVariable String id,
            @RequestAttribute(required = true) String tenantId) {

        ConversacionIA conversacion = conversacionIAService.getConversacionById(id, tenantId);
        if (conversacion != null) {
            return ResponseEntity.ok(conversacion);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Crear nueva conversación
     */
    @PostMapping
    public ResponseEntity<ConversacionIA> crearConversacion(
            @RequestBody ConversacionIA conversacion,
            @RequestAttribute(required = true) String tenantId) {

        conversacion.setTenantId(tenantId);
        ConversacionIA nuevaConversacion = conversacionIAService.guardarConversacion(conversacion);
        return ResponseEntity.ok(nuevaConversacion);
    }

    /**
     * Eliminar conversación
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarConversacion(
            @PathVariable String id,
            @RequestAttribute(required = true) String tenantId) {

        conversacionIAService.eliminarConversacion(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener conversaciones por CallSid
     */
    @GetMapping("/call/{callSid}")
    public ResponseEntity<List<ConversacionIA>> getConversacionesByCallSid(
            @PathVariable String callSid,
            @RequestAttribute(required = true) String tenantId) {

        List<ConversacionIA> conversaciones = conversacionIAService.getConversacionesByCallSid(callSid, tenantId);
        return ResponseEntity.ok(conversaciones);
    }

    /**
     * Obtener conversaciones por canal
     */
    @GetMapping("/canal/{canal}")
    public ResponseEntity<List<ConversacionIA>> getConversacionesByCanal(
            @PathVariable String canal,
            @RequestAttribute(required = true) String tenantId) {

        try {
            ConversacionIA.CanalComunicacion canalEnum = ConversacionIA.CanalComunicacion.valueOf(canal.toUpperCase());
            List<ConversacionIA> conversaciones = conversacionIAService.getConversacionesByCanal(tenantId, canalEnum);
            return ResponseEntity.ok(conversaciones);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtener estadísticas de conversaciones IA
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEstadisticas(
            @RequestAttribute(required = true) String tenantId) {

        Map<String, Object> stats = new HashMap<>();

        stats.put("totalConversaciones", conversacionIAService.getTotalConversaciones(tenantId));
        stats.put("conversacionesExitosas", conversacionIAService.getConversacionesExitosas(tenantId));

        // Estadísticas por canal
        Map<String, Long> porCanal = new HashMap<>();
        for (ConversacionIA.CanalComunicacion canal : ConversacionIA.CanalComunicacion.values()) {
            porCanal.put(canal.name(), conversacionIAService.getConversacionesPorCanal(tenantId, canal));
        }
        stats.put("porCanal", porCanal);

        return ResponseEntity.ok(stats);
    }

    /**
     * Buscar conversaciones por fecha
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<ConversacionIA>> buscarConversaciones(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestAttribute(required = true) String tenantId) {

        try {
            LocalDateTime inicio = fechaInicio != null ? LocalDateTime.parse(fechaInicio) : LocalDateTime.now().minusDays(7);
            LocalDateTime fin = fechaFin != null ? LocalDateTime.parse(fechaFin) : LocalDateTime.now();

            List<ConversacionIA> conversaciones = conversacionIAService.getConversacionesByFecha(tenantId, inicio, fin);
            return ResponseEntity.ok(conversaciones);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}