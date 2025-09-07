package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.model.LogLlamada;
import com.peluqueria.recepcionista_virtual.service.LogLlamadaService;
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
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/logs/llamadas")
public class LogLlamadaController {

    @Autowired
    private LogLlamadaService logLlamadaService;

    /**
     * Obtener todas las llamadas del tenant autenticado
     */
    @GetMapping
    public ResponseEntity<List<LogLlamada>> getLlamadas(
            @RequestAttribute(required = true) String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (size > 0) {
            Pageable pageable = PageRequest.of(page, size);
            Page<LogLlamada> llamadasPage = logLlamadaService.getLlamadasByTenantId(tenantId, pageable);
            return ResponseEntity.ok(llamadasPage.getContent());
        } else {
            List<LogLlamada> llamadas = logLlamadaService.getLlamadasByTenantId(tenantId);
            return ResponseEntity.ok(llamadas);
        }
    }

    /**
     * Obtener llamada específica por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<LogLlamada> getLlamadaById(
            @PathVariable String id,
            @RequestAttribute(required = true) String tenantId) {

        LogLlamada llamada = logLlamadaService.getLlamadaById(id, tenantId);
        if (llamada != null) {
            return ResponseEntity.ok(llamada);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Crear nueva llamada
     */
    @PostMapping
    public ResponseEntity<LogLlamada> crearLlamada(
            @RequestBody LogLlamada llamada,
            @RequestAttribute(required = true) String tenantId) {

        llamada.setTenantId(tenantId);
        LogLlamada nuevaLlamada = logLlamadaService.guardarLlamada(llamada);
        return ResponseEntity.ok(nuevaLlamada);
    }

    /**
     * Actualizar estado de llamada por CallSid
     */
    @PutMapping("/{callSid}/estado")
    public ResponseEntity<LogLlamada> actualizarEstado(
            @PathVariable String callSid,
            @RequestBody Map<String, String> request,
            @RequestAttribute(required = true) String tenantId) {

        String nuevoEstado = request.get("estado");
        if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        LogLlamada llamada = logLlamadaService.actualizarEstado(callSid, tenantId, nuevoEstado);
        if (llamada != null) {
            return ResponseEntity.ok(llamada);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Finalizar llamada por CallSid
     */
    @PutMapping("/{callSid}/finalizar")
    public ResponseEntity<LogLlamada> finalizarLlamada(
            @PathVariable String callSid,
            @RequestAttribute(required = true) String tenantId) {

        LogLlamada llamada = logLlamadaService.finalizarLlamada(callSid, tenantId);
        if (llamada != null) {
            return ResponseEntity.ok(llamada);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Agregar transcripción a llamada
     */
    @PutMapping("/{callSid}/transcripcion")
    public ResponseEntity<LogLlamada> agregarTranscripcion(
            @PathVariable String callSid,
            @RequestBody Map<String, String> request,
            @RequestAttribute(required = true) String tenantId) {

        String transcripcion = request.get("transcripcion");
        if (transcripcion == null || transcripcion.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        LogLlamada llamada = logLlamadaService.agregarTranscripcion(callSid, tenantId, transcripcion);
        if (llamada != null) {
            return ResponseEntity.ok(llamada);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtener llamadas por estado
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<LogLlamada>> getLlamadasByEstado(
            @PathVariable String estado,
            @RequestAttribute(required = true) String tenantId) {

        List<LogLlamada> llamadas = logLlamadaService.getLlamadasByEstado(tenantId, estado);
        return ResponseEntity.ok(llamadas);
    }

    /**
     * Obtener llamadas por dirección (entrantes/salientes)
     */
    @GetMapping("/direccion/{direccion}")
    public ResponseEntity<List<LogLlamada>> getLlamadasByDireccion(
            @PathVariable String direccion,
            @RequestAttribute(required = true) String tenantId) {

        List<LogLlamada> llamadas = logLlamadaService.getLlamadasByDireccion(tenantId, direccion);
        return ResponseEntity.ok(llamadas);
    }

    /**
     * Obtener llamadas por número de origen
     */
    @GetMapping("/numero/{numeroOrigen}")
    public ResponseEntity<List<LogLlamada>> getLlamadasByNumero(
            @PathVariable String numeroOrigen,
            @RequestAttribute(required = true) String tenantId) {

        List<LogLlamada> llamadas = logLlamadaService.getLlamadasByNumeroOrigen(tenantId, numeroOrigen);
        return ResponseEntity.ok(llamadas);
    }

    /**
     * Vincular llamada con cita creada
     */
    @PutMapping("/{callSid}/cita")
    public ResponseEntity<LogLlamada> vincularConCita(
            @PathVariable String callSid,
            @RequestBody Map<String, String> request,
            @RequestAttribute(required = true) String tenantId) {

        String citaId = request.get("citaId");
        if (citaId == null || citaId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        LogLlamada llamada = logLlamadaService.vincularConCita(callSid, tenantId, citaId);
        if (llamada != null) {
            return ResponseEntity.ok(llamada);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Obtener estadísticas de llamadas
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEstadisticas(
            @RequestAttribute(required = true) String tenantId) {

        Map<String, Object> stats = new HashMap<>();

        stats.put("totalLlamadas", logLlamadaService.getTotalLlamadas(tenantId));
        stats.put("llamadasEntrantes", logLlamadaService.getLlamadasEntrantes(tenantId));
        stats.put("llamadasSalientes", logLlamadaService.getLlamadasSalientes(tenantId));
        stats.put("llamadasCompletadas", logLlamadaService.getLlamadasCompletadas(tenantId));
        stats.put("llamadasConCita", logLlamadaService.getLlamadasConCitaCreada(tenantId));

        BigDecimal costoTotal = logLlamadaService.getCostoTotalLlamadas(tenantId);
        stats.put("costoTotal", costoTotal != null ? costoTotal : BigDecimal.ZERO);

        Long duracionTotal = logLlamadaService.getDuracionTotalLlamadas(tenantId);
        stats.put("duracionTotalSegundos", duracionTotal != null ? duracionTotal : 0L);

        return ResponseEntity.ok(stats);
    }

    /**
     * Buscar llamadas por fecha
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<LogLlamada>> buscarLlamadas(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestAttribute(required = true) String tenantId) {

        try {
            LocalDateTime inicio = fechaInicio != null ? LocalDateTime.parse(fechaInicio) : LocalDateTime.now().minusDays(7);
            LocalDateTime fin = fechaFin != null ? LocalDateTime.parse(fechaFin) : LocalDateTime.now();

            List<LogLlamada> llamadas = logLlamadaService.getLlamadasByFecha(tenantId, inicio, fin);
            return ResponseEntity.ok(llamadas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}