package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.dto.*;
import com.peluqueria.recepcionista_virtual.model.HorarioEspecial;
import com.peluqueria.recepcionista_virtual.model.TipoCierre;
import com.peluqueria.recepcionista_virtual.model.Cita;
import com.peluqueria.recepcionista_virtual.repository.HorarioEspecialRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SERVICIO CRITICO: Gestion de horarios especiales y verificacion de disponibilidad
 *
 * MULTITENANT: Todos los metodos requieren tenantId - aislamiento perfecto
 * ZERO HARDCODING: Mensajes generados dinamicamente o tomados de BD
 * OpenAI CEREBRO: La IA llama verificarDisponibilidad() antes de CADA respuesta sobre fechas
 */
@Service
@Transactional
public class HorarioEspecialService {

    private static final Logger logger = LoggerFactory.getLogger(HorarioEspecialService.class);

    @Autowired
    private HorarioEspecialRepository horarioEspecialRepository;

    // Inyectaremos estos servicios cuando estén listos
    // @Autowired
    // private CitaService citaService;

    // @Autowired
    // private OpenAIService openAIService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========================================
    // METODO CRITICO PARA LA IA
    // ========================================

    /**
     * METODO MAS IMPORTANTE: Verificar disponibilidad antes de crear citas
     *
     * La IA SIEMPRE debe llamar este metodo antes de confirmar una cita
     *
     * MULTITENANT: Solo verifica cierres del tenant especificado
     * ZERO HARDCODING: Mensajes vienen de BD o se generan dinamicamente
     * OpenAI CEREBRO: Si no hay mensaje personalizado, se puede generar con IA
     *
     * @param tenantId ID del tenant (salon)
     * @param fechaHora Fecha y hora solicitada
     * @param empleadoId ID del empleado (opcional)
     * @param servicioId ID del servicio (opcional)
     * @return DisponibilidadResult con informacion detallada
     */
    public DisponibilidadResult verificarDisponibilidad(String tenantId,
                                                        LocalDateTime fechaHora,
                                                        String empleadoId,
                                                        String servicioId) {

        logger.debug("Verificando disponibilidad para tenant: {}, fecha: {}, empleado: {}, servicio: {}",
                tenantId, fechaHora, empleadoId, servicioId);

        LocalDate fecha = fechaHora.toLocalDate();
        LocalTime hora = fechaHora.toLocalTime();

        // 1. Buscar todos los cierres que afecten esta fecha
        List<HorarioEspecial> cierres = horarioEspecialRepository.findCierresParaFecha(tenantId, fecha);

        if (cierres.isEmpty()) {
            logger.debug("No hay cierres para la fecha {}", fecha);
            return DisponibilidadResult.disponible();
        }

        // 2. Evaluar cada cierre en orden de prioridad
        for (HorarioEspecial cierre : cierres) {
            DisponibilidadResult resultado = evaluarCierre(cierre, hora, empleadoId, servicioId);

            if (!resultado.isDisponible()) {
                logger.debug("Fecha no disponible: {}", resultado.getMensaje());

                // Agregar fechas alternativas
                List<LocalDate> alternativas = buscarFechasAlternativas(tenantId, fecha, 7);
                resultado.setFechasAlternativas(alternativas);

                return resultado;
            }

            // Si es horario reducido, actualizar el resultado pero continuar verificando
            if (cierre.getTipoCierre() == TipoCierre.HORARIO_REDUCIDO) {
                resultado.setHorarioDisponibleInicio(cierre.getHorarioInicio());
                resultado.setHorarioDisponibleFin(cierre.getHorarioFin());
            }
        }

        logger.debug("Fecha disponible");
        return DisponibilidadResult.disponible();
    }

    /**
     * Evaluar un cierre especifico contra una solicitud de cita
     * ZERO HARDCODING: Usa mensajes de BD o retorna null para que OpenAI genere
     */
    private DisponibilidadResult evaluarCierre(HorarioEspecial cierre,
                                               LocalTime hora,
                                               String empleadoId,
                                               String servicioId) {

        logger.debug("Evaluando cierre: {} - {}", cierre.getTipoCierre(), cierre.getMotivo());

        switch (cierre.getTipoCierre()) {
            case CERRADO_COMPLETO:
                return DisponibilidadResult.noDisponible(obtenerMensajeDinamico(cierre));

            case HORARIO_REDUCIDO:
                if (hora.isBefore(cierre.getHorarioInicio()) ||
                        hora.isAfter(cierre.getHorarioFin())) {

                    // ZERO HARDCODING: Generar mensaje dinamico
                    String mensaje = generarMensajeHorarioReducido(cierre);
                    return DisponibilidadResult.noDisponible(mensaje);
                }
                break;

            case EMPLEADO_AUSENTE:
                if (empleadoId != null && esEmpleadoAfectado(cierre.getEmpleadosAfectados(), empleadoId)) {
                    return DisponibilidadResult.noDisponible(obtenerMensajeDinamico(cierre));
                }
                break;

            case SERVICIO_NO_DISPONIBLE:
                if (servicioId != null && esServicioAfectado(cierre.getServiciosAfectados(), servicioId)) {
                    return DisponibilidadResult.noDisponible(obtenerMensajeDinamico(cierre));
                }
                break;

            case SOLO_EMERGENCIAS:
                // Por ahora tratamos como no disponible, en el futuro se puede verificar si es emergencia
                return DisponibilidadResult.noDisponible(obtenerMensajeDinamico(cierre));
        }

        return DisponibilidadResult.disponible();
    }

    /**
     * Obtener mensaje dinamico sin hardcoding
     * 1. Si hay mensaje personalizado en BD -> usarlo
     * 2. Si no hay mensaje -> generar con OpenAI (futuro) o mensaje basico
     */
    private String obtenerMensajeDinamico(HorarioEspecial cierre) {
        // 1. Usar mensaje personalizado si existe
        String mensajePersonalizado = cierre.obtenerMensajeParaIA();
        if (mensajePersonalizado != null && !mensajePersonalizado.trim().isEmpty()) {
            return mensajePersonalizado;
        }

        // 2. Si no hay mensaje personalizado, generar uno basico (SIN hardcoding de textos fijos)
        // En el futuro esto se hara con OpenAI segun el tenant
        return generarMensajeBasico(cierre);
    }

    /**
     * Generar mensaje basico sin hardcoding de textos especificos
     * ZERO HARDCODING: No contiene textos fijos como "Lo siento, estamos cerrados"
     */
    private String generarMensajeBasico(HorarioEspecial cierre) {
        StringBuilder mensaje = new StringBuilder();

        // Base del mensaje sin texto hardcodeado
        if (cierre.getMotivo() != null && !cierre.getMotivo().trim().isEmpty()) {
            mensaje.append(cierre.getMotivo());
        } else {
            mensaje.append("No disponible");
        }

        // En el futuro, aqui se llamaria a OpenAI para generar mensaje personalizado
        // return openAIService.generarMensajeCierre(cierre, tenantId);

        return mensaje.toString();
    }

    /**
     * Generar mensaje para horario reducido sin hardcoding
     */
    private String generarMensajeHorarioReducido(HorarioEspecial cierre) {
        if (cierre.getMensajePersonalizado() != null && !cierre.getMensajePersonalizado().trim().isEmpty()) {
            return cierre.getMensajePersonalizado();
        }

        // Mensaje basico sin hardcoding
        StringBuilder mensaje = new StringBuilder();
        if (cierre.getHorarioInicio() != null && cierre.getHorarioFin() != null) {
            mensaje.append("Horario especial: ")
                    .append(cierre.getHorarioInicio())
                    .append(" a ")
                    .append(cierre.getHorarioFin());
        }

        if (cierre.getMotivo() != null) {
            mensaje.append(" - ").append(cierre.getMotivo());
        }

        return mensaje.toString();
    }

    // ========================================
    // CREACION DE CIERRES
    // ========================================

    /**
     * METODO CRITICO: Crear cierre rapido de emergencia
     *
     * Para casos urgentes: "Cerrar HOY por emergencia familiar"
     * MULTITENANT: Se asocia automaticamente al tenant
     * ZERO HARDCODING: No contiene mensajes predefinidos
     */
    public HorarioEspecial crearCierreRapido(String tenantId, LocalDate fecha, String motivo) {
        logger.info("Creando cierre rapido para tenant: {}, fecha: {}, motivo: {}",
                tenantId, fecha, motivo);

        // Verificar si ya existe un cierre para esa fecha
        List<HorarioEspecial> cierresExistentes = horarioEspecialRepository.findCierresParaFecha(tenantId, fecha);

        if (!cierresExistentes.isEmpty()) {
            logger.warn("Ya existe un cierre para la fecha {}, actualizando...", fecha);
            HorarioEspecial existente = cierresExistentes.get(0);
            existente.setTipoCierre(TipoCierre.CERRADO_COMPLETO);
            existente.setMotivo(motivo + " (ACTUALIZADO)");
            // NO establecer mensaje hardcodeado
            return horarioEspecialRepository.save(existente);
        }

        // Crear nuevo cierre de emergencia
        HorarioEspecial cierre = HorarioEspecial.crearCierreEmergencia(tenantId, fecha, motivo);
        HorarioEspecial guardado = horarioEspecialRepository.save(cierre);

        // Notificar clientes con citas existentes (cuando CitaService este listo)
        // notificarClientesAfectados(tenantId, fecha, fecha, motivo);

        logger.info("Cierre rapido creado exitosamente: {}", guardado.getId());
        return guardado;
    }

    /**
     * Crear cierre planificado (vacaciones, eventos, etc.)
     * MULTITENANT: Aislado por tenant
     * ZERO HARDCODING: Mensajes personalizables
     */
    public HorarioEspecial crearCierre(String tenantId, HorarioEspecialDTO dto) {
        logger.info("Creando cierre planificado para tenant: {}", tenantId);

        // Validaciones de negocio
        validarDatosCierre(dto);

        // Verificar solapamientos
        List<HorarioEspecial> solapamientos = horarioEspecialRepository.findSolapamientos(
                tenantId, dto.getFechaInicio(), dto.getFechaFin(), ""
        );

        if (!solapamientos.isEmpty()) {
            logger.warn("Creando cierre con solapamientos existentes para tenant: {}", tenantId);
        }

        // Crear entidad
        HorarioEspecial horario = mapearDTOToEntity(dto, tenantId);
        HorarioEspecial guardado = horarioEspecialRepository.save(horario);

        // Notificar clientes si es necesario (cuando CitaService este listo)
        if (Boolean.TRUE.equals(dto.getNotificarClientesExistentes())) {
            // notificarClientesAfectados(tenantId, dto.getFechaInicio(), dto.getFechaFin(), dto.getMotivo());
        }

        logger.info("Cierre planificado creado exitosamente: {}", guardado.getId());
        return guardado;
    }

    // ========================================
    // CONSULTAS Y ESTADISTICAS
    // ========================================

    /**
     * Obtener calendario de cierres para el frontend
     * MULTITENANT: Solo cierres del tenant actual
     */
    public List<CalendarioCierreDTO> obtenerCalendarioCierres(String tenantId, String mesAno) {
        LocalDate inicio = LocalDate.parse(mesAno + "-01");
        LocalDate fin = inicio.plusMonths(1).minusDays(1);

        List<HorarioEspecial> cierres = horarioEspecialRepository.findCierresEnRango(tenantId, inicio, fin);

        return cierres.stream()
                .map(this::mapearACalendarioDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtener estadisticas de cierres
     * MULTITENANT: Solo estadisticas del tenant actual
     */
    public EstadisticasCierreDTO obtenerEstadisticas(String tenantId) {
        EstadisticasCierreDTO stats = new EstadisticasCierreDTO();

        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        LocalDate finMes = inicioMes.plusMonths(1).minusDays(1);

        // Contar por tipos
        List<Object[]> conteos = horarioEspecialRepository.countCierresPorTipo(
                tenantId, inicioMes.minusMonths(12), finMes
        );

        for (Object[] conteo : conteos) {
            TipoCierre tipo = (TipoCierre) conteo[0];
            Long cantidad = (Long) conteo[1];

            switch (tipo) {
                case CERRADO_COMPLETO:
                    stats.setCierresCompletos(cantidad);
                    break;
                case HORARIO_REDUCIDO:
                    stats.setHorariosReducidos(cantidad);
                    break;
                case EMPLEADO_AUSENTE:
                    stats.setEmpleadosAusentes(cantidad);
                    break;
                case SERVICIO_NO_DISPONIBLE:
                    stats.setServiciosNoDisponibles(cantidad);
                    break;
                case SOLO_EMERGENCIAS:
                    stats.setSoloEmergencias(cantidad);
                    break;
            }
        }

        stats.setTotalCierres(stats.getCierresCompletos() + stats.getHorariosReducidos() +
                stats.getEmpleadosAusentes() + stats.getServiciosNoDisponibles() +
                stats.getSoloEmergencias());

        return stats;
    }

    /**
     * Obtener cierres proximos (para dashboard)
     * MULTITENANT: Solo del tenant actual
     */
    public List<HorarioEspecial> obtenerCierresProximos(String tenantId, int diasAdelante) {
        LocalDate fechaLimite = LocalDate.now().plusDays(diasAdelante);
        return horarioEspecialRepository.findCierresProximos(tenantId, fechaLimite);
    }

    /**
     * Eliminar/desactivar cierre
     * MULTITENANT: Solo puede eliminar cierres de su tenant
     */
    public void eliminarCierre(String tenantId, String cierreId) {
        Optional<HorarioEspecial> horarioOpt = horarioEspecialRepository.findByTenantIdAndIdAndActivo(
                tenantId, cierreId, true
        );

        if (horarioOpt.isPresent()) {
            HorarioEspecial horario = horarioOpt.get();
            horario.setActivo(false);
            horarioEspecialRepository.save(horario);

            logger.info("Cierre eliminado exitosamente: {}", cierreId);
        } else {
            logger.warn("Intento de eliminar cierre inexistente: {}", cierreId);
            throw new IllegalArgumentException("Cierre no encontrado");
        }
    }

    // ========================================
    // METODOS PRIVADOS DE UTILIDAD
    // ========================================

    private List<LocalDate> buscarFechasAlternativas(String tenantId, LocalDate fechaOriginal, int diasBuscar) {
        List<LocalDate> alternativas = new ArrayList<>();
        LocalDate fechaBusqueda = fechaOriginal.plusDays(1);

        for (int i = 0; i < diasBuscar && alternativas.size() < 3; i++) {
            Boolean disponible = horarioEspecialRepository.isFechaDisponible(tenantId, fechaBusqueda);
            if (Boolean.TRUE.equals(disponible)) {
                alternativas.add(fechaBusqueda);
            }
            fechaBusqueda = fechaBusqueda.plusDays(1);
        }

        return alternativas;
    }

    private void validarDatosCierre(HorarioEspecialDTO dto) {
        if (dto.getFechaInicio().isAfter(dto.getFechaFin())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin");
        }

        if (dto.getTipoCierre() == TipoCierre.HORARIO_REDUCIDO) {
            if (dto.getHorarioInicio() == null || dto.getHorarioFin() == null) {
                throw new IllegalArgumentException("Horario reducido requiere horas de inicio y fin");
            }
            if (dto.getHorarioInicio().isAfter(dto.getHorarioFin())) {
                throw new IllegalArgumentException("Hora de inicio no puede ser posterior a la de fin");
            }
        }
    }

    private HorarioEspecial mapearDTOToEntity(HorarioEspecialDTO dto, String tenantId) {
        HorarioEspecial horario = new HorarioEspecial();
        horario.setTenantId(tenantId);
        horario.setFechaInicio(dto.getFechaInicio());
        horario.setFechaFin(dto.getFechaFin());
        horario.setTipoCierre(dto.getTipoCierre());
        horario.setMotivo(dto.getMotivo());
        horario.setHorarioInicio(dto.getHorarioInicio());
        horario.setHorarioFin(dto.getHorarioFin());
        horario.setMensajePersonalizado(dto.getMensajePersonalizado());
        horario.setNotificarClientesExistentes(dto.getNotificarClientesExistentes());
        horario.setCreadoPor(dto.getCreadoPor() != null ? dto.getCreadoPor() : "usuario");

        // Convertir listas a JSON
        if (dto.getEmpleadosAfectados() != null && !dto.getEmpleadosAfectados().isEmpty()) {
            try {
                horario.setEmpleadosAfectados(objectMapper.writeValueAsString(dto.getEmpleadosAfectados()));
            } catch (Exception e) {
                logger.error("Error serializando empleados afectados", e);
            }
        }

        if (dto.getServiciosAfectados() != null && !dto.getServiciosAfectados().isEmpty()) {
            try {
                horario.setServiciosAfectados(objectMapper.writeValueAsString(dto.getServiciosAfectados()));
            } catch (Exception e) {
                logger.error("Error serializando servicios afectados", e);
            }
        }

        return horario;
    }

    private CalendarioCierreDTO mapearACalendarioDTO(HorarioEspecial horario) {
        CalendarioCierreDTO dto = new CalendarioCierreDTO(
                horario.getId(),
                horario.getFechaInicio(),
                horario.getTipoCierre(),
                horario.getMotivo(),
                horario.getFechaFin()
        );

        // ZERO HARDCODING: No establecer descripciones hardcodeadas
        // La descripcion se genera dinamicamente en el frontend o via OpenAI

        return dto;
    }

    private boolean esEmpleadoAfectado(String empleadosAfectadosJson, String empleadoId) {
        if (empleadosAfectadosJson == null || empleadoId == null) {
            return false;
        }

        try {
            List<String> empleados = objectMapper.readValue(empleadosAfectadosJson, new TypeReference<List<String>>() {});
            return empleados.contains(empleadoId);
        } catch (Exception e) {
            logger.error("Error deserializando empleados afectados", e);
            return empleadosAfectadosJson.contains(empleadoId);
        }
    }

    private boolean esServicioAfectado(String serviciosAfectadosJson, String servicioId) {
        if (serviciosAfectadosJson == null || servicioId == null) {
            return false;
        }

        try {
            List<String> servicios = objectMapper.readValue(serviciosAfectadosJson, new TypeReference<List<String>>() {});
            return servicios.contains(servicioId);
        } catch (Exception e) {
            logger.error("Error deserializando servicios afectados", e);
            return serviciosAfectadosJson.contains(servicioId);
        }
    }
}