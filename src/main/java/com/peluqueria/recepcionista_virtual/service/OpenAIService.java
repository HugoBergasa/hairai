package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.dto.DatosCita;
import com.peluqueria.recepcionista_virtual.dto.OpenAIResponse;
import com.peluqueria.recepcionista_virtual.model.Tenant;
import com.peluqueria.recepcionista_virtual.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TenantRepository tenantRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * PROCESAMIENTO INTELIGENTE - GPT-4 COMO CEREBRO
     */
    public OpenAIResponse procesarMensaje(String mensaje, String tenantId, String callSid) {
        try {
            logger.info("Procesando mensaje con OpenAI - Tenant: {}, Mensaje: {}", tenantId, mensaje);

            // 1. VALIDAR API KEY
            if (apiKey == null || apiKey.equals("sk-dummy") || apiKey.startsWith("sk-proj-tu-clave")) {
                logger.warn("OpenAI API Key no configurada - usando respuesta mock");
                return crearRespuestaMock(mensaje);
            }

            // 2. OBTENER CONTEXTO COMPLETO DEL TENANT
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) {
                logger.error("Tenant no encontrado: {}", tenantId);
                return crearRespuestaError("Tenant no encontrado");
            }

            // 3. CONSTRUIR PROMPT PERSONALIZADO CON CONTEXTO DEL NEGOCIO
            String systemPrompt = construirPromptPersonalizado(tenant);

            // 4. LLAMAR A GPT-4 CON CONTEXTO COMPLETO
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4",
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", mensaje)
                    ),
                    "temperature", 0.7,
                    "max_tokens", 300,
                    "response_format", Map.of("type", "json_object")
            );

            logger.debug("Enviando request a OpenAI: {}", requestBody);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.openai.com/v1/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // 5. PROCESAR RESPUESTA Y EXTRAER DATOS ESTRUCTURADOS
            return parsearRespuestaGPT(response);

        } catch (Exception e) {
            logger.error("Error en OpenAI para tenant {}: {}", tenantId, e.getMessage(), e);
            return crearRespuestaError("Error procesando mensaje: " + e.getMessage());
        }
    }

    /**
     * CONSTRUIR PROMPT PERSONALIZADO POR TENANT
     */
    private String construirPromptPersonalizado(Tenant tenant) {
        return String.format("""
            Eres la recepcionista virtual de %s.
            
            INFORMACIÓN DEL NEGOCIO:
            - Nombre: %s
            - Horarios: %s a %s
            - Días laborables: %s
            - Duración por cita: %d minutos
            
            SERVICIOS DISPONIBLES:
            - Corte básico (30 min, €20)
            - Peinado (45 min, €35)  
            - Tinte (90 min, €50)
            - Manicura (30 min, €25)
            
            INSTRUCCIONES CRÍTICAS:
            1. Sé amable y profesional
            2. Habla en español natural
            3. Si quieren reservar cita, EXTRAE datos estructurados:
               - servicio (debe ser uno de los disponibles)
               - fecha (formato: yyyy-MM-dd o texto como "hoy", "mañana")
               - hora (formato: HH:mm)
               - nombre del cliente
               - teléfono
            
            RESPONDE SIEMPRE EN JSON VÁLIDO:
            {
              "mensaje": "tu respuesta natural y amable",
              "intencion": "RESERVAR_CITA|CONSULTAR_INFO|CANCELAR_CITA|OTRO",
              "requiereAccion": true/false,
              "accion": "CREAR_CITA|NINGUNA",
              "datosCita": {
                "servicio": "nombre del servicio exacto",
                "fecha": "fecha extraída",
                "hora": "hora extraída",
                "nombreCliente": "nombre del cliente",
                "telefono": "teléfono si lo menciona"
              }
            }
            """,
                tenant.getNombrePeluqueria(),
                tenant.getNombrePeluqueria(),
                tenant.getHoraApertura(),
                tenant.getHoraCierre(),
                tenant.getDiasLaborables(),
                tenant.getDuracionCitaMinutos()
        );
    }

    /**
     * PARSEAR RESPUESTA DE GPT-4 Y CONVERTIR A OpenAIResponse
     */
    private OpenAIResponse parsearRespuestaGPT(ResponseEntity<Map> response) {
        try {
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("choices")) {
                logger.error("Respuesta de OpenAI inválida: {}", responseBody);
                return crearRespuestaError("Respuesta inválida de OpenAI");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices.isEmpty()) {
                return crearRespuestaError("No hay respuestas de OpenAI");
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            String content = (String) message.get("content");

            logger.debug("Contenido de OpenAI: {}", content);

            // Parsear JSON response de GPT-4
            Map<String, Object> jsonResponse = objectMapper.readValue(content, Map.class);

            OpenAIResponse respuesta = new OpenAIResponse();
            respuesta.setMensaje((String) jsonResponse.get("mensaje"));
            respuesta.setIntencion((String) jsonResponse.get("intencion"));
            respuesta.setRequiereAccion((Boolean) jsonResponse.getOrDefault("requiereAccion", false));
            respuesta.setAccion((String) jsonResponse.getOrDefault("accion", "NINGUNA"));

            // Parsear datosCita si existe
            Map<String, Object> datosCitaMap = (Map<String, Object>) jsonResponse.get("datosCita");
            if (datosCitaMap != null) {
                DatosCita datosCita = new DatosCita();
                datosCita.setServicio((String) datosCitaMap.get("servicio"));
                datosCita.setFecha((String) datosCitaMap.get("fecha"));
                datosCita.setHora((String) datosCitaMap.get("hora"));
                datosCita.setNombreCliente((String) datosCitaMap.get("nombreCliente"));
                datosCita.setTelefono((String) datosCitaMap.get("telefono"));
                respuesta.setDatosCita(datosCita);
            }

            return respuesta;

        } catch (Exception e) {
            logger.error("Error parseando respuesta de GPT: {}", e.getMessage(), e);
            return crearRespuestaError("Error procesando respuesta IA");
        }
    }

    /**
     * CREAR RESPUESTA MOCK PARA TESTING SIN API KEY
     */
    private OpenAIResponse crearRespuestaMock(String mensaje) {
        OpenAIResponse respuesta = new OpenAIResponse();

        if (mensaje.toLowerCase().contains("cita") ||
                mensaje.toLowerCase().contains("reservar") ||
                mensaje.toLowerCase().contains("agendar")) {

            respuesta.setMensaje("Perfecto, entiendo que quiere reservar una cita. " +
                    "¿Para qué servicio sería? Tenemos corte, peinado, tinte y manicura.");
            respuesta.setIntencion("RESERVAR_CITA");
            respuesta.setRequiereAccion(true);
            respuesta.setAccion("CREAR_CITA");

            // Mock de datos de cita parciales
            DatosCita datosCita = new DatosCita();
            datosCita.setServicio("corte");
            datosCita.setFecha("mañana");
            datosCita.setHora("10:00");
            datosCita.setNombreCliente("Cliente");
            respuesta.setDatosCita(datosCita);

        } else {
            respuesta.setMensaje("Hola, gracias por contactar con nosotros. " +
                    "¿En qué puedo ayudarle hoy?");
            respuesta.setIntencion("CONSULTAR_INFO");
            respuesta.setRequiereAccion(false);
            respuesta.setAccion("NINGUNA");
        }

        return respuesta;
    }

    /**
     * CREAR RESPUESTA DE ERROR
     */
    private OpenAIResponse crearRespuestaError(String mensaje) {
        OpenAIResponse respuesta = new OpenAIResponse();
        respuesta.setMensaje("Disculpe, hay un problema técnico temporal. " +
                "Puede llamar directamente para reservar su cita.");
        respuesta.setIntencion("ERROR");
        respuesta.setRequiereAccion(false);
        respuesta.setAccion("NINGUNA");
        return respuesta;
    }
}