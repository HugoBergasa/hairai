package com.peluqueria.recepcionista_virtual.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.*;
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
        Twilio.init(accountSid, authToken);
        this.openAiService = new OpenAiService(openAiApiKey);
    }

    public String procesarLlamadaConIA(String transcripcion, String tenantId) {
        try {
            // Crear contexto para OpenAI
            List<ChatMessage> messages = new ArrayList<>();

            ChatMessage systemMessage = new ChatMessage(
                    ChatMessageRole.SYSTEM.value(),
                    """
                    Eres un recepcionista virtual amable y profesional de una peluquería.
                    Tu trabajo es:
                    1. Saludar cordialmente
                    2. Identificar si el cliente quiere: agendar cita, cancelar, modificar o consultar horarios
                    3. Para agendar: preguntar servicio deseado, fecha y hora preferida
                    4. Confirmar los datos antes de procesar
                    5. Ser breve y claro en las respuestas
                    
                    Servicios disponibles:
                    - Corte de cabello (30 min, 20€)
                    - Tinte (90 min, 50€)
                    - Peinado (45 min, 35€)
                    - Manicura (30 min, 25€)
                    
                    Horario: Lunes a Sábado, 9:00 - 20:00
                    """
            );

            ChatMessage userMessage = new ChatMessage(
                    ChatMessageRole.USER.value(),
                    transcripcion
            );

            messages.add(systemMessage);
            messages.add(userMessage);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-4-turbo-preview")
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
        Say say = new Say.Builder(mensaje)
                .voice(Say.Voice.POLLY_CONCHITA) // Voz en español
                .language(Say.Language.ES_ES)
                .build();

        Gather gather = new Gather.Builder()
                .input(Gather.Input.SPEECH)
                .language(Gather.Language.ES_ES)
                .timeout(3)
                .action("/api/twilio/process-speech")
                .method(Method.POST)
                .speechTimeout("auto")
                .build();

        VoiceResponse response = new VoiceResponse.Builder()
                .say(say)
                .gather(gather)
                .build();

        return response.toXml();
    }
}