package com.peluqueria.recepcionista_virtual.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.*;
import com.twilio.http.HttpMethod;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.*;
import com.peluqueria.recepcionista_virtual.model.Tenant;
import com.peluqueria.recepcionista_virtual.model.Servicio;
import com.peluqueria.recepcionista_virtual.repository.TenantRepository;
import com.peluqueria.recepcionista_virtual.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
@Slf4j
public class TwilioAIService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String twilioPhoneNumber;

    @Value("${openai.api-key}")
    private String openAiApiKey;

    @Autowired
    private TenantRepository tenantRepository; // ✅ AGREGADO: Para datos del tenant

    @Autowired
    private ServicioRepository servicioRepository; // ✅ AGREGADO: Para servicios dinámicos

    private OpenAiService openAiService;

    @PostConstruct
    public void init() {
        try {
            Twilio.init(accountSid, authToken);
            if (openAiApiKey != null && !openAiApiKey.equals("sk-dummy")) {
                this.openAiService = new OpenAiService(openAiApiKey);
            }
        } catch (Exception e) {
            log.error("Error inicializando servicios: ", e);
        }
    }

    /**
     * ✅ PROCESAMIENTO CON IA PERSONALIZADO POR TENANT - NO MÁS HARDCODING
     */
    public String procesarLlamadaConIA(String transcripcion, String tenantId) {
        try {
            if (openAiService == null) {
                return "Lo siento, el servicio de IA no está disponible en este momento.";
            }

            // ✅ OBTENER DATOS DEL TENANT Y SERVICIOS DESDE BD
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) {
                log.error("Tenant no encontrado: {}", tenantId);
                return "Lo siento, hay un problema con la configuración. Por favor, inténtelo más tarde.";
            }

            List<ChatMessage> messages = new ArrayList<>();

            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setRole(ChatMessageRole.SYSTEM.value());
            systemMessage.setContent(construirPromptPersonalizadoPorTenant(tenant));

            ChatMessage userMessage = new ChatMessage();
            userMessage.setRole(ChatMessageRole.USER.value());
            userMessage.setContent(transcripcion);

            messages.add(systemMessage);
            messages.add(userMessage);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-4")
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(150)
                    .build();

            ChatCompletionResult result = openAiService.createChatCompletion(request);
            return result.getChoices().get(0).getMessage().getContent();

        } catch (Exception e) {
            log.error("Error procesando con OpenAI: ", e);
            return "Lo siento, estoy teniendo problemas técnicos. ¿Podría llamar más tarde o dejar su número?";
        }
    }

    /**
     * ✅ CONSTRUIR PROMPT PERSONALIZADO POR TENANT CON SERVICIOS REALES DESDE BD
     */
    private String construirPromptPersonalizadoPorTenant(Tenant tenant) {
        // ✅ OBTENER SERVICIOS DINÁMICOS DESDE BD
        String serviciosDisponibles = construirServiciosDesdeDB(tenant.getId());

        return String.format(
                "Eres un recepcionista virtual amable y profesional de %s. " +
                        "Tu trabajo es: " +
                        "1. Saludar cordialmente " +
                        "2. Identificar si el cliente quiere: agendar cita, cancelar, modificar o consultar horarios " +
                        "3. Para agendar: preguntar servicio deseado, fecha y hora preferida " +
                        "4. Confirmar los datos antes de procesar " +
                        "5. Ser breve y claro en las respuestas " +
                        "\n%s\n" +
                        "Horario: %s, %s - %s" +
                        "%s",
                tenant.getNombrePeluqueria(),
                serviciosDisponibles,
                tenant.getDiasLaborables() != null ? tenant.getDiasLaborables() : "Lunes a Sábado",
                tenant.getHoraApertura() != null ? tenant.getHoraApertura() : "9:00",
                tenant.getHoraCierre() != null ? tenant.getHoraCierre() : "20:00",
                tenant.getTelefono() != null ? "\nPara más información: " + tenant.getTelefono() : ""
        );
    }

    /**
     * ✅ CONSTRUIR SERVICIOS DINÁMICOS DESDE BD POR TENANT
     */
    private String construirServiciosDesdeDB(String tenantId) {
        try {
            List<Servicio> servicios = servicioRepository.findActivosByTenantId(tenantId);

            if (servicios.isEmpty()) {
                log.warn("No hay servicios activos para tenant: {}", tenantId);
                return "Servicios disponibles: Consultar servicios disponibles al llegar.";
            }

            StringBuilder serviciosStr = new StringBuilder("Servicios disponibles:");

            for (Servicio servicio : servicios) {
                serviciosStr.append(String.format(
                        "\n- %s (%d min, €%.0f)",
                        servicio.getNombre(),
                        servicio.getDuracionMinutos(),
                        servicio.getPrecio()
                ));
            }

            log.info("✅ Servicios construidos para TwilioAI - Tenant {}: {} servicios", tenantId, servicios.size());
            return serviciosStr.toString();

        } catch (Exception e) {
            log.error("❌ Error construyendo servicios para TwilioAI tenant {}: {}", tenantId, e.getMessage());
            return "Servicios disponibles: Error cargando lista. Consulte al llegar.";
        }
    }

    public void enviarSMS(String numeroDestino, String mensaje) {
        try {
            if (twilioPhoneNumber == null || twilioPhoneNumber.equals("+34000000000")) {
                log.info("📱 SMS simulado a {}: {}", numeroDestino, mensaje);
                return;
            }

            Message.creator(
                    new com.twilio.type.PhoneNumber(numeroDestino),
                    new com.twilio.type.PhoneNumber(twilioPhoneNumber),
                    mensaje
            ).create();

            log.info("📱 SMS enviado a: " + numeroDestino);
        } catch (Exception e) {
            log.error("❌ Error enviando SMS: ", e);
        }
    }

    /**
     * ✅ GENERAR TWIML PERSONALIZADO POR TENANT
     */
    public String generarTwiML(String mensaje, String tenantId) {
        try {
            // ✅ OBTENER DATOS DEL TENANT PARA PERSONALIZAR RESPUESTA
            Tenant tenant = null;
            if (tenantId != null) {
                tenant = tenantRepository.findById(tenantId).orElse(null);
            }

            // Si el mensaje no incluye el nombre del negocio, agregarlo
            String mensajePersonalizado = mensaje;
            if (tenant != null && !mensaje.toLowerCase().contains(tenant.getNombrePeluqueria().toLowerCase())) {
                mensajePersonalizado = "En " + tenant.getNombrePeluqueria() + ": " + mensaje;
            }

            Say say = new Say.Builder(mensajePersonalizado)
                    .voice(Say.Voice.POLLY_CONCHITA)
                    .language(Say.Language.ES_ES)
                    .build();

            VoiceResponse response = new VoiceResponse.Builder()
                    .say(say)
                    .build();

            return response.toXml();
        } catch (Exception e) {
            log.error("❌ Error generando TwiML: ", e);
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Say>Error en el sistema</Say></Response>";
        }
    }

    /**
     * ✅ GENERAR TWIML SIMPLE (SOBRECARGA PARA COMPATIBILIDAD)
     */
    public String generarTwiML(String mensaje) {
        return generarTwiML(mensaje, null);
    }

    /**
     * ✅ MÉTODO AUXILIAR - OBTENER INFORMACIÓN RESUMIDA DEL TENANT PARA LOGS
     */
    public String obtenerInfoTenant(String tenantId) {
        try {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) {
                return "Tenant no encontrado: " + tenantId;
            }

            int numServicios = servicioRepository.findActivosByTenantId(tenantId).size();

            return String.format("Tenant: %s | Servicios: %d | Horario: %s-%s",
                    tenant.getNombrePeluqueria(),
                    numServicios,
                    tenant.getHoraApertura(),
                    tenant.getHoraCierre()
            );

        } catch (Exception e) {
            return "Error obteniendo info del tenant: " + e.getMessage();
        }
    }

    /**
     * ✅ VALIDAR SI TENANT TIENE CONFIGURACIÓN COMPLETA
     */
    public boolean tenantTieneConfiguracionCompleta(String tenantId) {
        try {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) return false;

            // Verificar datos básicos
            boolean datosBasicos = tenant.getNombrePeluqueria() != null &&
                    tenant.getHoraApertura() != null &&
                    tenant.getHoraCierre() != null;

            // Verificar que tenga al menos un servicio
            int numServicios = servicioRepository.findActivosByTenantId(tenantId).size();
            boolean tieneServicios = numServicios > 0;

            log.info("✅ Validación tenant {}: datos básicos={}, servicios={} ({})",
                    tenantId, datosBasicos, tieneServicios, numServicios);

            return datosBasicos && tieneServicios;

        } catch (Exception e) {
            log.error("❌ Error validando configuración del tenant {}: {}", tenantId, e.getMessage());
            return false;
        }
    }
}