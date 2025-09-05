package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.ConfiguracionTenant;
import com.peluqueria.recepcionista_virtual.model.Tenant;
import com.peluqueria.recepcionista_virtual.repository.ConfiguracionTenantRepository;
import com.peluqueria.recepcionista_virtual.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TenantConfigService {

    private static final Logger logger = LoggerFactory.getLogger(TenantConfigService.class);

    @Autowired
    private ConfiguracionTenantRepository configRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Value("${default.tenant.id:tenant_demo_001}")
    private String defaultTenantId;

    /**
     * Obtiene toda la configuración de un tenant como mapa
     */
    @Cacheable(value = "tenantConfig", key = "#tenantId")
    public Map<String, String> obtenerConfiguracion(String tenantId) {
        Map<String, String> config = new HashMap<>();
        List<ConfiguracionTenant> configuraciones = configRepository.findByTenantId(tenantId);

        for (ConfiguracionTenant c : configuraciones) {
            config.put(c.getClave(), c.getValor());
        }

        return config;
    }

    /**
     * Obtiene un valor específico de configuración
     */
    public String obtenerValor(String tenantId, String clave, String valorPorDefecto) {
        Optional<ConfiguracionTenant> config = configRepository.findByTenantIdAndClave(tenantId, clave);
        return config.map(ConfiguracionTenant::getValor).orElse(valorPorDefecto);
    }

    /**
     * Obtiene el mensaje de bienvenida personalizado del tenant
     */
    public String getMensajeBienvenida(String tenantId) {
        String mensaje = obtenerValor(tenantId,
                ConfiguracionTenant.Claves.MENSAJE_BIENVENIDA,
                "Hola, bienvenido a nuestra peluquería. ¿En qué puedo ayudarte hoy?");

        // Personalizar con el nombre del negocio
        String nombreNegocio = obtenerValor(tenantId,
                ConfiguracionTenant.Claves.NOMBRE_NEGOCIO,
                "nuestra peluquería");

        return mensaje.replace("{nombre_negocio}", nombreNegocio);
    }

    /**
     * Obtiene el prompt del sistema para GPT-4
     */
    public String getPromptSistema(String tenantId) {
        String promptBase = obtenerValor(tenantId,
                ConfiguracionTenant.Claves.PROMPT_SISTEMA,
                buildPromptPorDefecto(tenantId));

        return promptBase;
    }

    /**
     * Construye un prompt por defecto basado en la configuración
     */
    private String buildPromptPorDefecto(String tenantId) {
        Map<String, String> config = obtenerConfiguracion(tenantId);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres la recepcionista virtual de ")
                .append(config.getOrDefault("nombre_negocio", "la peluquería"))
                .append(".\n\n");

        prompt.append("HORARIO DE ATENCIÓN:\n");
        prompt.append("- Apertura: ").append(config.getOrDefault("horario_apertura", "09:00")).append("\n");
        prompt.append("- Cierre: ").append(config.getOrDefault("horario_cierre", "20:00")).append("\n");
        prompt.append("- Días: ").append(config.getOrDefault("dias_trabajo", "Lunes a Sábado")).append("\n\n");

        prompt.append("INSTRUCCIONES:\n");
        prompt.append("1. Sé amable y profesional en todo momento\n");
        prompt.append("2. Ayuda a los clientes a reservar citas\n");
        prompt.append("3. Si el cliente quiere reservar, pregunta:\n");
        prompt.append("   - Servicio deseado\n");
        prompt.append("   - Fecha y hora preferida\n");
        prompt.append("   - Nombre del cliente\n");
        prompt.append("4. Confirma todos los datos antes de proceder\n");
        prompt.append("5. Responde siempre en español\n\n");

        prompt.append("FORMATO DE RESPUESTA:\n");
        prompt.append("Debes responder en formato JSON con la siguiente estructura:\n");
        prompt.append("{\n");
        prompt.append("  \"mensaje\": \"tu respuesta al cliente\",\n");
        prompt.append("  \"intencion\": \"RESERVAR_CITA|CONSULTA_INFO|CANCELAR|OTRO\",\n");
        prompt.append("  \"accion\": \"CREAR_CITA|CONSULTAR_DISPONIBILIDAD|NINGUNA\",\n");
        prompt.append("  \"datos\": { ... datos relevantes ... }\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    /**
     * Encuentra el tenant por número de teléfono
     */
    public String findTenantByPhoneNumber(String phoneNumber) {
        // Normalizar el número (quitar espacios, guiones, etc.)
        String normalizedNumber = phoneNumber.replaceAll("[^0-9+]", "");

        // Buscar en la configuración de todos los tenants
        List<Tenant> tenants = tenantRepository.findByActivo(true);

        for (Tenant tenant : tenants) {
            String tenantPhone = obtenerValor(tenant.getId(),
                    ConfiguracionTenant.Claves.NUMERO_TWILIO, "");

            if (normalizedNumber.equals(tenantPhone.replaceAll("[^0-9+]", ""))) {
                return tenant.getId();
            }
        }

        // Si no se encuentra, usar el tenant por defecto
        logger.warn("No se encontró tenant para el número: {}. Usando tenant por defecto.", phoneNumber);
        return defaultTenantId;
    }

    /**
     * Actualiza un valor de configuración
     */
    @Transactional
    public void actualizarConfiguracion(String tenantId, String clave, String valor) {
        Optional<ConfiguracionTenant> configOpt =
                configRepository.findByTenantIdAndClave(tenantId, clave);

        if (configOpt.isPresent()) {
            ConfiguracionTenant config = configOpt.get();
            config.setValor(valor);
            configRepository.save(config);
        } else {
            ConfiguracionTenant nuevaConfig = new ConfiguracionTenant(tenantId, clave, valor, "GENERAL");
            configRepository.save(nuevaConfig);
        }
    }

    /**
     * Inicializa la configuración por defecto para un nuevo tenant
     */
    @Transactional
    public void inicializarConfiguracionTenant(String tenantId, String nombreNegocio) {
        logger.info("Inicializando configuración para tenant: {}", tenantId);

        // Configuración general
        crearConfiguracion(tenantId, ConfiguracionTenant.Claves.NOMBRE_NEGOCIO,
                nombreNegocio, ConfiguracionTenant.Categorias.GENERAL);

        // Horarios
        crearConfiguracion(tenantId, ConfiguracionTenant.Claves.HORARIO_APERTURA,
                "09:00", ConfiguracionTenant.Categorias.HORARIOS);
        crearConfiguracion(tenantId, ConfiguracionTenant.Claves.HORARIO_CIERRE,
                "20:00", ConfiguracionTenant.Categorias.HORARIOS);
        crearConfiguracion(tenantId, ConfiguracionTenant.Claves.DIAS_TRABAJO,
                "Lunes a Sábado", ConfiguracionTenant.Categorias.HORARIOS);
        crearConfiguracion(tenantId, ConfiguracionTenant.Claves.TIEMPO_SLOT_MINUTOS,
                "30", ConfiguracionTenant.Categorias.HORARIOS);

        // Mensajes
        crearConfiguracion(tenantId, ConfiguracionTenant.Claves.MENSAJE_BIENVENIDA,
                "Hola, bienvenido a " + nombreNegocio + ". ¿En qué puedo ayudarte?",
                ConfiguracionTenant.Categorias.MENSAJES);

        // IA
        crearConfiguracion(tenantId, ConfiguracionTenant.Claves.MODELO_GPT,
                "gpt-4", ConfiguracionTenant.Categorias.IA);
        crearConfiguracion(tenantId, ConfiguracionTenant.Claves.TEMPERATURA_IA,
                "0.7", ConfiguracionTenant.Categorias.IA);

        // Notificaciones
        crearConfiguracion(tenantId, ConfiguracionTenant.Claves.SMS_RECORDATORIO,
                "true", ConfiguracionTenant.Categorias.NOTIFICACIONES);
        crearConfiguracion(tenantId, ConfiguracionTenant.Claves.PERMITIR_CANCELACIONES,
                "true", ConfiguracionTenant.Categorias.NOTIFICACIONES);
        crearConfiguracion(tenantId, ConfiguracionTenant.Claves.HORAS_MIN_CANCELACION,
                "24", ConfiguracionTenant.Categorias.NOTIFICACIONES);
    }

    private void crearConfiguracion(String tenantId, String clave, String valor, String categoria) {
        ConfiguracionTenant config = new ConfiguracionTenant(tenantId, clave, valor, categoria);
        configRepository.save(config);
    }
}