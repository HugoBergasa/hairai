package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@RestController
@RequestMapping("/api/twilio")
@Slf4j
public class TwilioController {

    @Autowired
    private TwilioAIService twilioAIService;

    @Autowired
    private CitaService citaService;

    @PostMapping(value = "/voice", produces = MediaType.APPLICATION_XML_VALUE)
    public String handleIncomingCall(
            @RequestParam Map<String, String> params
    ) {
        log.info("Llamada entrante de: " + params.get("From"));

        String mensajeBienvenida = "Hola, bienvenido a Peluquería Style. " +
                "Soy tu asistente virtual. Puedes decirme si quieres agendar " +
                "una cita, cancelar o modificar una existente.";

        return twilioAIService.generarTwiML(mensajeBienvenida);
    }

    @PostMapping(value = "/process-speech", produces = MediaType.APPLICATION_XML_VALUE)
    public String processSpeech(
            @RequestParam Map<String, String> params
    ) {
        String speechResult = params.get("SpeechResult");
        String caller = params.get("From");

        log.info("Transcripción recibida: " + speechResult);

        // Procesar con IA
        String respuestaIA = twilioAIService.procesarLlamadaConIA(
                speechResult,
                "default-tenant" // TODO: Identificar tenant por número
        );

        // Analizar intención y ejecutar acción
        if (speechResult.toLowerCase().contains("agendar") ||
                speechResult.toLowerCase().contains("cita")) {
            // Lógica para agendar cita
            procesarAgendamiento(speechResult, caller);
        }

        return twilioAIService.generarTwiML(respuestaIA);
    }

    private void procesarAgendamiento(String texto, String telefono) {
        // TODO: Implementar lógica de agendamiento
        // Extraer fecha, hora, servicio del texto
        // Crear cita en base de datos
        // Enviar SMS de confirmación
    }
}