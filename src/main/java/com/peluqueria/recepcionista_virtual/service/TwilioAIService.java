package com.peluqueria.recepcionista_virtual.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.*;
import com.twilio.http.HttpMethod;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.*;
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

    public String procesarLlamadaConIA(String transcripcion, String tenantId) {
        try {
            if (openAiService == null) {
                return "Lo siento, el servicio de IA no está disponible en este momento.";
            }

            List<ChatMessage> messages = new ArrayList<>();

            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setRole(ChatMessageRole.SYSTEM.value());
            systemMessage.setContent(
                    "Eres un recepcionista virtual amable y profesional de una peluquería. " +
                            "Tu trabajo es: " +
                            "1. Saludar cordialmente " +
                            "2. Identificar si el cliente quiere: agendar cita, cancelar, modificar o consultar horarios " +
                            "3. Para agendar: preguntar servicio deseado, fecha y hora preferida " +
                            "4. Confirmar los datos antes de procesar " +
                            "5. Ser breve y claro en las respuestas " +
                            "Servicios disponibles: " +
                            "- Corte de cabello (30 min, 20€) " +
                            "- Tinte (90 min, 50€) " +
                            "- Peinado (45 min, 35€) " +
                            "- Manicura (30 min, 25€) " +
                            "Horario: Lunes a Sábado, 9:00 - 20:00"
            );

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

    public void enviarSMS(String numeroDestino, String mensaje) {
        try {
            if (twilioPhoneNumber == null || twilioPhoneNumber.equals("+34000000000")) {
                log.info("SMS simulado a {}: {}", numeroDestino, mensaje);
                return;
            }

            Message.creator(
                    new com.twilio.type.PhoneNumber(numeroDestino),
                    new com.twilio.type.PhoneNumber(twilioPhoneNumber),
                    mensaje
            ).create();

            log.info("SMS enviado a: " + numeroDestino);
        } catch (Exception e) {
            log.error("Error enviando SMS: ", e);
        }
    }

    public String generarTwiML(String mensaje) {
        try {
            Say say = new Say.Builder(mensaje)
                    .voice(Say.Voice.POLLY_CONCHITA)
                    .language(Say.Language.ES_ES)
                    .build();

            // Versión simplificada sin Gather por ahora
            VoiceResponse response = new VoiceResponse.Builder()
                    .say(say)
                    .build();

            return response.toXml();
        } catch (Exception e) {
            log.error("Error generando TwiML: ", e);
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Say>Error en el sistema</Say></Response>";
        }
    }
}