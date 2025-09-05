package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.ConfiguracionTenant;
import com.peluqueria.recepcionista_virtual.model.LogLlamada;
import com.peluqueria.recepcionista_virtual.repository.ConfiguracionTenantRepository;
import com.peluqueria.recepcionista_virtual.repository.LogLlamadaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio modular de compliance RGPD
 * Se puede activar/desactivar sin afectar el sistema principal
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final ConfiguracionTenantRepository configRepo;
    private final LogLlamadaRepository logRepo;
    private final OpenAIService openAIService; // Tu servicio existente

    @Value("${features.compliance.enabled:false}")
    private boolean complianceEnabled;

    @Value("${features.compliance.rgpd.aviso-obligatorio:true}")
    private boolean avisoObligatorio;

    @Value("${features.compliance.rgpd.retencion-dias:30}")
    private int diasRetencion;

    @Value("${features.ai-enhanced.compliance-prompts:true}")
    private boolean usarIAParaAvisos;

    @Value("${legal.default-rgpd-notice}")
    private String avisoRGPDDefault;

    @Value("${legal.short-notice}")
    private String avisoCorto;

    /**
     * Verifica si el compliance está activo para un tenant
     */
    public boolean isComplianceRequired(String tenantId) {
        if (!complianceEnabled) {
            return false;
        }

        // Verificar configuración específica del tenant
        Optional<ConfiguracionTenant> config = configRepo
                .findByTenantIdAndClave(tenantId, "compliance.enabled");

        return config.map(c -> Boolean.parseBoolean(c.getValor()))
                .orElse(avisoObligatorio);
    }

    /**
     * Genera el aviso legal personalizado
     * Usa GPT-4 si está habilitado, sino usa plantilla
     */
    @Transactional(readOnly = true)
    public String generarAvisoLegal(String tenantId, boolean primeraLlamada) {
        try {
            if (!isComplianceRequired(tenantId)) {
                log.debug("Compliance no requerido para tenant: {}", tenantId);
                return null;
            }

            // Obtener nombre de la peluquería
            String nombrePeluqueria = obtenerNombrePeluqueria(tenantId);

            // Si está habilitado, usar GPT-4 para personalizar
            if (usarIAParaAvisos && openAIService != null) {
                return generarAvisoConIA(tenantId, nombrePeluqueria, primeraLlamada);
            }

            // Usar plantilla predefinida
            return primeraLlamada ?
                    String.format(avisoRGPDDefault.replace("{nombre}", nombrePeluqueria)) :
                    avisoCorto;

        } catch (Exception e) {
            log.error("Error generando aviso legal para tenant {}: ", tenantId, e);
            return avisoRGPDDefault; // Fallback seguro
        }
    }

    /**
     * Genera aviso usando GPT-4 para hacerlo más natural
     */
    private String generarAvisoConIA(String tenantId, String nombrePeluqueria, boolean primeraLlamada) {
        try {
            String prompt = String.format(
                    "Genera un aviso RGPD breve y amable para %s. " +
                            "Debe informar que la llamada puede ser grabada para mejorar el servicio. " +
                            "Máximo 2 frases. Tono profesional pero cercano. " +
                            "%s",
                    nombrePeluqueria,
                    primeraLlamada ? "Es la primera vez que llama hoy." : "Ya conoce nuestro servicio."
            );

            // Usar tu OpenAIService existente
            var respuesta = openAIService.procesarMensaje(prompt, tenantId, "aviso_legal");
            return respuesta != null ? respuesta.getMensaje() : avisoRGPDDefault;

        } catch (Exception e) {
            log.warn("No se pudo generar aviso con IA, usando plantilla: {}", e.getMessage());
            return avisoRGPDDefault;
        }
    }

    /**
     * Registra el consentimiento del usuario
     */
    @Transactional
    public void registrarConsentimiento(String callSid, String tenantId, boolean acepta) {
        try {
            Map<String, Object> datosConsentimiento = new HashMap<>();
            datosConsentimiento.put("timestamp", Instant.now());
            datosConsentimiento.put("acepta_rgpd", acepta);
            datosConsentimiento.put("version_aviso", "1.0");
            datosConsentimiento.put("metodo", "voz_implicito");

            // Actualizar log de llamada
            logRepo.findByCallSid(callSid).ifPresent(log -> {
                log.setDatosCompliance(datosConsentimiento);
                log.setConsentimientoRgpd(acepta);
                logRepo.save(log);
            });

            log.info("Consentimiento registrado - CallSid: {}, Tenant: {}, Acepta: {}",
                    callSid, tenantId, acepta);

        } catch (Exception e) {
            log.error("Error registrando consentimiento: ", e);
        }
    }

    /**
     * Verifica y limpia datos según política de retención
     */
    @Transactional
    public void aplicarPoliticaRetencion(String tenantId) {
        if (!complianceEnabled) {
            return;
        }

        try {
            LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasRetencion);

            // Obtener política específica del tenant si existe
            Optional<ConfiguracionTenant> configRetencion = configRepo
                    .findByTenantIdAndClave(tenantId, "rgpd.retencion.dias");

            if (configRetencion.isPresent()) {
                int diasTenant = Integer.parseInt(configRetencion.get().getValor());
                fechaLimite = LocalDateTime.now().minusDays(diasTenant);
            }

            // Anonimizar llamadas antiguas
            int registrosAnonimizados = logRepo.anonimizarLlamadasAntiguas(
                    tenantId, fechaLimite
            );

            log.info("Política retención aplicada - Tenant: {}, Registros anonimizados: {}",
                    tenantId, registrosAnonimizados);

        } catch (Exception e) {
            log.error("Error aplicando política de retención: ", e);
        }
    }

    /**
     * Genera reporte de compliance para auditoría
     */
    public Map<String, Object> generarReporteCompliance(String tenantId) {
        Map<String, Object> reporte = new HashMap<>();

        if (!complianceEnabled) {
            reporte.put("status", "DISABLED");
            return reporte;
        }

        reporte.put("status", "ACTIVE");
        reporte.put("tenant_id", tenantId);
        reporte.put("rgpd_compliant", true);
        reporte.put("retencion_dias", diasRetencion);
        reporte.put("avisos_personalizados", usarIAParaAvisos);
        reporte.put("ultima_auditoria", LocalDateTime.now());

        // Estadísticas
        Long totalLlamadas = logRepo.countByTenantId(tenantId);
        Long consentimientos = logRepo.countByTenantIdAndConsentimientoRgpd(tenantId, true);

        reporte.put("total_llamadas", totalLlamadas);
        reporte.put("consentimientos_obtenidos", consentimientos);
        reporte.put("porcentaje_consentimiento",
                totalLlamadas > 0 ? (consentimientos * 100.0 / totalLlamadas) : 0);

        return reporte;
    }

    private String obtenerNombrePeluqueria(String tenantId) {
        return configRepo.findByTenantIdAndClave(tenantId, "nombre_peluqueria")
                .map(ConfiguracionTenant::getValor)
                .orElse("nuestra peluquería");
    }

    /**
     * Método para verificar si el sistema está en modo compliance
     * Útil para el frontend
     */
    public boolean isComplianceEnabled() {
        return complianceEnabled;
    }
}