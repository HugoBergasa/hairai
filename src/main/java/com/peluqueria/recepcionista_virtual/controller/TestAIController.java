package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.service.OpenAIService;
import com.peluqueria.recepcionista_virtual.dto.OpenAIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador de prueba para verificar que la IA funciona correctamente
 * NOTA: Este controlador es solo para desarrollo/testing
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = {"http://localhost:3000", "https://hairai.netlify.app"})
public class TestAIController {

    private static final Logger logger = LoggerFactory.getLogger(TestAIController.class);

    @Autowired
    private OpenAIService openAIService;

    @Value("${default.tenant.id:tenant_demo_001}")
    private String defaultTenantId;

    /**
     * Endpoint para probar el procesamiento de IA
     * POST /api/test/ai
     * Body: { "mensaje": "Hola, quiero reservar una cita para corte de pelo" }
     */
    @PostMapping("/ai")
    public ResponseEntity<?> testAI(@RequestBody Map<String, String> request) {
        try {
            String mensaje = request.get("mensaje");
            String tenantId = request.getOrDefault("tenantId", defaultTenantId);
            String callSid = "TEST-" + System.currentTimeMillis(); // CallSid de prueba

            logger.info("Test AI - Mensaje: {}, Tenant: {}", mensaje, tenantId);

            // Procesar con OpenAI
            OpenAIResponse response = openAIService.procesarMensaje(mensaje, tenantId, callSid);

            // Preparar respuesta
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", response);
            result.put("tenantId", tenantId);
            result.put("callSid", callSid);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error en test de IA", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());

            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Endpoint para verificar que el servicio está funcionando
     * GET /api/test/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "RecepcionistaVirtual-IA");
        status.put("timestamp", System.currentTimeMillis());
        status.put("defaultTenant", defaultTenantId);

        return ResponseEntity.ok(status);
    }

    /**
     * Endpoint para simular una conversación completa
     * POST /api/test/conversation
     * Body: {
     *   "mensajes": [
     *     "Hola",
     *     "Quiero reservar una cita",
     *     "Para corte de pelo",
     *     "Mañana a las 10"
     *   ]
     * }
     */
    @PostMapping("/conversation")
    public ResponseEntity<?> testConversation(@RequestBody Map<String, Object> request) {
        try {
            java.util.List<String> mensajes = (java.util.List<String>) request.get("mensajes");
            String tenantId = (String) request.getOrDefault("tenantId", defaultTenantId);
            String callSid = "CONV-" + System.currentTimeMillis();

            java.util.List<Map<String, Object>> conversacion = new java.util.ArrayList<>();

            for (String mensaje : mensajes) {
                logger.info("Procesando mensaje: {}", mensaje);

                OpenAIResponse response = openAIService.procesarMensaje(mensaje, tenantId, callSid);

                Map<String, Object> turn = new HashMap<>();
                turn.put("usuario", mensaje);
                turn.put("ia", response);
                conversacion.add(turn);

                // Pequeña pausa para simular conversación real
                Thread.sleep(500);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("conversacion", conversacion);
            result.put("tenantId", tenantId);
            result.put("callSid", callSid);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error en test de conversación", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.status(500).body(error);
        }
    }
}