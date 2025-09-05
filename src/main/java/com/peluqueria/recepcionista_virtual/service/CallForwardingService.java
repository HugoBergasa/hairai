package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.ConfiguracionTenant;
import com.peluqueria.recepcionista_virtual.model.Tenant;
import com.peluqueria.recepcionista_virtual.repository.ConfiguracionTenantRepository;
import com.peluqueria.recepcionista_virtual.repository.TenantRepository;
import com.twilio.rest.api.v2010.account.IncomingPhoneNumber;
import com.twilio.rest.api.v2010.account.availablephonenumbers.Local;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar desvío de llamadas y múltiples números
 * Mantiene los números originales de las peluquerías
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallForwardingService {

    private final ConfiguracionTenantRepository configRepo;
    private final TenantRepository tenantRepo;

    @Value("${features.call-forwarding.enabled:false}")
    private boolean forwardingEnabled;

    @Value("${features.call-forwarding.identify-by-forwarded:true}")
    private boolean identifyByForwarded;

    @Value("${features.call-forwarding.fallback-tenant:tenant_demo_001}")
    private String fallbackTenant;

    @Value("${features.multi-number.enabled:false}")
    private boolean multiNumberEnabled;

    @Value("${twilio.numbers.country-code:+34}")
    private String defaultCountryCode;

    @Value("${twilio.account-sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token}")
    private String twilioAuthToken;

    /**
     * Identifica el tenant basado en el número llamado o desviado
     * Estrategia en cascada para máxima compatibilidad
     */
    @Cacheable(value = "tenantByPhone", key = "#numeroDestino + '_' + #numeroDesviado")
    public String identificarTenant(String numeroDestino, String numeroDesviado) {
        try {
            // Si no está habilitado el forwarding, usar lógica simple
            if (!forwardingEnabled) {
                return identificarPorNumeroDirecto(numeroDestino);
            }

            // ESTRATEGIA 1: Si hay número desviado (ForwardedFrom)
            if (identifyByForwarded && numeroDesviado != null && !numeroDesviado.isEmpty()) {
                log.debug("Identificando tenant por número desviado: {}", numeroDesviado);
                Optional<String> tenant = buscarTenantPorNumeroOriginal(numeroDesviado);
                if (tenant.isPresent()) {
                    log.info("Tenant identificado por desvío: {} -> {}", numeroDesviado, tenant.get());
                    return tenant.get();
                }
            }

            // ESTRATEGIA 2: Por número Twilio directo
            Optional<String> tenant = buscarTenantPorNumeroTwilio(numeroDestino);
            if (tenant.isPresent()) {
                log.info("Tenant identificado por número Twilio: {} -> {}", numeroDestino, tenant.get());
                return tenant.get();
            }

            // ESTRATEGIA 3: Por mapeo personalizado (para casos especiales)
            tenant = buscarTenantPorMapeoPersonalizado(numeroDestino);
            if (tenant.isPresent()) {
                log.info("Tenant identificado por mapeo personalizado: {} -> {}", numeroDestino, tenant.get());
                return tenant.get();
            }

            // ESTRATEGIA 4: Tenant por defecto
            log.warn("No se pudo identificar tenant para números: destino={}, desviado={}. Usando fallback: {}",
                    numeroDestino, numeroDesviado, fallbackTenant);
            return fallbackTenant;

        } catch (Exception e) {
            log.error("Error identificando tenant: ", e);
            return fallbackTenant;
        }
    }

    /**
     * Configura el desvío para un tenant
     * Guarda tanto el número original como el Twilio
     */
    @Transactional
    public void configurarDesvioTenant(String tenantId, String numeroOriginal, String numeroTwilio) {
        try {
            // Normalizar números
            numeroOriginal = normalizarNumero(numeroOriginal);
            numeroTwilio = normalizarNumero(numeroTwilio);

            // Guardar número original de la peluquería
            guardarConfiguracion(tenantId, "numero.original", numeroOriginal);

            // Guardar número Twilio asignado
            guardarConfiguracion(tenantId, "numero.twilio", numeroTwilio);

            // Guardar mapeo inverso para búsqueda rápida
            guardarConfiguracion(tenantId, "forwarding.enabled", "true");
            guardarConfiguracion(tenantId, "forwarding.configured_at", String.valueOf(System.currentTimeMillis()));

            // Si multi-número está habilitado, permitir varios números
            if (multiNumberEnabled) {
                agregarNumeroAdicional(tenantId, numeroOriginal, "principal");
            }

            log.info("Desvío configurado - Tenant: {}, Original: {}, Twilio: {}",
                    tenantId, numeroOriginal, numeroTwilio);

        } catch (Exception e) {
            log.error("Error configurando desvío para tenant {}: ", tenantId, e);
            throw new RuntimeException("Error configurando desvío: " + e.getMessage());
        }
    }

    /**
     * Obtiene todos los números configurados para un tenant
     */
    @Transactional(readOnly = true)
    public Map<String, String> obtenerNumerosTenant(String tenantId) {
        Map<String, String> numeros = new HashMap<>();

        try {
            // Número original principal
            configRepo.findByTenantIdAndClave(tenantId, "numero.original")
                    .ifPresent(c -> numeros.put("original", c.getValor()));

            // Número Twilio asignado
            configRepo.findByTenantIdAndClave(tenantId, "numero.twilio")
                    .ifPresent(c -> numeros.put("twilio", c.getValor()));

            // Números adicionales si multi-número está activo
            if (multiNumberEnabled) {
                List<ConfiguracionTenant> adicionales = configRepo
                        .findByTenantIdAndClaveStartingWith(tenantId, "numero.adicional.");

                for (ConfiguracionTenant config : adicionales) {
                    String tipo = config.getClave().replace("numero.adicional.", "");
                    numeros.put("adicional_" + tipo, config.getValor());
                }
            }

        } catch (Exception e) {
            log.error("Error obteniendo números del tenant {}: ", tenantId, e);
        }

        return numeros;
    }

    /**
     * Verifica si un número está disponible para desvío
     */
    public boolean verificarNumeroDisponible(String numero) {
        String numeroNormalizado = normalizarNumero(numero);

        // Verificar que no esté ya asignado
        Optional<ConfiguracionTenant> existente = configRepo
                .findByClaveAndValor("numero.original", numeroNormalizado);

        return existente.isEmpty();
    }

    /**
     * Genera instrucciones de desvío específicas por operador
     */
    public Map<String, String> generarInstruccionesDesvio(String operador, String numeroTwilio) {
        Map<String, String> instrucciones = new HashMap<>();

        String codigoActivacion = "";
        String codigoDesactivacion = "";
        String verificacion = "";

        switch (operador.toLowerCase()) {
            case "movistar":
            case "telefonica":
            case "o2":
                codigoActivacion = "**21*" + numeroTwilio + "#";
                codigoDesactivacion = "##21#";
                verificacion = "*#21#";
                break;

            case "vodafone":
                codigoActivacion = "**21*" + numeroTwilio + "#";
                codigoDesactivacion = "##21#";
                verificacion = "*#21#";
                break;

            case "orange":
            case "amena":
                codigoActivacion = "**21*" + numeroTwilio + "#";
                codigoDesactivacion = "##21#";
                verificacion = "*#21#";
                break;

            case "yoigo":
            case "masmovil":
                codigoActivacion = "**21*" + numeroTwilio + "#";
                codigoDesactivacion = "##21#";
                verificacion = "*#21#";
                break;

            default:
                codigoActivacion = "**21*" + numeroTwilio + "#";
                codigoDesactivacion = "##21#";
                verificacion = "*#21#";
        }

        instrucciones.put("activar", codigoActivacion);
        instrucciones.put("desactivar", codigoDesactivacion);
        instrucciones.put("verificar", verificacion);
        instrucciones.put("operador", operador);
        instrucciones.put("numero_destino", numeroTwilio);

        // Instrucciones adicionales
        instrucciones.put("nota", "Marque el código desde el teléfono que desea desviar");
        instrucciones.put("coste", "El coste del desvío depende de su tarifa con " + operador);

        return instrucciones;
    }

    /**
     * Busca números españoles disponibles en Twilio
     */
    public List<Map<String, String>> buscarNumerosDisponibles(String codigoArea) {
        List<Map<String, String>> numerosDisponibles = new ArrayList<>();

        try {
            if (!forwardingEnabled) {
                log.warn("Forwarding no está habilitado");
                return numerosDisponibles;
            }

            // Inicializar cliente Twilio (usar tu configuración existente)
            // Este es un ejemplo, adapta según tu TwilioService actual

            // Por ahora retornamos números de ejemplo
            // En producción, esto consultaría la API de Twilio
            Map<String, String> numeroEjemplo = new HashMap<>();
            numeroEjemplo.put("numero", "+34910123456");
            numeroEjemplo.put("ciudad", "Madrid");
            numeroEjemplo.put("precio_mensual", "15.00 EUR");
            numeroEjemplo.put("capacidades", "voz, sms");
            numerosDisponibles.add(numeroEjemplo);

        } catch (Exception e) {
            log.error("Error buscando números disponibles: ", e);
        }

        return numerosDisponibles;
    }

    // ===== MÉTODOS PRIVADOS DE BÚSQUEDA =====

    private Optional<String> buscarTenantPorNumeroOriginal(String numero) {
        String numeroNormalizado = normalizarNumero(numero);
        return configRepo.findByClaveAndValor("numero.original", numeroNormalizado)
                .map(ConfiguracionTenant::getTenantId);
    }

    private Optional<String> buscarTenantPorNumeroTwilio(String numero) {
        String numeroNormalizado = normalizarNumero(numero);
        return configRepo.findByClaveAndValor("numero.twilio", numeroNormalizado)
                .map(ConfiguracionTenant::getTenantId);
    }

    private Optional<String> buscarTenantPorMapeoPersonalizado(String numero) {
        String numeroNormalizado = normalizarNumero(numero);
        return configRepo.findByClaveAndValor("numero.mapeo.custom", numeroNormalizado)
                .map(ConfiguracionTenant::getTenantId);
    }

    private String identificarPorNumeroDirecto(String numero) {
        return buscarTenantPorNumeroTwilio(numero)
                .orElse(fallbackTenant);
    }

    private void guardarConfiguracion(String tenantId, String clave, String valor) {
        ConfiguracionTenant config = configRepo
                .findByTenantIdAndClave(tenantId, clave)
                .orElse(new ConfiguracionTenant());

        config.setTenantId(tenantId);
        config.setClave(clave);
        config.setValor(valor);
        config.setActualizadoEn(new Date());

        configRepo.save(config);
    }

    private void agregarNumeroAdicional(String tenantId, String numero, String tipo) {
        String clave = "numero.adicional." + tipo;
        guardarConfiguracion(tenantId, clave, numero);
    }

    /**
     * Normaliza números de teléfono al formato E.164
     */
    private String normalizarNumero(String numero) {
        if (numero == null) return null;

        // Eliminar espacios y caracteres especiales
        numero = numero.replaceAll("[\\s\\-\\(\\)]", "");

        // Si no empieza con +, agregar código de país por defecto
        if (!numero.startsWith("+")) {
            if (numero.startsWith("00")) {
                numero = "+" + numero.substring(2);
            } else if (numero.length() == 9 && (numero.startsWith("6") || numero.startsWith("7") || numero.startsWith("9"))) {
                // Número español sin código de país
                numero = defaultCountryCode + numero;
            }
        }

        return numero;
    }
}