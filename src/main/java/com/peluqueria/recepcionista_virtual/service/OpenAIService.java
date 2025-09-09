package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.dto.DatosCita;
import com.peluqueria.recepcionista_virtual.dto.DisponibilidadResult;
import com.peluqueria.recepcionista_virtual.dto.OpenAIResponse;
import com.peluqueria.recepcionista_virtual.model.*;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private ServicioRepository servicioRepository;

    @Autowired
    private HorarioEspecialService horarioEspecialService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * PROCESAMIENTO INTELIGENTE CORREGIDO - GPT-4 COMO CEREBRO PERSONALIZADO POR TENANT
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

            // 3. CONSTRUIR PROMPT PERSONALIZADO CON SERVICIOS DINÁMICOS DESDE BD
            String systemPrompt = construirPromptPersonalizadoDinamico(tenant);

            // 4. LLAMAR A GPT-4 CON CONTEXTO COMPLETO
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4-turbo",
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
     * CONSTRUIR PROMPT PERSONALIZADO CON SERVICIOS DE BD REAL
     */
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
     * BD COMPATIBLE: Construir servicios usando query que existe en el schema
     */
    private String construirServiciosDesdeDB(String tenantId) {
        try {
            // BD COMPATIBLE: Usar método que existe según schema
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
     * CONSTRUIR INFORMACIÓN DE CIERRES ESPECIALES PARA EL PROMPT
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
                        cierre.getTipoCierre().name(), // BD COMPATIBLE: usar name() en lugar de método inexistente
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
     * CREAR RESPUESTA MOCK PERSONALIZADA POR TENANT
     */
    private OpenAIResponse crearRespuestaMock(String mensaje, String tenantId) {
        OpenAIResponse respuesta = new OpenAIResponse();

        // Obtener servicios reales para el mock también
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
     * PROCESAMIENTO CON VERIFICACIÓN DE CIERRES
     */
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
    // AGREGAR ESTOS MÉTODOS AL OpenAIService.java EXISTENTE:

// ========================================
// MÉTODOS DE ANÁLISIS INTELIGENTE - AGREGAR
// ========================================

    /**
     * CEREBRO OPENAI: Analizar conflictos de disponibilidad con sugerencias
     */
    public String analizarConflictoDisponibilidad(String tenantId, String errorMsg, LocalDateTime fechaHora) {
        try {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) return "Análisis no disponible";

            if (apiKey == null || apiKey.equals("sk-dummy") || apiKey.startsWith("sk-proj-tu-clave")) {
                logger.warn("OpenAI API Key no configurada - usando análisis mock");
                return generarAnalisisMock(errorMsg, fechaHora);
            }

            String prompt = String.format(
                    "Como experto en gestión de citas para %s, analiza este conflicto: '%s'. " +
                            "Fecha solicitada: %s. " +
                            "Proporciona: 1) Causa probable del conflicto, 2) 3 alternativas específicas de fecha/hora, " +
                            "3) Recomendación para evitar futuros conflictos similares. " +
                            "Responde en español, máximo 150 palabras, tono profesional pero empático.",
                    tenant.getNombrePeluqueria(),
                    errorMsg,
                    fechaHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );

            String respuesta = llamarOpenAI(prompt, "gpt-4", 0.3);
            logger.debug("Análisis IA generado para conflicto: {}", respuesta.substring(0, Math.min(100, respuesta.length())));

            return respuesta;

        } catch (Exception e) {
            logger.error("Error en análisis IA de conflicto: {}", e.getMessage());
            return generarAnalisisMock(errorMsg, fechaHora);
        }
    }

    /**
     * CEREBRO OPENAI: Generar mensajes de error personalizados por tenant
     */
    public String generarMensajeError(String tenantId, String contextoError) {
        try {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) return "Error en operación";

            if (apiKey == null || apiKey.equals("sk-dummy") || apiKey.startsWith("sk-proj-tu-clave")) {
                return generarMensajeErrorMock(contextoError, tenant.getNombrePeluqueria());
            }

            String prompt = String.format(
                    "Genera un mensaje de error profesional y empático para %s. " +
                            "Contexto del error: %s. " +
                            "Estilo: profesional pero cálido, en español. " +
                            "Máximo 50 palabras. " +
                            "Incluye una sugerencia constructiva si es posible.",
                    tenant.getNombrePeluqueria(),
                    traducirContextoError(contextoError)
            );

            String respuesta = llamarOpenAI(prompt, "gpt-4", 0.7);
            logger.debug("Mensaje de error IA generado: {}", respuesta);

            return respuesta;

        } catch (Exception e) {
            logger.error("Error generando mensaje con IA: {}", e.getMessage());
            return generarMensajeErrorMock(contextoError, "el salón");
        }
    }

    /**
     * CEREBRO OPENAI: Sugerir horarios alternativos optimizados
     */
    public List<LocalDateTime> sugerirHorariosAlternativos(String tenantId, LocalDateTime fechaDeseada,
                                                           String servicioId, String empleadoId) {
        try {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) return generarAlternativasBasicas(fechaDeseada);

            if (apiKey == null || apiKey.equals("sk-dummy") || apiKey.startsWith("sk-proj-tu-clave")) {
                return generarAlternativasBasicas(fechaDeseada);
            }

            String prompt = String.format(
                    "Para %s, el cliente quería cita el %s pero no está disponible. " +
                            "Horario del negocio: %s a %s, días %s. " +
                            "Sugiere 3 horarios alternativos óptimos considerando: " +
                            "1) Cercanía a fecha deseada, 2) Horarios populares (mañana y tarde), 3) Distribución de carga. " +
                            "Responde solo las fechas en formato 'dd/MM/yyyy HH:mm', una por línea.",
                    tenant.getNombrePeluqueria(),
                    fechaDeseada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    tenant.getHoraApertura() != null ? tenant.getHoraApertura() : "09:00",
                    tenant.getHoraCierre() != null ? tenant.getHoraCierre() : "20:00",
                    tenant.getDiasLaborables() != null ? tenant.getDiasLaborables() : "L-S"
            );

            String respuestaIA = llamarOpenAI(prompt, "gpt-4", 0.3);
            List<LocalDateTime> fechas = parsearFechasAlternativas(respuestaIA);

            logger.debug("IA sugirió {} alternativas para {}", fechas.size(), fechaDeseada);

            return fechas.isEmpty() ? generarAlternativasBasicas(fechaDeseada) : fechas;

        } catch (Exception e) {
            logger.error("Error generando alternativas con IA: {}", e.getMessage());
            return generarAlternativasBasicas(fechaDeseada);
        }
    }

    /**
     * CEREBRO OPENAI: Análisis de patrones de citas para optimización
     */
    public String analizarPatronesCitas(String tenantId, List<Cita> citasRecientes) {
        try {
            if (citasRecientes.isEmpty()) {
                return "Sin datos suficientes para análisis de patrones";
            }

            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) return "Análisis no disponible";

            if (apiKey == null || apiKey.equals("sk-dummy") || apiKey.startsWith("sk-proj-tu-clave")) {
                return generarAnalisisPatronesMock(citasRecientes.size());
            }

            // Crear resumen estadístico de las citas
            String resumenEstadistico = crearResumenEstadistico(citasRecientes);

            String prompt = String.format(
                    "Analiza estos patrones de citas para %s: %s. " +
                            "Identifica: 1) Horarios más demandados, 2) Servicios populares, " +
                            "3) Días con mayor actividad, 4) Recomendaciones para optimizar agenda. " +
                            "Responde en español, máximo 200 palabras, enfoque práctico.",
                    tenant.getNombrePeluqueria(),
                    resumenEstadistico
            );

            return llamarOpenAI(prompt, "gpt-4", 0.5);

        } catch (Exception e) {
            logger.error("Error analizando patrones con IA: {}", e.getMessage());
            return "Error generando análisis de patrones";
        }
    }

    /**
     * CEREBRO OPENAI: Generar recomendaciones personalizadas por cliente
     */
    public String generarRecomendacionesCliente(String tenantId, Cliente cliente,
                                                List<Cita> historialCitas) {
        try {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) return "Recomendaciones no disponibles";

            if (apiKey == null || apiKey.equals("sk-dummy") || apiKey.startsWith("sk-proj-tu-clave")) {
                return generarRecomendacionesMock(cliente.getNombre(), historialCitas.size());
            }

            String historialResumen = crearResumenHistorialCliente(historialCitas);

            String prompt = String.format(
                    "Para %s en %s, cliente: %s. Historial: %s. " +
                            "Genera recomendaciones personalizadas de: 1) Servicios complementarios, " +
                            "2) Frecuencia óptima de visitas, 3) Horarios que le convendrían mejor. " +
                            "Tono amigable, máximo 120 palabras.",
                    cliente.getNombre(),
                    tenant.getNombrePeluqueria(),
                    cliente.getNombre(),
                    historialResumen
            );

            return llamarOpenAI(prompt, "gpt-4", 0.8);

        } catch (Exception e) {
            logger.error("Error generando recomendaciones cliente: {}", e.getMessage());
            return "Gracias por su confianza. Consulte con nuestros profesionales para recomendaciones personalizadas.";
        }
    }

// ========================================
// MÉTODOS AUXILIARES PRIVADOS
// ========================================

    private String traducirContextoError(String contexto) {
        Map<String, String> traducciones = Map.of(
                "empleado_inactivo", "empleado no disponible o inactivo",
                "empleado_no_autorizado", "empleado no autorizado para este servicio",
                "servicio_no_disponible", "servicio no disponible en este momento",
                "capacidad_excedida", "capacidad máxima del salón alcanzada",
                "horario_invalido", "horario fuera del rango permitido",
                "transicion_invalida", "cambio de estado no permitido",
                "cliente_duplicado", "cliente ya tiene cita programada",
                "fecha_retroactiva", "no se permiten citas en fechas pasadas"
        );
        return traducciones.getOrDefault(contexto, contexto);
    }

    private String generarAnalisisMock(String errorMsg, LocalDateTime fechaHora) {
        StringBuilder analisis = new StringBuilder();

        if (errorMsg.toLowerCase().contains("empleado")) {
            analisis.append("El profesional seleccionado no está disponible en ese horario. ");
            analisis.append("Alternativas: ").append(fechaHora.plusHours(2).format(DateTimeFormatter.ofPattern("HH:mm")));
            analisis.append(" mismo día, o ").append(fechaHora.plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
            analisis.append(". Recomendación: Reserve con más anticipación.");
        } else if (errorMsg.toLowerCase().contains("capacidad")) {
            analisis.append("Horario de alta demanda. ");
            analisis.append("Alternativas: ").append(fechaHora.minusHours(1).format(DateTimeFormatter.ofPattern("HH:mm")));
            analisis.append(" o ").append(fechaHora.plusHours(3).format(DateTimeFormatter.ofPattern("HH:mm")));
            analisis.append(" mismo día. Recomendación: Horarios de mañana suelen tener más disponibilidad.");
        } else {
            analisis.append("Horario no disponible. ");
            analisis.append("Alternativas: ").append(fechaHora.plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM")));
            analisis.append(" a la misma hora, o horarios de mañana el mismo día.");
        }

        return analisis.toString();
    }

    private String generarMensajeErrorMock(String contexto, String nombreSalon) {
        Map<String, String> mensajesMock = Map.of(
                "empleado_inactivo", "El profesional seleccionado no está disponible. ¿Le ayudamos con otro horario?",
                "empleado_no_autorizado", "Este profesional no realiza el servicio solicitado. Consulte nuestras especializaciones.",
                "capacidad_excedida", "Horario muy solicitado. ¿Qué tal una hora antes o después?",
                "horario_invalido", "Horario fuera de nuestro servicio. Atendemos de 9:00 a 20:00.",
                "transicion_invalida", "No es posible realizar ese cambio. Consulte con recepción."
        );

        return mensajesMock.getOrDefault(contexto,
                "Inconveniente temporal en " + nombreSalon + ". ¿Podemos ofrecerle una alternativa?");
    }

    private List<LocalDateTime> parsearFechasAlternativas(String respuestaIA) {
        List<LocalDateTime> fechas = new ArrayList<>();

        try {
            String[] lineas = respuestaIA.split("\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (String linea : lineas) {
                linea = linea.trim();
                if (linea.matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}")) {
                    try {
                        LocalDateTime fecha = LocalDateTime.parse(linea, formatter);
                        if (fecha.isAfter(LocalDateTime.now())) {
                            fechas.add(fecha);
                        }
                    } catch (Exception e) {
                        logger.debug("Error parseando fecha alternativa: {}", linea);
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("Error parseando respuesta IA para fechas alternativas: {}", e.getMessage());
        }

        return fechas;
    }

    private List<LocalDateTime> generarAlternativasBasicas(LocalDateTime fechaOriginal) {
        List<LocalDateTime> alternativas = new ArrayList<>();
        LocalDateTime base = fechaOriginal;

        // Alternativa 1: 2 horas después el mismo día
        alternativas.add(base.plusHours(2));

        // Alternativa 2: Día siguiente a la misma hora
        alternativas.add(base.plusDays(1));

        // Alternativa 3: 2 días después a las 10:00
        alternativas.add(base.plusDays(2).withHour(10).withMinute(0));

        return alternativas;
    }

    private String crearResumenEstadistico(List<Cita> citas) {
        Map<String, Integer> servicios = new HashMap<>();
        Map<Integer, Integer> horas = new HashMap<>();
        Map<String, Integer> dias = new HashMap<>();

        for (Cita cita : citas) {
            // Contar servicios
            String servicio = cita.getServicio() != null ? cita.getServicio().getNombre() : "Sin servicio";
            servicios.put(servicio, servicios.getOrDefault(servicio, 0) + 1);

            // Contar horas
            int hora = cita.getFechaHora().getHour();
            horas.put(hora, horas.getOrDefault(hora, 0) + 1);

            // Contar días de semana
            String dia = cita.getFechaHora().getDayOfWeek().toString();
            dias.put(dia, dias.getOrDefault(dia, 0) + 1);
        }

        return String.format("Total citas: %d. Servicios populares: %s. Horas pico: %s. Días activos: %s",
                citas.size(),
                servicios.entrySet().stream().limit(3).map(e -> e.getKey() + "(" + e.getValue() + ")")
                        .collect(Collectors.joining(", ")),
                horas.entrySet().stream().limit(3).map(e -> e.getKey() + ":00(" + e.getValue() + ")")
                        .collect(Collectors.joining(", ")),
                dias.entrySet().stream().limit(3).map(e -> e.getKey() + "(" + e.getValue() + ")")
                        .collect(Collectors.joining(", "))
        );
    }

    private String crearResumenHistorialCliente(List<Cita> historial) {
        if (historial.isEmpty()) return "Cliente nuevo";

        Map<String, Integer> serviciosCliente = new HashMap<>();
        for (Cita cita : historial) {
            String servicio = cita.getServicio() != null ? cita.getServicio().getNombre() : "Sin servicio";
            serviciosCliente.put(servicio, serviciosCliente.getOrDefault(servicio, 0) + 1);
        }

        String servicioFavorito = serviciosCliente.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Ninguno");

        return String.format("%d visitas, servicio preferido: %s",
                historial.size(), servicioFavorito);
    }

    private String generarAnalisisPatronesMock(int numCitas) {
        return String.format("Análisis de %d citas: Los horarios de 10:00-12:00 y 16:00-18:00 " +
                        "son los más demandados. Los servicios de corte y color representan el 70%% de reservas. " +
                        "Recomendación: Ampliar disponibilidad en horarios pico y considerar servicios complementarios.",
                numCitas);
    }

    private String generarRecomendacionesMock(String nombreCliente, int numVisitas) {
        if (numVisitas == 0) {
            return String.format("Bienvenido/a %s. Le recomendamos comenzar con nuestro servicio signature " +
                    "y agendar su próxima cita en 4-6 semanas para mantener el look perfecto.", nombreCliente);
        } else {
            return String.format("Gracias por su confianza, %s. Basado en su historial de %d visitas, " +
                            "le sugerimos complementar con tratamientos nutritivos y mantener frecuencia mensual.",
                    nombreCliente, numVisitas);
        }
    }

    // AGREGAR ESTE MÉTODO AL FINAL DE OpenAIService.java (antes del último })

    /**
     * Método principal para llamar a la API de OpenAI
     *
     * @param prompt El prompt a enviar
     * @param model El modelo a usar (ej: "gpt-4", "gpt-3.5-turbo")
     * @param temperature Temperatura para creatividad (0.0 - 1.0)
     * @return Respuesta de OpenAI o mensaje de fallback
     */
    private String llamarOpenAI(String prompt, String model, double temperature) {
        try {
            // Validar API Key
            if (apiKey == null || apiKey.equals("sk-dummy") || apiKey.startsWith("sk-proj-tu-clave")) {
                logger.warn("OpenAI API Key no configurada - usando respuesta fallback");
                return generarRespuestaFallback(prompt);
            }

            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Construir request body
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", temperature,
                    "max_tokens", 500
            );

            logger.debug("Enviando request a OpenAI - Modelo: {}, Temperature: {}", model, temperature);

            // Hacer llamada a OpenAI
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.openai.com/v1/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // Procesar respuesta
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("choices")) {
                logger.error("Respuesta de OpenAI inválida: {}", responseBody);
                return generarRespuestaFallback(prompt);
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices.isEmpty()) {
                logger.error("No hay respuestas de OpenAI");
                return generarRespuestaFallback(prompt);
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            String content = (String) message.get("content");

            logger.debug("Respuesta de OpenAI obtenida exitosamente");
            return content.trim();

        } catch (Exception e) {
            logger.error("Error llamando a OpenAI: {}", e.getMessage(), e);
            return generarRespuestaFallback(prompt);
        }
    }

    /**
     * Generar respuesta fallback cuando OpenAI no está disponible
     */
    private String generarRespuestaFallback(String prompt) {
        // Análisis básico del prompt para dar respuesta apropiada
        String promptLower = prompt.toLowerCase();

        if (promptLower.contains("conflicto") || promptLower.contains("disponibilidad")) {
            return "El horario solicitado presenta conflictos. " +
                    "Alternativas sugeridas: pruebe 1-2 horas antes o después, " +
                    "o el mismo horario al día siguiente. " +
                    "Recomendación: reserve con más anticipación para mayor disponibilidad.";
        }

        if (promptLower.contains("error") || promptLower.contains("problema")) {
            return "Se ha producido un inconveniente técnico temporal. " +
                    "Le sugerimos intentar nuevamente en unos minutos " +
                    "o contactar directamente con el establecimiento.";
        }

        if (promptLower.contains("cancelacion") || promptLower.contains("cancelar")) {
            return "Su cita ha sido procesada. " +
                    "Para más información o cambios adicionales, " +
                    "puede contactar directamente con nosotros.";
        }

        if (promptLower.contains("cliente") || promptLower.contains("recomendacion")) {
            return "Gracias por su confianza en nuestros servicios. " +
                    "Para recomendaciones personalizadas, " +
                    "consulte con nuestros profesionales durante su próxima visita.";
        }

        // Fallback genérico
        return "Información procesada correctamente. " +
                "Para asistencia adicional, no dude en contactarnos directamente.";
    }

    public String obtenerRespuestaSimple(String prompt, String tenantId) {
        try {
            // Configurar el request para OpenAI
            Map<String, Object> request = new HashMap<>();
            request.put("model", "gpt-4");
            request.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));
            request.put("max_tokens", 150);
            request.put("temperature", 0.7);

            // Hacer la llamada a OpenAI API
            // IMPLEMENTAR según tu configuración actual de OpenAI

            return "Respuesta de OpenAI para el prompt"; // Placeholder

        } catch (Exception e) {
            logger.error("Error llamando a OpenAI: {}", e.getMessage());
            return "Respuesta no disponible temporalmente";
        }
    }
}