package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.dto.DatosCita;
import com.peluqueria.recepcionista_virtual.dto.DisponibilidadResult;
import com.peluqueria.recepcionista_virtual.dto.OpenAIResponse;
import com.peluqueria.recepcionista_virtual.model.HorarioEspecial;
import com.peluqueria.recepcionista_virtual.model.Tenant;
import com.peluqueria.recepcionista_virtual.model.Servicio;
import com.peluqueria.recepcionista_virtual.repository.TenantRepository;
import com.peluqueria.recepcionista_virtual.repository.ServicioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.LogicalType.DateTime;
@Service
public class OpenAIService {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ServicioRepository servicioRepository; // ✅ AGREGADO: Para consultas dinámicas

    @Autowired
    private HorarioEspecialService horarioEspecialService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ✅ PROCESAMIENTO INTELIGENTE - GPT-4 COMO CEREBRO PERSONALIZADO POR TENANT
     */
    public OpenAIResponse procesarMensaje(String mensaje, String tenantId, String callSid) {
        try {
            logger.info("Procesando mensaje con OpenAI - Tenant: {}, Mensaje: {}", tenantId, mensaje);

            // 1. VALIDAR API KEY
            if (apiKey == null || apiKey.equals("sk-dummy") || apiKey.startsWith("sk-proj-tu-clave")) {
                logger.warn("OpenAI API Key no configurada - usando respuesta mock");
                return crearRespuestaMock(mensaje, tenantId);
            }

            // 2. OBTENER CONTEXTO COMPLETO DEL TENANT
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) {
                logger.error("Tenant no encontrado: {}", tenantId);
                return crearRespuestaError("Tenant no encontrado");
            }

            // 3. ✅ CONSTRUIR PROMPT PERSONALIZADO CON SERVICIOS DINÁMICOS DESDE BD
            String systemPrompt = construirPromptPersonalizadoDinamico(tenant);

            // 4. LLAMAR A GPT-4 CON CONTEXTO COMPLETO
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4-turbo", // ✅ Usar GPT-4 Turbo que soporta JSON
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

    // ============================================
// 2. MODIFICAR EL MÉTODO construirPromptPersonalizadoDinamico()
// Agregar verificación de cierres al prompt
// ============================================

    private String construirPromptPersonalizadoDinamico(Tenant tenant) {
        String serviciosDisponibles = construirServiciosDesdeDB(tenant.getId());
        String informacionCierres = construirInformacionCierres(tenant.getId());

        return String.format(
                "Eres la recepcionista virtual de %s.\n\n" +
                        "INFORMACION DEL NEGOCIO:\n" +
                        "- Nombre: %s\n" +
                        "- Horarios: %s a %s\n" +
                        "- Dias laborables: %s\n" +
                        "- Duracion por cita: %d minutos\n" +
                        "- Telefono: %s\n\n" +
                        "%s\n\n" +
                        "%s\n\n" +
                        "INSTRUCCIONES CRITICAS:\n" +
                        "1. Se amable y profesional\n" +
                        "2. Habla en espanol natural\n" +
                        "3. ANTES de confirmar cualquier cita, SIEMPRE verifica si la fecha esta disponible\n" +
                        "4. Si hay cierres especiales, informa al cliente y ofrece alternativas\n" +
                        "5. Si quieren reservar cita, EXTRAE datos estructurados\n\n" +
                        "RESPONDE SIEMPRE EN JSON VALIDO:\n" +
                        "{\n" +
                        "  \"mensaje\": \"tu respuesta natural y amable\",\n" +
                        "  \"intencion\": \"RESERVAR_CITA|CONSULTAR_INFO|CANCELAR_CITA|OTRO\",\n" +
                        "  \"requiereAccion\": true,\n" +
                        "  \"accion\": \"CREAR_CITA|VERIFICAR_DISPONIBILIDAD|NINGUNA\",\n" +
                        "  \"datosCita\": {\n" +
                        "    \"servicio\": \"nombre del servicio exacto\",\n" +
                        "    \"fecha\": \"fecha extraida\",\n" +
                        "    \"hora\": \"hora extraida\",\n" +
                        "    \"nombreCliente\": \"nombre del cliente\",\n" +
                        "    \"telefono\": \"telefono si lo menciona\"\n" +
                        "  }\n" +
                        "}",
                tenant.getNombrePeluqueria(),
                tenant.getNombrePeluqueria(),
                tenant.getHoraApertura(),
                tenant.getHoraCierre(),
                tenant.getDiasLaborables(),
                tenant.getDuracionCitaMinutos(),
                tenant.getTelefono() != null ? tenant.getTelefono() : "No especificado",
                serviciosDisponibles,
                informacionCierres
        );
    }

    /**
     * ✅ CONSTRUIR SERVICIOS DINÁMICOS DESDE BD - CADA TENANT SUS PROPIOS SERVICIOS
     */
    private String construirServiciosDesdeDB(String tenantId) {
        try {
            List<Servicio> servicios = servicioRepository.findActivosByTenantId(tenantId);

            if (servicios.isEmpty()) {
                logger.warn("No hay servicios activos para tenant: {}", tenantId);
                return "SERVICIOS DISPONIBLES:\n- Consultar servicios disponibles";
            }

            StringBuilder serviciosStr = new StringBuilder("SERVICIOS DISPONIBLES:\n");

            for (Servicio servicio : servicios) {
                serviciosStr.append(String.format(
                        "- %s (%d min, €%.2f)%s\n",
                        servicio.getNombre(),
                        servicio.getDuracionMinutos(),
                        servicio.getPrecio(),
                        servicio.getDescripcion() != null ? " - " + servicio.getDescripcion() : ""
                ));
            }

            logger.info("Servicios construidos para tenant {}: {} servicios", tenantId, servicios.size());
            return serviciosStr.toString();

        } catch (Exception e) {
            logger.error("Error construyendo servicios para tenant {}: {}", tenantId, e.getMessage());
            return "SERVICIOS DISPONIBLES:\n- Error cargando servicios. Consulte disponibilidad.";
        }
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
     * ✅ CREAR RESPUESTA MOCK PERSONALIZADA POR TENANT
     */
    private OpenAIResponse crearRespuestaMock(String mensaje, String tenantId) {
        OpenAIResponse respuesta = new OpenAIResponse();

        // ✅ Obtener servicios reales para el mock también
        String serviciosInfo = construirServiciosDesdeDB(tenantId);

        if (mensaje.toLowerCase().contains("cita") ||
                mensaje.toLowerCase().contains("reservar") ||
                mensaje.toLowerCase().contains("agendar")) {

            respuesta.setMensaje("Perfecto, entiendo que quiere reservar una cita. " +
                    "¿Para qué servicio sería? " + serviciosInfo);
            respuesta.setIntencion("RESERVAR_CITA");
            respuesta.setRequiereAccion(true);
            respuesta.setAccion("CREAR_CITA");

            // Mock de datos de cita parciales
            DatosCita datosCita = new DatosCita();
            datosCita.setFecha("mañana");
            datosCita.setHora("10:00");
            datosCita.setNombreCliente("Cliente");
            respuesta.setDatosCita(datosCita);

        } else {
            respuesta.setMensaje("Hola, gracias por contactar con nosotros. " +
                    "¿En qué puedo ayudarle hoy? " + serviciosInfo);
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

    /**
     * 🆕 CONSTRUIR INFORMACIÓN DE CIERRES ESPECIALES PARA EL PROMPT
     *
     * MULTITENANT: Solo cierres del tenant actual
     * ZERO HARDCODING: Mensajes dinámicos desde BD
     * OpenAI CEREBRO: IA adaptará respuestas según cierres
     */
    private String construirInformacionCierres(String tenantId) {
        try {
            List<HorarioEspecial> cierresProximos = horarioEspecialService.obtenerCierresProximos(tenantId, 7);

            if (cierresProximos.isEmpty()) {
                return "DISPONIBILIDAD: Sin restricciones especiales los proximos dias.";
            }

            StringBuilder cierresInfo = new StringBuilder("CIERRES ESPECIALES PROXIMOS:\n");

            for (HorarioEspecial cierre : cierresProximos) {
                cierresInfo.append(String.format(
                        "- %s a %s: %s (%s)%s\n",
                        cierre.getFechaInicio(),
                        cierre.getFechaFin(),
                        cierre.getTipoCierre().getDescripcionTecnica(),
                        cierre.getMotivo() != null ? cierre.getMotivo() : "Sin motivo especificado",
                        cierre.getMensajePersonalizado() != null ?
                                " - " + cierre.getMensajePersonalizado() : ""
                ));
            }

            cierresInfo.append("\nIMPORTANTE: Verifica SIEMPRE la disponibilidad antes de confirmar citas.");

            logger.debug("Informacion de cierres construida para tenant {}: {} cierres",
                    tenantId, cierresProximos.size());

            return cierresInfo.toString();

        } catch (Exception e) {
            logger.error("Error construyendo informacion de cierres para tenant {}: {}",
                    tenantId, e.getMessage());
            return "DISPONIBILIDAD: Verificar disponibilidad antes de confirmar citas.";
        }
    }

    public OpenAIResponse procesarMensajeConVerificacionCierres(String mensaje, String tenantId, String callSid) {
        try {
            logger.info("Procesando mensaje con verificacion de cierres - Tenant: {}, Mensaje: {}",
                    tenantId, mensaje);

            OpenAIResponse respuestaIA = procesarMensaje(mensaje, tenantId, callSid);

            if ("RESERVAR_CITA".equals(respuestaIA.getIntencion()) &&
                    respuestaIA.getDatosCita() != null &&
                    respuestaIA.getDatosCita().getFecha() != null) {

                logger.debug("Verificando disponibilidad para cita solicitada");
                return verificarYAdaptarRespuesta(respuestaIA, tenantId);
            }

            return respuestaIA;

        } catch (Exception e) {
            logger.error("Error en procesamiento con verificacion de cierres: {}", e.getMessage(), e);
            return crearRespuestaError("Error procesando mensaje: " + e.getMessage());
        }
    }

    private OpenAIResponse verificarYAdaptarRespuesta(OpenAIResponse respuestaOriginal, String tenantId) {
        try {
            DatosCita datosCita = respuestaOriginal.getDatosCita();

            LocalDateTime fechaHoraSolicitada = parsearFechaHora(datosCita.getFecha(), datosCita.getHora());

            if (fechaHoraSolicitada == null) {
                logger.warn("No se pudo parsear fecha/hora: {} - {}",
                        datosCita.getFecha(), datosCita.getHora());
                return respuestaOriginal;
            }

            DisponibilidadResult disponibilidad = horarioEspecialService.verificarDisponibilidad(
                    tenantId,
                    fechaHoraSolicitada,
                    null,
                    null
            );

            if (disponibilidad.isDisponible()) {
                logger.debug("Fecha disponible, procediendo con respuesta original");
                return respuestaOriginal;
            }

            logger.info("Fecha no disponible, adaptando respuesta: {}", disponibilidad.getMensaje());
            return adaptarRespuestaPorCierre(respuestaOriginal, disponibilidad, tenantId);

        } catch (Exception e) {
            logger.error("Error verificando disponibilidad: {}", e.getMessage(), e);
            return respuestaOriginal;
        }
    }

    private OpenAIResponse adaptarRespuestaPorCierre(OpenAIResponse respuestaOriginal,
                                                     DisponibilidadResult disponibilidad,
                                                     String tenantId) {

        OpenAIResponse respuestaAdaptada = new OpenAIResponse();

        StringBuilder mensajeAdaptado = new StringBuilder();

        String mensajeCierre = disponibilidad.getMensaje();
        if (mensajeCierre != null && !mensajeCierre.trim().isEmpty()) {
            mensajeAdaptado.append(mensajeCierre);
        } else {
            mensajeAdaptado.append("Lo siento, esa fecha no esta disponible.");
        }

        if (disponibilidad.getFechasAlternativas() != null &&
                !disponibilidad.getFechasAlternativas().isEmpty()) {

            mensajeAdaptado.append(" ¿Te gustaria agendar para ");

            List<LocalDate> alternativas = disponibilidad.getFechasAlternativas();
            for (int i = 0; i < alternativas.size(); i++) {
                if (i > 0 && i == alternativas.size() - 1) {
                    mensajeAdaptado.append(" o ");
                } else if (i > 0) {
                    mensajeAdaptado.append(", ");
                }
                mensajeAdaptado.append(formatearFechaParaCliente(alternativas.get(i)));
            }
            mensajeAdaptado.append("?");
        } else {
            mensajeAdaptado.append(" ¿Que otra fecha te conviene?");
        }

        respuestaAdaptada.setMensaje(mensajeAdaptado.toString());
        respuestaAdaptada.setIntencion("CONSULTAR_INFO");
        respuestaAdaptada.setRequiereAccion(false);
        respuestaAdaptada.setAccion("NINGUNA");
        respuestaAdaptada.setDatosCita(respuestaOriginal.getDatosCita());

        logger.info("Respuesta adaptada por cierre: {}", mensajeAdaptado.toString());

        return respuestaAdaptada;
    }

    private LocalDateTime parsearFechaHora(String fechaTexto, String horaTexto) {
        try {
            LocalDate fecha;
            LocalTime hora;

            if ("hoy".equalsIgnoreCase(fechaTexto)) {
                fecha = LocalDate.now();
            } else if ("mañana".equalsIgnoreCase(fechaTexto) || "manana".equalsIgnoreCase(fechaTexto)) {
                fecha = LocalDate.now().plusDays(1);
            } else {
                fecha = LocalDate.parse(fechaTexto);
            }

            if (horaTexto != null && !horaTexto.trim().isEmpty()) {
                hora = LocalTime.parse(horaTexto);
            } else {
                hora = LocalTime.of(10, 0);
            }

            return LocalDateTime.of(fecha, hora);

        } catch (Exception e) {
            logger.warn("Error parseando fecha/hora: {} - {}", fechaTexto, horaTexto);
            return null;
        }
    }

    private String formatearFechaParaCliente(LocalDate fecha) {
        LocalDate hoy = LocalDate.now();

        if (fecha.equals(hoy)) {
            return "hoy";
        } else if (fecha.equals(hoy.plusDays(1))) {
            return "mañana";
        } else if (fecha.equals(hoy.plusDays(2))) {
            return "pasado mañana";
        } else {
            return fecha.toString();
        }
    }
}