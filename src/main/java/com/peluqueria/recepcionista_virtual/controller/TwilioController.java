package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.dto.OpenAIResponse;
import com.peluqueria.recepcionista_virtual.service.*;
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

    @Value("${default.tenant.id:tenant_demo_001}")
    private String defaultTenantId;

    /**
     * 🚨 ENDPOINT FALTANTE - Causa del error 500
     * Este es el webhook que Twilio está llamando
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

            // DETERMINAR TENANT_ID (por ahora usar default)
            String tenantId = defaultTenantId;
            log.info("Usando tenant: {} para número: {}", tenantId, to);

            // RESPUESTA BÁSICA DE PRUEBA (sin IA por ahora)
            String mensaje = "Hola, gracias por contactar con nosotros. " +
                    "Su mensaje ha sido recibido correctamente. " +
                    "Un momento, por favor.";

            String twimlResponse = generarTwiMLBasico(mensaje);
            log.info("Respuesta TwiML generada exitosamente");

            return ResponseEntity.ok(twimlResponse);

        } catch (Exception e) {
            log.error("❌ ERROR en webhook Twilio: ", e);
            return ResponseEntity.ok(generarTwiMLError("Error técnico temporal"));
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

    // ===== ENDPOINTS EXISTENTES (mantener tal como están) =====

    @PostMapping(value = "/voice", produces = "application/xml; charset=UTF-8")
    public String handleIncomingCall(@RequestParam Map<String, String> params) {
        String from = params.get("From");
        String callSid = params.get("CallSid");

        log.info("📞 Nueva llamada de: {} - CallSid: {}", from, callSid);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Response>" +
                "<Gather input=\"speech\" action=\"/api/twilio/process-speech\" " +
                "method=\"POST\" language=\"es-ES\" speechTimeout=\"auto\">" +
                "<Say language=\"es-ES\" voice=\"Polly.Conchita\">" +
                "Hola, bienvenido a Peluquería Style. Soy su asistente virtual. " +
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

        log.info("🎤 Usuario dijo: {}", speechResult);

        try {
            // USAR OpenAIService EXISTENTE (cuando esté completo)
            OpenAIResponse respuestaIA = openAIService.procesarMensaje(
                    speechResult,
                    defaultTenantId,
                    callSid
            );

            String mensaje = respuestaIA.getMensaje();
            log.info("🤖 IA responde: {}", mensaje);

            // Si la IA detectó que hay que crear una cita
            if ("CREAR_CITA".equals(respuestaIA.getAccion()) &&
                    respuestaIA.getDatosCita() != null &&
                    respuestaIA.getDatosCita().isCompleto()) {

                try {
                    citaService.crearCita(
                            defaultTenantId,
                            from,
                            respuestaIA.getDatosCita()
                    );
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

    @GetMapping("/test")
    @ResponseBody
    public Map<String, Object> test() {
        try {
            OpenAIResponse test = openAIService.procesarMensaje(
                    "Hola, quiero una cita",
                    defaultTenantId,
                    "test-" + System.currentTimeMillis()
            );

            return Map.of(
                    "status", "OK",
                    "openai_connected", true,
                    "response", test.getMensaje(),
                    "tenant", defaultTenantId
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "ERROR",
                    "error", e.getMessage(),
                    "tenant", defaultTenantId
            );
        }
    }
}