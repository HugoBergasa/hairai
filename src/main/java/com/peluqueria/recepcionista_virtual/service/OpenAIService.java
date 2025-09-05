package com.peluqueria.recepcionista_virtual.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peluqueria.recepcionista_virtual.model.ConversacionIA;
import com.peluqueria.recepcionista_virtual.model.ConfiguracionTenant;
import com.peluqueria.recepcionista_virtual.repository.ConversacionIARepository;
import com.peluqueria.recepcionista_virtual.repository.ServicioRepository;
import com.peluqueria.recepcionista_virtual.dto.OpenAIResponse;
import com.peluqueria.recepcionista_virtual.dto.DatosCita;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OpenAIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Autowired
    private TenantConfigService tenantConfigService;

    @Autowired
    private ConversacionIARepository conversacionRepo;

    @Autowired
    private ServicioRepository servicioRepo;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int MAX_TOKENS = 500;
    private static final double DEFAULT_TEMPERATURE = 0.7;

    /**
     * Procesa un mensaje del usuario y devuelve la respuesta de IA
     */
    public OpenAIResponse procesarMensaje(String mensaje, String tenantId, String callSid) {
        long startTime = System.currentTimeMillis();

        try {
            logger.info("Procesando mensaje para tenant {} - CallSid: {}", tenantId, callSid);

            // 1. Obtener contexto del tenant
            String systemPrompt = construirPromptConContexto(tenantId);

            // 2. Obtener historial de conversación si existe
            List<ConversacionIA> historial = obtenerHistorialReciente(tenantId, callSid);

            // 3. Construir mensajes para GPT
            List<Map<String, String>> messages = construirMensajes(systemPrompt, mensaje, historial);

            // 4. Llamar a OpenAI
            String respuestaGPT = llamarOpenAI(messages, tenantId);

            // 5. Parsear respuesta
            OpenAIResponse response = parsearRespuesta(respuestaGPT);

            // 6. Guardar en base de datos
            guardarConversacion(tenantId, callSid, mensaje, response,
                    System.currentTimeMillis() - startTime);

            return response;

        } catch (Exception e) {
            logger.error("Error procesando mensaje con OpenAI", e);
            return crearRespuestaError();
        }
    }

    /**
     * Construye el prompt del sistema con toda la información del tenant
     */
    private String construirPromptConContexto(String tenantId) {
        Map<String, String> config = tenantConfigService.obtenerConfiguracion(tenantId);

        StringBuilder prompt = new StringBuilder();

        // Información básica del negocio
        prompt.append("CONTEXTO DEL SISTEMA:\n");
        prompt.append("Eres la recepcionista virtual de ").append(config.getOrDefault(
                ConfiguracionTenant.Claves.NOMBRE_NEGOCIO, "la peluquería")).append(".\n");
        prompt.append("Fecha y hora actual: ").append(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");

        // Horario
        prompt.append("HORARIO DE ATENCIÓN:\n");
        prompt.append("- Apertura: ").append(config.getOrDefault(
                ConfiguracionTenant.Claves.HORARIO_APERTURA, "09:00")).append("\n");
        prompt.append("- Cierre: ").append(config.getOrDefault(
                ConfiguracionTenant.Claves.HORARIO_CIERRE, "20:00")).append("\n");
        prompt.append("- Días laborables: ").append(config.getOrDefault(
                ConfiguracionTenant.Claves.DIAS_TRABAJO, "Lunes a Sábado")).append("\n\n");

        // Servicios disponibles (si los hay en BD)
        agregarServiciosAlPrompt(prompt, tenantId);

        // Instrucciones de comportamiento
        prompt.append("INSTRUCCIONES DE COMPORTAMIENTO:\n");
        prompt.append("1. Sé siempre amable, profesional y servicial\n");
        prompt.append("2. Habla en español de España\n");
        prompt.append("3. Si el cliente quiere reservar una cita, necesitas:\n");
        prompt.append("   - Tipo de servicio\n");
        prompt.append("   - Fecha deseada\n");
        prompt.append("   - Hora preferida\n");
        prompt.append("   - Nombre del cliente\n");
        prompt.append("   - Teléfono de contacto (si no lo tenemos)\n");
        prompt.append("4. Confirma siempre los detalles antes de finalizar una reserva\n");
        prompt.append("5. Si no puedes ayudar con algo, ofrece alternativas\n\n");

        // Formato de respuesta
        prompt.append("FORMATO DE RESPUESTA OBLIGATORIO:\n");
        prompt.append("DEBES responder SIEMPRE en formato JSON con esta estructura EXACTA:\n");
        prompt.append("{\n");
        prompt.append("  \"mensaje\": \"tu respuesta al cliente en lenguaje natural\",\n");
        prompt.append("  \"intencion\": \"RESERVAR_CITA|CONSULTAR_INFO|CANCELAR_CITA|MODIFICAR_CITA|SALUDO|DESPEDIDA|OTRO\",\n");
        prompt.append("  \"requiere_accion\": true/false,\n");
        prompt.append("  \"accion\": \"CREAR_CITA|BUSCAR_DISPONIBILIDAD|CANCELAR_CITA|NINGUNA\",\n");
        prompt.append("  \"datos_extraidos\": {\n");
        prompt.append("    \"servicio\": \"nombre del servicio si lo menciona\",\n");
        prompt.append("    \"fecha\": \"fecha en formato YYYY-MM-DD si la menciona\",\n");
        prompt.append("    \"hora\": \"hora en formato HH:MM si la menciona\",\n");
        prompt.append("    \"nombre_cliente\": \"nombre si lo proporciona\",\n");
        prompt.append("    \"telefono\": \"teléfono si lo proporciona\"\n");
        prompt.append("  },\n");
        prompt.append("  \"confianza\": 0.0 a 1.0\n");
        prompt.append("}\n\n");

        prompt.append("IMPORTANTE: Tu respuesta debe ser ÚNICAMENTE el JSON, sin texto adicional.\n");

        // Agregar prompt personalizado si existe
        String promptPersonalizado = config.get(ConfiguracionTenant.Claves.PROMPT_SISTEMA);
        if (promptPersonalizado != null && !promptPersonalizado.isEmpty()) {
            prompt.append("\nINSTRUCCIONES ADICIONALES ESPECÍFICAS:\n");
            prompt.append(promptPersonalizado).append("\n");
        }

        return prompt.toString();
    }

    /**
     * Agrega los servicios disponibles al prompt
     */
    private void agregarServiciosAlPrompt(StringBuilder prompt, String tenantId) {
        try {
            List<Map<String, Object>> servicios = servicioRepo.findServiciosByTenantId(tenantId);

            if (!servicios.isEmpty()) {
                prompt.append("SERVICIOS DISPONIBLES:\n");
                for (Map<String, Object> servicio : servicios) {
                    prompt.append("- ").append(servicio.get("nombre"));
                    prompt.append(" (").append(servicio.get("duracion")).append(" min)");
                    prompt.append(" - Precio: €").append(servicio.get("precio"));
                    if (servicio.get("descripcion") != null) {
                        prompt.append(" - ").append(servicio.get("descripcion"));
                    }
                    prompt.append("\n");
                }
                prompt.append("\n");
            }
        } catch (Exception e) {
            logger.warn("No se pudieron cargar los servicios para el tenant: {}", tenantId);
        }
    }

    /**
     * Obtiene el historial reciente de la conversación
     */
    private List<ConversacionIA> obtenerHistorialReciente(String tenantId, String callSid) {
        if (callSid == null || callSid.isEmpty()) {
            return new ArrayList<>();
        }

        List<ConversacionIA> historial = conversacionRepo.findByTenantIdAndCallSid(tenantId, callSid);

        // Limitar a las últimas 5 interacciones para no exceder el contexto
        if (historial.size() > 5) {
            return historial.subList(historial.size() - 5, historial.size());
        }

        return historial;
    }

    /**
     * Construye la lista de mensajes para enviar a GPT
     */
    private List<Map<String, String>> construirMensajes(String systemPrompt,
                                                        String mensajeActual,
                                                        List<ConversacionIA> historial) {
        List<Map<String, String>> messages = new ArrayList<>();

        // Mensaje del sistema
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        // Historial de conversación
        for (ConversacionIA conv : historial) {
            // Mensaje del usuario
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", conv.getMensajeUsuario());
            messages.add(userMsg);

            // Respuesta del asistente
            if (conv.getRespuestaIA() != null) {
                Map<String, String> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", conv.getRespuestaIA());
                messages.add(assistantMsg);
            }
        }

        // Mensaje actual
        Map<String, String> currentMessage = new HashMap<>();
        currentMessage.put("role", "user");
        currentMessage.put("content", mensajeActual);
        messages.add(currentMessage);

        return messages;
    }

    /**
     * Realiza la llamada a la API de OpenAI
     */
    private String llamarOpenAI(List<Map<String, String>> messages, String tenantId) {
        try {
            // Obtener configuración del modelo
            String modelo = tenantConfigService.obtenerValor(tenantId,
                    ConfiguracionTenant.Claves.MODELO_GPT, "gpt-4");
            String temperaturaStr = tenantConfigService.obtenerValor(tenantId,
                    ConfiguracionTenant.Claves.TEMPERATURA_IA, "0.7");
            double temperatura = Double.parseDouble(temperaturaStr);

            // Preparar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Preparar body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelo);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperatura);
            requestBody.put("max_tokens", MAX_TOKENS);
            requestBody.put("response_format", Map.of("type", "json_object"));

            // Hacer la llamada
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            // Extraer respuesta
            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");

            return (String) message.get("content");

        } catch (HttpClientErrorException e) {
            logger.error("Error llamando a OpenAI API: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Error en la llamada a OpenAI: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado llamando a OpenAI", e);
            throw new RuntimeException("Error procesando con IA: " + e.getMessage());
        }
    }

    /**
     * Parsea la respuesta de GPT a nuestro formato
     */
    private OpenAIResponse parsearRespuesta(String respuestaGPT) {
        try {
            Map<String, Object> jsonResponse = objectMapper.readValue(respuestaGPT, Map.class);

            OpenAIResponse response = new OpenAIResponse();
            response.setMensaje((String) jsonResponse.get("mensaje"));
            response.setIntencion((String) jsonResponse.get("intencion"));
            response.setRequiereAccion((Boolean) jsonResponse.getOrDefault("requiere_accion", false));
            response.setAccion((String) jsonResponse.getOrDefault("accion", "NINGUNA"));

            // Extraer datos si existen
            Map<String, Object> datosExtraidos = (Map<String, Object>) jsonResponse.get("datos_extraidos");
            if (datosExtraidos != null) {
                DatosCita datos = new DatosCita();
                datos.setServicio((String) datosExtraidos.get("servicio"));
                datos.setFecha((String) datosExtraidos.get("fecha"));
                datos.setHora((String) datosExtraidos.get("hora"));
                datos.setNombreCliente((String) datosExtraidos.get("nombre_cliente"));
                datos.setTelefono((String) datosExtraidos.get("telefono"));
                response.setDatosCita(datos);
            }

            response.setConfianza(((Number) jsonResponse.getOrDefault("confianza", 0.8)).doubleValue());

            return response;

        } catch (Exception e) {
            logger.error("Error parseando respuesta de GPT: {}", respuestaGPT, e);
            // Crear respuesta por defecto si falla el parseo
            OpenAIResponse response = new OpenAIResponse();
            response.setMensaje(respuestaGPT); // Usar la respuesta raw
            response.setIntencion("OTRO");
            response.setRequiereAccion(false);
            response.setAccion("NINGUNA");
            response.setConfianza(0.5);
            return response;
        }
    }

    /**
     * Guarda la conversación en la base de datos
     */
    private void guardarConversacion(String tenantId, String callSid, String mensaje,
                                     OpenAIResponse response, long duracionMs) {
        ConversacionIA conversacion = new ConversacionIA();
        conversacion.setTenantId(tenantId);
        conversacion.setCallSid(callSid);
        conversacion.setMensajeUsuario(mensaje);
        conversacion.setRespuestaIA(response.getMensaje());
        conversacion.setIntencionDetectada(response.getIntencion());
        conversacion.setAccionEjecutada(response.getAccion());
        conversacion.setCanal(ConversacionIA.CanalComunicacion.TELEFONO);
        conversacion.setTimestamp(LocalDateTime.now());
        conversacion.setDuracionMs((int) duracionMs);
        conversacion.setExitoso(true);

        // Guardar contexto como JSON
        try {
            Map<String, Object> contexto = new HashMap<>();
            contexto.put("confianza", response.getConfianza());
            if (response.getDatosCita() != null) {
                contexto.put("datos_extraidos", response.getDatosCita());
            }
            conversacion.setContexto(objectMapper.writeValueAsString(contexto));
        } catch (Exception e) {
            logger.warn("No se pudo serializar el contexto", e);
        }

        conversacionRepo.save(conversacion);
    }

    /**
     * Crea una respuesta de error por defecto
     */
    private OpenAIResponse crearRespuestaError() {
        OpenAIResponse response = new OpenAIResponse();
        response.setMensaje("Disculpa, estoy teniendo problemas técnicos. ¿Podrías repetir tu solicitud?");
        response.setIntencion("ERROR");
        response.setRequiereAccion(false);
        response.setAccion("NINGUNA");
        response.setConfianza(0.0);
        return response;
    }
}