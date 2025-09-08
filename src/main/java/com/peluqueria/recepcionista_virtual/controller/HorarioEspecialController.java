package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.dto.*;
import com.peluqueria.recepcionista_virtual.model.HorarioEspecial;
import com.peluqueria.recepcionista_virtual.model.TipoCierre;
import com.peluqueria.recepcionista_virtual.service.HorarioEspecialService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CONTROLLER CRITICO CORREGIDO: API para gestion de horarios especiales
 *
 * MULTITENANT: Todos los endpoints requieren tenantId del RequestAttribute
 * ZERO HARDCODING: Respuestas dinamicas, sin textos fijos
 * OpenAI CEREBRO: Endpoints optimizados para que la IA verifique disponibilidad
 */
@RestController
@RequestMapping("/api/horarios-especiales")
// ❌ ELIMINADO: @CrossOrigin hardcodeado - usar CorsConfig global
public class HorarioEspecialController {

    private static final Logger logger = LoggerFactory.getLogger(HorarioEspecialController.class);

    @Autowired
    private HorarioEspecialService horarioEspecialService;

    // ========================================
    // ENDPOINTS CRITICOS - CORREGIDOS
    // ========================================

    /**
     * ENDPOINT SUPER CRITICO CORREGIDO: Cierre rapido de emergencia
     * VALIDACION AGREGADA: Fechas futuras + usuarioId
     */
    @PostMapping("/cierre-rapido")
    public ResponseEntity<?> cierreRapido(
            @RequestAttribute("tenantId") String tenantId,
            @RequestAttribute(value = "usuarioId", required = false) String usuarioId,
            @RequestBody CierreRapidoRequest request) {

        logger.info("🚨 Solicitud de cierre rapido - Tenant: {}, Usuario: {}, Fecha: {}, Motivo: {}",
                tenantId, usuarioId, request.getFecha(), request.getMotivo());

        try {
            // VALIDACIÓN CRÍTICA: Fechas futuras
            if (request.getFecha().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No se pueden crear cierres en fechas pasadas"));
            }

            // BD COMPATIBLE: Usar usuarioId o fallback
            String usuario = usuarioId != null ? usuarioId : "sistema";

            HorarioEspecial horario = horarioEspecialService.crearCierreRapido(
                    tenantId,
                    request.getFecha(),
                    request.getMotivo(),
                    usuario
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Salon cerrado exitosamente para " + request.getFecha());
            response.put("cierre", horario);
            response.put("fechaAfectada", request.getFecha());
            response.put("tenantId", tenantId);

            logger.info("✅ Cierre rapido creado exitosamente: {}", horario.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error creando cierre rapido", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error creando cierre: " + e.getMessage());
            errorResponse.put("error", "CIERRE_RAPIDO_ERROR");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * ENDPOINT CRITICO: Verificar disponibilidad (usado por la IA)
     * SIN CAMBIOS - funciona correctamente
     */
    @GetMapping("/verificar-disponibilidad")
    public ResponseEntity<DisponibilidadResult> verificarDisponibilidad(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam String fecha,
            @RequestParam String hora,
            @RequestParam(required = false) String empleadoId,
            @RequestParam(required = false) String servicioId) {

        logger.debug("🔍 Verificando disponibilidad - Tenant: {}, Fecha: {}, Hora: {}",
                tenantId, fecha, hora);

        try {
            LocalDateTime fechaHora = LocalDateTime.parse(fecha + "T" + hora);

            DisponibilidadResult resultado = horarioEspecialService.verificarDisponibilidad(
                    tenantId, fechaHora, empleadoId, servicioId
            );

            logger.debug("✅ Resultado disponibilidad: {}", resultado.isDisponible());

            return ResponseEntity.ok(resultado);

        } catch (DateTimeParseException e) {
            logger.warn("⚠️ Formato de fecha/hora invalido: {} - {}", fecha, hora);

            DisponibilidadResult error = DisponibilidadResult.noDisponible(
                    "Formato de fecha u hora invalido"
            );

            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            logger.error("❌ Error verificando disponibilidad", e);

            DisponibilidadResult error = DisponibilidadResult.noDisponible(
                    "Error interno verificando disponibilidad"
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * CORREGIDO: Crear cierre con verificación + usuarioId
     */
    @PostMapping("/crear-cierre")
    public ResponseEntity<?> crearCierre(
            @RequestAttribute("tenantId") String tenantId,
            @RequestAttribute(value = "usuarioId", required = false) String usuarioId,
            @Valid @RequestBody HorarioEspecialDTO dto,
            @RequestParam(defaultValue = "false") boolean forzar) {

        logger.info("Creando cierre - Tenant: {}, Usuario: {}, Fechas: {} - {}, Forzar: {}",
                tenantId, usuarioId, dto.getFechaInicio(), dto.getFechaFin(), forzar);

        try {
            // BD COMPATIBLE: Usar usuarioId o fallback
            String usuario = usuarioId != null ? usuarioId : "usuario";

            Object resultado = horarioEspecialService.crearCierreConVerificacion(
                    tenantId, dto, forzar, usuario
            );

            // Si el resultado es ResultadoVerificacionCitas, hay citas afectadas
            if (resultado instanceof ResultadoVerificacionCitas) {
                ResultadoVerificacionCitas verificacion = (ResultadoVerificacionCitas) resultado;

                Map<String, Object> response = new HashMap<>();
                response.put("requiereConfirmacion", true);
                response.put("citasAfectadas", verificacion.getNumeroCitasAfectadas());
                response.put("mensaje", verificacion.getMensajeAviso());
                response.put("citas", verificacion.getCitasAfectadas());
                response.put("accion", "Para confirmar el cierre, reenvie la peticion con parametro forzar=true");

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            // Si el resultado es HorarioEspecial, el cierre se creó exitosamente
            HorarioEspecial horario = (HorarioEspecial) resultado;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", forzar ?
                    "Cierre creado y notificaciones enviadas automaticamente" :
                    "Cierre programado exitosamente");
            response.put("cierre", horario);
            response.put("tenantId", tenantId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Datos invalidos para cierre: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("error", "VALIDATION_ERROR");

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("Error creando cierre", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("error", "INTERNAL_ERROR");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * CORREGIDO: Eliminar cierre con usuarioId
     */
    @DeleteMapping("/{cierreId}")
    public ResponseEntity<?> eliminarCierre(
            @RequestAttribute("tenantId") String tenantId,
            @RequestAttribute(value = "usuarioId", required = false) String usuarioId,
            @PathVariable String cierreId) {

        logger.info("❌ Eliminando cierre - Tenant: {}, Usuario: {}, ID: {}", tenantId, usuarioId, cierreId);

        try {
            // BD COMPATIBLE: Usar usuarioId o fallback
            String usuario = usuarioId != null ? usuarioId : "usuario";

            horarioEspecialService.eliminarCierre(tenantId, cierreId, usuario);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cierre eliminado exitosamente");
            response.put("cierreId", cierreId);
            response.put("tenantId", tenantId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ Cierre no encontrado: {}", cierreId);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("error", "CIERRE_NOT_FOUND");

            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("❌ Error eliminando cierre", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("error", "INTERNAL_ERROR");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ========================================
    // ENDPOINTS PARA DASHBOARD - SIN CAMBIOS
    // ========================================

    @GetMapping("/calendario-cierres")
    public ResponseEntity<List<CalendarioCierreDTO>> obtenerCalendarioCierres(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam String mesAno) {

        logger.debug("📅 Obteniendo calendario - Tenant: {}, Mes: {}", tenantId, mesAno);

        try {
            List<CalendarioCierreDTO> calendario = horarioEspecialService.obtenerCalendarioCierres(tenantId, mesAno);
            return ResponseEntity.ok(calendario);

        } catch (Exception e) {
            logger.error("❌ Error obteniendo calendario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasCierreDTO> obtenerEstadisticas(
            @RequestAttribute("tenantId") String tenantId) {

        logger.debug("📊 Obteniendo estadisticas - Tenant: {}", tenantId);

        try {
            EstadisticasCierreDTO stats = horarioEspecialService.obtenerEstadisticas(tenantId);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("❌ Error obteniendo estadisticas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/proximos")
    public ResponseEntity<List<HorarioEspecial>> obtenerCierresProximos(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(defaultValue = "30") int dias) {

        logger.debug("🔮 Obteniendo cierres proximos - Tenant: {}, Dias: {}", tenantId, dias);

        try {
            List<HorarioEspecial> proximos = horarioEspecialService.obtenerCierresProximos(tenantId, dias);
            return ResponseEntity.ok(proximos);

        } catch (Exception e) {
            logger.error("❌ Error obteniendo cierres proximos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/tipos-cierre")
    public ResponseEntity<Map<String, Object>> obtenerTiposCierre() {
        Map<String, Object> tipos = new HashMap<>();

        for (TipoCierre tipo : TipoCierre.values()) {
            Map<String, Object> tipoInfo = new HashMap<>();
            tipoInfo.put("nombre", tipo.name());
            // BD COMPATIBLE: Solo propiedades que existen
            tipos.put(tipo.name(), tipoInfo);
        }

        return ResponseEntity.ok(tipos);
    }

    @GetMapping("/test-fecha-disponible")
    public ResponseEntity<Map<String, Object>> testFechaDisponible(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam String fecha) {

        try {
            LocalDate fechaTest = LocalDate.parse(fecha);

            DisponibilidadResult resultado = horarioEspecialService.verificarDisponibilidad(
                    tenantId, fechaTest.atStartOfDay(), null, null
            );

            Map<String, Object> response = new HashMap<>();
            response.put("tenantId", tenantId);
            response.put("fecha", fecha);
            response.put("disponible", resultado.isDisponible());
            response.put("mensaje", resultado.getMensaje());
            response.put("fechasAlternativas", resultado.getFechasAlternativas());

            return ResponseEntity.ok(response);

        } catch (DateTimeParseException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Formato de fecha invalido. Use: YYYY-MM-DD");
            errorResponse.put("fechaRecibida", fecha);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}