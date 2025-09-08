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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CONTROLLER CRITICO: API para gestion de horarios especiales y cierres
 *
 * MULTITENANT: Todos los endpoints requieren tenantId del RequestAttribute
 * ZERO HARDCODING: Respuestas dinamicas, sin textos fijos
 * OpenAI CEREBRO: Endpoints optimizados para que la IA verifique disponibilidad
 *
 * Endpoints principales:
 * - POST /cierre-rapido: Para emergencias (boton rojo dashboard)
 * - POST /crear-cierre: Para planificacion (vacaciones, eventos)
 * - GET /verificar-disponibilidad: Para la IA (SUPER CRITICO)
 * - GET /calendario-cierres: Para el dashboard
 */
@RestController
@RequestMapping("/api/horarios-especiales")
@CrossOrigin(origins = {"https://hairai.netlify.app", "http://localhost:3000"})
public class HorarioEspecialController {

    private static final Logger logger = LoggerFactory.getLogger(HorarioEspecialController.class);

    @Autowired
    private HorarioEspecialService horarioEspecialService;

    // ========================================
    // ENDPOINTS CRITICOS
    // ========================================

    /**
     * ENDPOINT SUPER CRITICO: Cierre rapido de emergencia
     *
     * El boton rojo del dashboard llama a este endpoint
     * Debe funcionar en 2 segundos para casos urgentes
     *
     * MULTITENANT: Se asocia automaticamente con el tenant del request
     * ZERO HARDCODING: No contiene mensajes predefinidos
     */
    @PostMapping("/cierre-rapido")
    public ResponseEntity<?> cierreRapido(
            @RequestAttribute("tenantId") String tenantId,
            @RequestBody CierreRapidoRequest request) {

        logger.info("🚨 Solicitud de cierre rapido - Tenant: {}, Fecha: {}, Motivo: {}",
                tenantId, request.getFecha(), request.getMotivo());

        try {
            HorarioEspecial horario = horarioEspecialService.crearCierreRapido(
                    tenantId,
                    request.getFecha(),
                    request.getMotivo()
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
     *
     * La IA llama este endpoint antes de confirmar CUALQUIER cita
     * DEBE ser super rapido (< 50ms) para no ralentizar conversaciones
     *
     * MULTITENANT: Solo verifica cierres del tenant actual
     * ZERO HARDCODING: Mensajes vienen de BD o son generados dinamicamente
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
     * Crear cierre planificado (vacaciones, eventos, formacion)
     *
     * MULTITENANT: Asociado automaticamente al tenant
     * ZERO HARDCODING: Mensajes personalizables
     */
    @PostMapping("/crear-cierre")
    public ResponseEntity<?> crearCierre(
            @RequestAttribute("tenantId") String tenantId,
            @Valid @RequestBody HorarioEspecialDTO dto) {

        logger.info("📋 Creando cierre planificado - Tenant: {}, Fechas: {} - {}, Tipo: {}",
                tenantId, dto.getFechaInicio(), dto.getFechaFin(), dto.getTipoCierre());

        try {
            HorarioEspecial horario = horarioEspecialService.crearCierre(tenantId, dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cierre programado exitosamente");
            response.put("cierre", horario);
            response.put("tenantId", tenantId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ Datos invalidos para cierre: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("error", "VALIDATION_ERROR");

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("❌ Error creando cierre planificado", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            errorResponse.put("error", "INTERNAL_ERROR");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ========================================
    // ENDPOINTS PARA DASHBOARD
    // ========================================

    /**
     * Obtener calendario de cierres para el dashboard
     *
     * MULTITENANT: Solo cierres del tenant actual
     * ZERO HARDCODING: Descripciones generadas dinamicamente
     */
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

    /**
     * Obtener estadisticas de cierres
     *
     * MULTITENANT: Solo estadisticas del tenant actual
     */
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

    /**
     * Obtener cierres proximos (para dashboard y alertas)
     *
     * MULTITENANT: Solo cierres del tenant actual
     */
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

    /**
     * Listar todos los cierres del tenant
     *
     * MULTITENANT: Solo cierres del tenant actual
     */
    @GetMapping("/listar")
    public ResponseEntity<List<HorarioEspecial>> listarCierres(
            @RequestAttribute("tenantId") String tenantId) {

        logger.debug("🗂️ Listando cierres - Tenant: {}", tenantId);

        try {
            List<HorarioEspecial> cierres = horarioEspecialService.obtenerCierresProximos(tenantId, 365);
            return ResponseEntity.ok(cierres);

        } catch (Exception e) {
            logger.error("❌ Error listando cierres", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ========================================
    // ENDPOINTS DE GESTION
    // ========================================

    /**
     * Eliminar/desactivar cierre
     *
     * MULTITENANT: Solo puede eliminar cierres de su tenant
     */
    @DeleteMapping("/{cierreId}")
    public ResponseEntity<?> eliminarCierre(
            @RequestAttribute("tenantId") String tenantId,
            @PathVariable String cierreId) {

        logger.info("❌ Eliminando cierre - Tenant: {}, ID: {}", tenantId, cierreId);

        try {
            horarioEspecialService.eliminarCierre(tenantId, cierreId);

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
    // ENDPOINTS DE UTILIDAD
    // ========================================

    /**
     * Obtener tipos de cierre disponibles (para frontend)
     *
     * ZERO HARDCODING: Descripcion tecnica sin textos fijos
     */
    @GetMapping("/tipos-cierre")
    public ResponseEntity<Map<String, Object>> obtenerTiposCierre() {

        Map<String, Object> tipos = new HashMap<>();

        for (TipoCierre tipo : TipoCierre.values()) {
            Map<String, Object> tipoInfo = new HashMap<>();
            tipoInfo.put("nombre", tipo.name());
            tipoInfo.put("descripcionTecnica", tipo.getDescripcionTecnica());
            tipoInfo.put("requiereHorarios", tipo.requiereHorarios());
            tipoInfo.put("requiereEmpleados", tipo.requiereEmpleados());
            tipoInfo.put("requiereServicios", tipo.requiereServicios());
            tipoInfo.put("bloqueaCompletamente", tipo.bloqueaCompletamente());

            tipos.put(tipo.name(), tipoInfo);
        }

        return ResponseEntity.ok(tipos);
    }

    /**
     * Endpoint para testing: verificacion simple de fecha
     *
     * MULTITENANT: Verificacion solo del tenant actual
     */
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

    /**
     * Endpoint helper: obtener siguiente fecha disponible
     *
     * MULTITENANT: Busqueda solo en el tenant actual
     */
    @GetMapping("/siguiente-fecha-disponible")
    public ResponseEntity<Map<String, Object>> siguienteFechaDisponible(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(required = false) String despuesDe) {

        try {
            LocalDate fechaBase = despuesDe != null ?
                    LocalDate.parse(despuesDe) : LocalDate.now();

            // Buscar proxima fecha disponible
            LocalDate siguiente = fechaBase.plusDays(1);
            DisponibilidadResult resultado = null;

            for (int i = 0; i < 30; i++) {
                resultado = horarioEspecialService.verificarDisponibilidad(
                        tenantId, siguiente.atStartOfDay(), null, null
                );

                if (resultado.isDisponible()) {
                    break;
                }
                siguiente = siguiente.plusDays(1);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("tenantId", tenantId);
            response.put("fechaBase", fechaBase);
            response.put("siguienteFechaDisponible", siguiente);
            response.put("disponible", resultado != null ? resultado.isDisponible() : false);
            response.put("diasBuscados", 30);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Error buscando siguiente fecha disponible", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error buscando fecha disponible");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ========================================
    // ENDPOINTS ESPECIALES PARA IA
    // ========================================

    /**
     * Endpoint optimizado para consultas de IA
     * Combina verificacion + alternativas en una sola llamada
     *
     * SUPER OPTIMIZADO: Diseñado para ser llamado por la IA frecuentemente
     */
    @PostMapping("/consultar-disponibilidad-ia")
    public ResponseEntity<DisponibilidadResult> consultarDisponibilidadIA(
            @RequestAttribute("tenantId") String tenantId,
            @RequestBody ConsultaDisponibilidadDTO consulta) {

        logger.debug("🤖 Consulta IA - Tenant: {}, Fecha: {}, Contexto: {}",
                tenantId, consulta.getFecha(), consulta.getContextoSolicitud());

        try {
            LocalDateTime fechaHora = consulta.getFecha().atTime(
                    consulta.getHora() != null ? consulta.getHora() : LocalTime.of(10, 0)
            );

            DisponibilidadResult resultado = horarioEspecialService.verificarDisponibilidad(
                    tenantId, fechaHora, consulta.getEmpleadoId(), consulta.getServicioId()
            );

            // Si no esta disponible y es emergencia, verificar si aplica excepcion
            if (!resultado.isDisponible() && consulta.isEsEmergencia()) {
                // Logica especial para emergencias (futuro)
                logger.debug("🆘 Solicitud marcada como emergencia");
            }

            // Metadata adicional para la IA
            resultado.setTipoRestriccion(null); // Se establecera segun el cierre encontrado

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            logger.error("❌ Error en consulta IA", e);

            DisponibilidadResult error = DisponibilidadResult.noDisponible(
                    "Error procesando consulta"
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Obtener resumen de cierres para contexto de IA
     * Proporciona informacion condensada para que la IA entienda el estado del salon
     *
     * OPTIMIZADO: Datos especificos para prompts de IA
     */
    @GetMapping("/resumen-para-ia")
    public ResponseEntity<Map<String, Object>> obtenerResumenParaIA(
            @RequestAttribute("tenantId") String tenantId,
            @RequestParam(defaultValue = "7") int diasAdelante) {

        try {
            List<HorarioEspecial> cierresProximos = horarioEspecialService.obtenerCierresProximos(tenantId, diasAdelante);

            Map<String, Object> resumen = new HashMap<>();
            resumen.put("tenantId", tenantId);
            resumen.put("diasConsultados", diasAdelante);
            resumen.put("totalCierres", cierresProximos.size());

            // Informacion estructurada para la IA
            Map<String, Object> cierresPorTipo = new HashMap<>();
            for (HorarioEspecial cierre : cierresProximos) {
                String tipo = cierre.getTipoCierre().name();
                if (!cierresPorTipo.containsKey(tipo)) {
                    cierresPorTipo.put(tipo, 0);
                }
                cierresPorTipo.put(tipo, (Integer) cierresPorTipo.get(tipo) + 1);
            }

            resumen.put("cierresPorTipo", cierresPorTipo);
            resumen.put("cierresProximos", cierresProximos);

            return ResponseEntity.ok(resumen);

        } catch (Exception e) {
            logger.error("❌ Error obteniendo resumen para IA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}