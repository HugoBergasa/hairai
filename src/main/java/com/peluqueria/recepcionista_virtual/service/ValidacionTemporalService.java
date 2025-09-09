package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.Cita;
import com.peluqueria.recepcionista_virtual.model.EstadoCita;
import com.peluqueria.recepcionista_virtual.repository.CitaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@Service
public class ValidacionTemporalService {

    private static final Logger logger = LoggerFactory.getLogger(ValidacionTemporalService.class);

    @Autowired
    private TenantConfigService tenantConfigService;

    @Autowired
    private CitaRepository citaRepository;

    /**
     * CRITICO: Validar transiciones de estado validas
     */
    public void validarTransicionEstado(Cita cita, EstadoCita nuevoEstado, String usuarioId, String tenantId) {
        EstadoCita estadoActual = cita.getEstado();

        if (!esTransicionValida(estadoActual, nuevoEstado, usuarioId, tenantId)) {
            throw new RuntimeException(
                    String.format("Transición no válida de %s a %s",
                            estadoActual, nuevoEstado)
            );
        }

        // Validaciones específicas por transición
        switch (nuevoEstado) {
            case EN_PROGRESO:
                validarIniciarCita(cita, tenantId);
                break;
            case COMPLETADA:
                validarCompletarCita(cita, tenantId);
                break;
            case CANCELADA:
                validarCancelarCita(cita, usuarioId, tenantId);
                break;
        }
    }

    /**
     * CRITICO: Validar secuencia temporal de citas del mismo cliente
     */
    public void validarSecuenciaTemporal(String clienteId, LocalDateTime nuevaFecha, String tenantId) {
        if (clienteId == null) return;

        String intervaloConfig = tenantConfigService.obtenerValor(tenantId,
                "intervalo_minimo_citas_mismo_cliente", "60");

        try {
            int minutos = Integer.parseInt(intervaloConfig);

            LocalDateTime inicio = nuevaFecha.minusMinutes(minutos);
            LocalDateTime fin = nuevaFecha.plusMinutes(minutos);

            List<Cita> citasExistentes = citaRepository.findCitasClienteEnRango(
                    clienteId, inicio, fin, EstadoCita.CONFIRMADA);

            if (!citasExistentes.isEmpty()) {
                logger.warn("Cliente {} tiene cita muy cercana a {}", clienteId, nuevaFecha);

                // Por ahora solo advertir, no bloquear
                String politica = tenantConfigService.obtenerValor(tenantId,
                        "politica_intervalo_cliente", "ADVERTIR");

                if ("BLOQUEAR".equals(politica)) {
                    throw new RuntimeException(
                            String.format("El cliente ya tiene una cita muy cercana (menos de %d minutos)", minutos)
                    );
                }
            }

        } catch (NumberFormatException e) {
            logger.warn("Configuración de intervalo inválida para tenant {}: {}",
                    tenantId, intervaloConfig);
        }
    }

    /**
     * CRITICO: Validar horarios de trabajo dinamicos por tenant
     */
    public void validarHorarioTrabajoDinamico(String tenantId, LocalDateTime fechaHora, String empleadoId) {
        // 1. Validaciones específicas por día de semana
        validarRestriccionesDiaSemana(tenantId, fechaHora);

        // 2. Validaciones de horarios especiales por fecha
        validarHorariosEspecialesFecha(tenantId, fechaHora);

        // 3. Log para seguimiento
        logger.debug("Validación horario dinámico OK para tenant {} en {}",
                tenantId, fechaHora);
    }

    /**
     * CRITICO: Validar modificacion de citas existentes
     */
    public void validarModificacionCitaTemporal(Cita citaExistente, LocalDateTime nuevaFechaHora,
                                                String tenantId) {
        if (nuevaFechaHora == null) return;

        // 1. Validar que no se mueva a fecha pasada
        if (nuevaFechaHora.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("No se puede mover cita a fecha pasada");
        }

        // 2. Validar ventana de modificación
        String horasAnticipacion = tenantConfigService.obtenerValor(tenantId,
                "horas_minimas_modificacion", "2");

        try {
            int horas = Integer.parseInt(horasAnticipacion);
            LocalDateTime limite = citaExistente.getFechaHora().minusHours(horas);

            if (LocalDateTime.now().isAfter(limite)) {
                // Permitir modificación pero registrar
                logger.warn("Modificación tardía de cita {} por tenant {}",
                        citaExistente.getId(), tenantId);
            }

        } catch (NumberFormatException e) {
            logger.warn("Configuración de horas de anticpación inválida: {}", horasAnticipacion);
        }
    }

    // ========================================
    // METODOS PRIVADOS DE VALIDACION
    // ========================================

    private boolean esTransicionValida(EstadoCita estadoActual, EstadoCita nuevoEstado,
                                       String usuarioId, String tenantId) {
        // ZERO HARDCODING: Transiciones desde configuración
        String transicionesConfig = tenantConfigService.obtenerValor(tenantId,
                "transiciones_permitidas_" + estadoActual.name(), null);

        if (transicionesConfig != null) {
            boolean valida = transicionesConfig.contains(nuevoEstado.name());
            logger.debug("Transición {} -> {} para tenant {}: {}",
                    estadoActual, nuevoEstado, tenantId, valida ? "VÁLIDA" : "INVÁLIDA");
            return valida;
        }

        // Transiciones por defecto
        return validarTransicionDefecto(estadoActual, nuevoEstado);
    }

    private boolean validarTransicionDefecto(EstadoCita actual, EstadoCita nuevo) {
        switch (actual) {
            case PENDIENTE:
                return nuevo == EstadoCita.CONFIRMADA || nuevo == EstadoCita.CANCELADA;
            case CONFIRMADA:
                return nuevo == EstadoCita.EN_PROGRESO || nuevo == EstadoCita.CANCELADA ||
                        nuevo == EstadoCita.NO_ASISTIO;
            case EN_PROGRESO:
                return nuevo == EstadoCita.COMPLETADA;
            case COMPLETADA:
            case CANCELADA:
            case NO_ASISTIO:
                return false; // Estados finales
            default:
                return false;
        }
    }

    private void validarIniciarCita(Cita cita, String tenantId) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioCita = cita.getFechaHora();

        String minutosAntesConfig = tenantConfigService.obtenerValor(tenantId,
                "minutos_iniciar_antes", "15");
        String minutosRetrasoConfig = tenantConfigService.obtenerValor(tenantId,
                "minutos_retraso_maximo", "30");

        try {
            int minutosAntes = Integer.parseInt(minutosAntesConfig);
            int minutosRetraso = Integer.parseInt(minutosRetrasoConfig);

            if (ahora.isBefore(inicioCita.minusMinutes(minutosAntes))) {
                throw new RuntimeException(
                        String.format("No se puede iniciar la cita antes de %d minutos", minutosAntes)
                );
            }

            if (ahora.isAfter(inicioCita.plusMinutes(minutosRetraso))) {
                String mensaje = String.format(
                        "Cita con más de %d minutos de retraso, considere marcar como NO_ASISTIO",
                        minutosRetraso);

                // Por ahora solo advertir
                logger.warn("Cita {} con retraso excesivo: {}", cita.getId(), mensaje);
            }

        } catch (NumberFormatException e) {
            logger.warn("Configuración de tiempos inválida para tenant {}", tenantId);
        }
    }

    private void validarCompletarCita(Cita cita, String tenantId) {
        if (cita.getEstado() != EstadoCita.EN_PROGRESO) {
            throw new RuntimeException("Solo se pueden completar citas en progreso");
        }

        String duracionMinimaConfig = tenantConfigService.obtenerValor(tenantId,
                "duracion_minima_para_completar", "5");

        try {
            int duracionMinima = Integer.parseInt(duracionMinimaConfig);

            // TODO: Calcular duración real de la cita cuando se tenga timestamp de inicio
            logger.debug("Validando duración mínima de {} minutos para completar cita",
                    duracionMinima);

        } catch (NumberFormatException e) {
            logger.warn("Configuración de duración mínima inválida: {}", duracionMinimaConfig);
        }
    }

    private void validarCancelarCita(Cita cita, String usuarioId, String tenantId) {
        String horasMinimias = tenantConfigService.obtenerValor(tenantId,
                "horas_minimas_cancelacion", "2");

        try {
            int horas = Integer.parseInt(horasMinimias);
            LocalDateTime limiteCancelacion = cita.getFechaHora().minusHours(horas);

            if (LocalDateTime.now().isAfter(limiteCancelacion)) {
                logger.warn("Cancelación tardía de cita {} por usuario {} en tenant {}",
                        cita.getId(), usuarioId, tenantId);

                // Permitir pero registrar para posibles penalizaciones
            }

        } catch (NumberFormatException e) {
            logger.warn("Configuración de horas mínimas de cancelación inválida: {}", horasMinimias);
        }
    }

    private void validarRestriccionesDiaSemana(String tenantId, LocalDateTime fechaHora) {
        DayOfWeek dia = fechaHora.getDayOfWeek();
        String restricciones = tenantConfigService.obtenerValor(tenantId,
                "restricciones_" + dia.name().toLowerCase(), null);

        if (restricciones != null) {
            // TODO: Implementar restricciones específicas por día
            logger.debug("Aplicando restricciones de {} para tenant {}: {}",
                    dia.name(), tenantId, restricciones);
        }
    }

    private void validarHorariosEspecialesFecha(String tenantId, LocalDateTime fechaHora) {
        // Esta validación se complementa con HorarioEspecialService
        // Por ahora solo loggear
        logger.debug("Validando horarios especiales para {} en tenant {}",
                fechaHora.toLocalDate(), tenantId);
    }

    /**
     * UTIL: Validar periodo de gracia para modificaciones
     */
    public boolean estaDentroPeriodoGracia(Cita cita, String tenantId) {
        String horasGracia = tenantConfigService.obtenerValor(tenantId,
                "horas_periodo_gracia", "24");

        try {
            int horas = Integer.parseInt(horasGracia);
            LocalDateTime limite = cita.getFechaHora().minusHours(horas);

            return LocalDateTime.now().isBefore(limite);

        } catch (NumberFormatException e) {
            return true; // Si no se puede parsear, permitir
        }
    }

    /**
     * UTIL: Obtener proxima fecha disponible para reprogramar
     */
    public LocalDateTime sugerirProximaFechaDisponible(LocalDateTime fechaOriginal, String tenantId) {
        // Implementación básica - sugerir el día siguiente a la misma hora
        LocalDateTime sugerencia = fechaOriginal.plusDays(1);

        // TODO: Integrar con validaciones de disponibilidad más complejas
        logger.info("Sugiriendo fecha alternativa para tenant {}: {} -> {}",
                tenantId, fechaOriginal, sugerencia);

        return sugerencia;
    }
}