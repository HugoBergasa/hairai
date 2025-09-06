package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.dto.OpenAIResponse;
import com.peluqueria.recepcionista_virtual.service.*;
import com.peluqueria.recepcionista_virtual.model.Cliente;
import com.peluqueria.recepcionista_virtual.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@RestController
@RequestMapping("/api/twilio")
@Slf4j
public class TwilioController {

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private CitaService citaService;

    @Autowired
    private ClienteRepository clienteRepository; // ✅ AGREGADO: Para mapear teléfono → tenant

    @Value("${default.tenant.id:tenant_demo_001}")
    private String defaultTenantId;

    /**
     * ✅ WEBHOOK PRINCIPAL - CON MAPEO DINÁMICO DE TENANT
     */
    @PostMapping(value = "/webhook",
            consumes = "application/x-www-form-urlencoded",
            produces = "application/xml; charset=UTF-8")
    public ResponseEntity<String> webhookTwilio(@RequestParam Map<String, String> params) {
        try {
            log.info("🔥 WEBHOOK TWILIO RECIBIDO - Parámetros: {}", params);

            String from = params.get("From");
            String body = params.get("Body");
            String callSid = params.get("CallSid");
            String to = params.get("To");

            // VALIDACIÓN BÁSICA
            if (from == null) {
                log.warn("Webhook sin parámetro 'From'");
                return ResponseEntity.ok(generarTwiMLError("Datos incompletos"));
            }

            // ✅ DETERMINAR TENANT_ID DINÁMICAMENTE POR TELÉFONO
            String tenantId = determinarTenantId(from, to);
            log.info("✅ Tenant determinado: {} para llamada desde: {} hacia: {}", tenantId, from, to);

            // ✅ PROCESAR CON OPENAI PERSONALIZADO POR TENANT
            OpenAIResponse respuestaIA = openAIService.procesarMensaje(
                    body != null ? body : "Hola",
                    tenantId,
                    callSid
            );

            String mensaje = respuestaIA.getMensaje();
            log.info("🤖 IA responde para tenant {}: {}", tenantId, mensaje);

            // Si la IA detectó que hay que crear una cita
            if ("CREAR_CITA".equals(respuestaIA.getAccion()) &&
                    respuestaIA.getDatosCita() != null &&
                    respuestaIA.getDatosCita().isCompleto()) {

                try {
                    citaService.crearCita(tenantId, from, respuestaIA.getDatosCita());
                    mensaje += " He confirmado su cita. Recibirá un SMS de confirmación.";
                    log.info("✅ Cita creada exitosamente para tenant: {}", tenantId);
                } catch (Exception e) {
                    log.error("❌ Error creando cita para tenant {}: {}", tenantId, e.getMessage());
                    mensaje += " Hubo un problema al confirmar la cita. Por favor, inténtelo de nuevo.";
                }
            }

            String twimlResponse = generarTwiMLBasico(mensaje);
            log.info("📞 Respuesta TwiML generada exitosamente para tenant: {}", tenantId);

            return ResponseEntity.ok(twimlResponse);

        } catch (Exception e) {
            log.error("❌ ERROR en webhook Twilio: ", e);
            return ResponseEntity.ok(generarTwiMLError("Error técnico temporal"));
        }
    }

    /**
     * ✅ DETERMINAR TENANT ID DINÁMICAMENTE
     * 1. Buscar cliente existente por teléfono → obtener su tenant
     * 2. Si no existe, usar tenant por defecto (o por número destino en futuro)
     */
    private String determinarTenantId(String telefonoFrom, String telefonoTo) {
        try {
            // ESTRATEGIA 1: Buscar cliente existente por teléfono
            List<Cliente> clientes = clienteRepository.findAll();
            for (Cliente cliente : clientes) {
                if (telefonoFrom.equals(cliente.getTelefono())) {
                    String tenantId = cliente.getTenant().getId();
                    log.info("👤 Cliente encontrado - Tenant: {} para teléfono: {}", tenantId, telefonoFrom);
                    return tenantId;
                }
            }

            // ESTRATEGIA 2: Mapeo por número de teléfono destino (futuro)
            // TODO: Implementar mapeo por número Twilio → tenant
            // Map<String, String> phoneToTenant = Map.of(
            //     "+16084707975", "tenant_demo_001",
            //     "+15551234567", "tenant_salon_madrid"
            // );
            // return phoneToTenant.getOrDefault(telefonoTo, defaultTenantId);

            // ESTRATEGIA 3: Usar tenant por defecto para clientes nuevos
            log.info("🆕 Cliente nuevo - usando tenant por defecto: {} para teléfono: {}", defaultTenantId, telefonoFrom);
            return defaultTenantId;

        } catch (Exception e) {
            log.error("❌ Error determinando tenant, usando por defecto: {}", e.getMessage());
            return defaultTenantId;
        }
    }

    /**
     * MÉTODO AUXILIAR - Generar TwiML básico
     */
    private String generarTwiMLBasico(String mensaje) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Response>" +
                "<Say language=\"es-ES\" voice=\"Polly.Conchita\">" +
                mensaje +
                "</Say>" +
                "</Response>";
    }

    /**
     * MÉTODO AUXILIAR - Generar TwiML de error
     */
    private String generarTwiMLError(String error) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Response>" +
                "<Say language=\"es-ES\">Disculpe, " + error + ". Intente más tarde.</Say>" +
                "<Hangup/>" +
                "</Response>";
    }

    // ===== ENDPOINTS EXISTENTES CON MEJORAS MULTI-TENANT =====

    @PostMapping(value = "/voice", produces = "application/xml; charset=UTF-8")
    public String handleIncomingCall(@RequestParam Map<String, String> params) {
        String from = params.get("From");
        String callSid = params.get("CallSid");

        log.info("📞 Nueva llamada de: {} - CallSid: {}", from, callSid);

        // ✅ Determinar tenant para personalizar saludo
        String tenantId = determinarTenantId(from, params.get("To"));

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Response>" +
                "<Gather input=\"speech\" action=\"/api/twilio/process-speech\" " +
                "method=\"POST\" language=\"es-ES\" speechTimeout=\"auto\">" +
                "<Say language=\"es-ES\" voice=\"Polly.Conchita\">" +
                "Hola, bienvenido. Soy su asistente virtual. " +
                "¿Cómo puedo ayudarle?" +
                "</Say>" +
                "</Gather>" +
                "</Response>";
    }

    @PostMapping(value = "/process-speech", produces = "application/xml; charset=UTF-8")
    public String processSpeech(@RequestParam Map<String, String> params) {
        String speechResult = params.get("SpeechResult");
        String callSid = params.get("CallSid");
        String from = params.get("From");
        String to = params.get("To");

        log.info("🎤 Usuario dijo: {}", speechResult);

        try {
            // ✅ DETERMINAR TENANT DINÁMICAMENTE
            String tenantId = determinarTenantId(from, to);

            // ✅ USAR OpenAIService PERSONALIZADO POR TENANT
            OpenAIResponse respuestaIA = openAIService.procesarMensaje(
                    speechResult,
                    tenantId,
                    callSid
            );

            String mensaje = respuestaIA.getMensaje();
            log.info("🤖 IA responde para tenant {}: {}", tenantId, mensaje);

            // Si la IA detectó que hay que crear una cita
            if ("CREAR_CITA".equals(respuestaIA.getAccion()) &&
                    respuestaIA.getDatosCita() != null &&
                    respuestaIA.getDatosCita().isCompleto()) {

                try {
                    citaService.crearCita(tenantId, from, respuestaIA.getDatosCita());
                    mensaje += " He confirmado su cita. Recibirá un SMS de confirmación.";
                } catch (Exception e) {
                    log.error("Error creando cita", e);
                    mensaje += " Hubo un problema al confirmar la cita. Por favor, inténtelo de nuevo.";
                }
            }

            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<Response>" +
                    "<Gather input=\"speech\" action=\"/api/twilio/process-speech\" " +
                    "method=\"POST\" language=\"es-ES\" speechTimeout=\"auto\">" +
                    "<Say language=\"es-ES\" voice=\"Polly.Conchita\">" +
                    mensaje +
                    "</Say>" +
                    "</Gather>" +
                    "</Response>";

        } catch (Exception e) {
            log.error("❌ Error procesando speech", e);

            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<Response>" +
                    "<Say language=\"es-ES\">Disculpe, hubo un problema técnico. Por favor llame más tarde.</Say>" +
                    "<Hangup/>" +
                    "</Response>";
        }
    }

    @PostMapping(value = "/hangup", produces = "application/xml; charset=UTF-8")
    public String handleHangup(@RequestParam Map<String, String> params) {
        String callSid = params.get("CallSid");
        log.info("🔴 Llamada finalizada: {}", callSid);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>";
    }

    /**
     * ✅ ENDPOINT DE TESTING MEJORADO - MULTI-TENANT
     */
    @GetMapping("/test")
    @ResponseBody
    public Map<String, Object> test(@RequestParam(defaultValue = "") String tenantId) {
        try {
            // Usar tenant específico o por defecto
            String testTenantId = tenantId.isEmpty() ? defaultTenantId : tenantId;

            OpenAIResponse test = openAIService.procesarMensaje(
                    "Hola, quiero una cita",
                    testTenantId,
                    "test-" + System.currentTimeMillis()
            );

            return Map.of(
                    "status", "OK",
                    "openai_connected", true,
                    "response", test.getMensaje(),
                    "tenant_used", testTenantId,
                    "mapping_strategy", "dynamic_by_phone"
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "ERROR",
                    "error", e.getMessage(),
                    "tenant_used", tenantId.isEmpty() ? defaultTenantId : tenantId
            );
        }
    }

    /**
     * ✅ NUEVO ENDPOINT - TESTING MAPEO DE TENANT
     */
    @GetMapping("/test-tenant-mapping")
    @ResponseBody
    public Map<String, Object> testTenantMapping(@RequestParam String telefono) {
        try {
            String tenantId = determinarTenantId(telefono, null);

            return Map.of(
                    "telefono", telefono,
                    "tenant_mapped", tenantId,
                    "strategy", "cliente_lookup",
                    "fallback", defaultTenantId
            );
        } catch (Exception e) {
            return Map.of(
                    "error", e.getMessage(),
                    "telefono", telefono
            );
        }
    }
}