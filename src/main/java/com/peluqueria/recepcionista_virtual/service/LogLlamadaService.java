package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.LogLlamada;
import com.peluqueria.recepcionista_virtual.repository.LogLlamadaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Service
@Transactional
public class LogLlamadaService {

    @Autowired
    private LogLlamadaRepository logLlamadaRepository;

    /**
     * Obtener todas las llamadas de un tenant específico
     */
    public List<LogLlamada> getLlamadasByTenantId(String tenantId) {
        return logLlamadaRepository.findByTenantIdOrderByFechaInicioDesc(tenantId);
    }

    /**
     * Obtener llamadas de un tenant con paginación
     */
    public Page<LogLlamada> getLlamadasByTenantId(String tenantId, Pageable pageable) {
        return logLlamadaRepository.findByTenantIdOrderByFechaInicioDesc(tenantId, pageable);
    }

    /**
     * Obtener llamada por CallSid
     */
    public LogLlamada getLlamadaByCallSid(String callSid, String tenantId) {
        return logLlamadaRepository.findByCallSidAndTenantId(callSid, tenantId);
    }

    /**
     * Obtener llamadas por estado - CORREGIDO: usar String
     */
    public List<LogLlamada> getLlamadasByEstado(String tenantId, String estado) {
        return logLlamadaRepository.findByTenantIdAndEstadoOrderByFechaInicioDesc(tenantId, estado);
    }

    /**
     * Obtener llamadas por dirección (entrantes/salientes) - CORREGIDO: usar String
     */
    public List<LogLlamada> getLlamadasByDireccion(String tenantId, String direccion) {
        return logLlamadaRepository.findByTenantIdAndDireccionOrderByFechaInicioDesc(tenantId, direccion);
    }

    /**
     * Obtener llamadas en un rango de fechas
     */
    public List<LogLlamada> getLlamadasByFecha(
            String tenantId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {
        return logLlamadaRepository.findByTenantIdAndFechaInicioBetweenOrderByFechaInicioDesc(
                tenantId, fechaInicio, fechaFin);
    }

    /**
     * Obtener llamadas por número de origen
     */
    public List<LogLlamada> getLlamadasByNumeroOrigen(String tenantId, String numeroOrigen) {
        return logLlamadaRepository.findByTenantIdAndNumeroOrigenOrderByFechaInicioDesc(tenantId, numeroOrigen);
    }

    /**
     * Crear nueva llamada - CORREGIDO: usar String para dirección
     */
    public LogLlamada crearLlamada(
            String tenantId,
            String callSid,
            String numeroOrigen,
            String numeroDestino,
            String direccion) {

        LogLlamada logLlamada = new LogLlamada();
        logLlamada.setTenantId(tenantId);
        logLlamada.setCallSid(callSid);
        logLlamada.setNumeroOrigen(numeroOrigen);
        logLlamada.setNumeroDestino(numeroDestino);
        logLlamada.setDireccion(direccion);  // String directo
        logLlamada.setFechaInicio(LocalDateTime.now());
        logLlamada.setEstado("INICIADA");    // String directo

        return logLlamadaRepository.save(logLlamada);
    }

    /**
     * Guardar o actualizar llamada
     */
    public LogLlamada guardarLlamada(LogLlamada logLlamada) {
        if (logLlamada.getId() == null) {
            logLlamada.setId(java.util.UUID.randomUUID().toString());
        }
        return logLlamadaRepository.save(logLlamada);
    }

    /**
     * Actualizar estado de llamada - CORREGIDO: usar String para estado
     */
    public LogLlamada actualizarEstado(String callSid, String tenantId, String nuevoEstado) {
        LogLlamada llamada = logLlamadaRepository.findByCallSidAndTenantId(callSid, tenantId);
        if (llamada != null) {
            llamada.setEstado(nuevoEstado);  // String directo
            if ("COMPLETADA".equals(nuevoEstado) ||
                    "FALLIDA".equals(nuevoEstado) ||
                    "ABANDONADA".equals(nuevoEstado)) {
                llamada.finalizarLlamada();
            }
            return logLlamadaRepository.save(llamada);
        }
        return null;
    }

    /**
     * Finalizar llamada
     */
    public LogLlamada finalizarLlamada(String callSid, String tenantId) {
        LogLlamada llamada = logLlamadaRepository.findByCallSidAndTenantId(callSid, tenantId);
        if (llamada != null) {
            llamada.finalizarLlamada();
            return logLlamadaRepository.save(llamada);
        }
        return null;
    }

    /**
     * Agregar transcripción a la llamada
     */
    public LogLlamada agregarTranscripcion(String callSid, String tenantId, String transcripcion) {
        LogLlamada llamada = logLlamadaRepository.findByCallSidAndTenantId(callSid, tenantId);
        if (llamada != null) {
            llamada.setTranscripcion(transcripcion);
            return logLlamadaRepository.save(llamada);
        }
        return null;
    }

    /**
     * Vincular llamada con cita creada - CORREGIDO: usar String para citaId
     */
    public LogLlamada vincularConCita(String callSid, String tenantId, String citaId) {
        LogLlamada llamada = logLlamadaRepository.findByCallSidAndTenantId(callSid, tenantId);
        if (llamada != null) {
            llamada.setCitaCreadaId(citaId);  // String directo
            return logLlamadaRepository.save(llamada);
        }
        return null;
    }

    /**
     * Estadísticas de llamadas por tenant
     */
    public Long getTotalLlamadas(String tenantId) {
        return logLlamadaRepository.countByTenantId(tenantId);
    }

    public Long getLlamadasEntrantes(String tenantId) {
        return logLlamadaRepository.countByTenantIdAndDireccion(tenantId, "entrante");
    }

    public Long getLlamadasSalientes(String tenantId) {
        return logLlamadaRepository.countByTenantIdAndDireccion(tenantId, "saliente");
    }

    public Long getLlamadasCompletadas(String tenantId) {
        return logLlamadaRepository.countByTenantIdAndEstado(tenantId, "COMPLETADA");
    }

    public Long getLlamadasConCitaCreada(String tenantId) {
        return logLlamadaRepository.countByTenantIdAndCitaCreadaIdIsNotNull(tenantId);
    }

    /**
     * Calcular costo total de llamadas por tenant
     */
    public BigDecimal getCostoTotalLlamadas(String tenantId) {
        List<LogLlamada> llamadas = logLlamadaRepository.findByTenantId(tenantId);
        return llamadas.stream()
                .filter(llamada -> llamada.getCosto() != null)
                .map(LogLlamada::getCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Obtener duración total de llamadas por tenant (en segundos)
     */
    public Long getDuracionTotalLlamadas(String tenantId) {
        List<LogLlamada> llamadas = logLlamadaRepository.findByTenantId(tenantId);
        return llamadas.stream()
                .filter(llamada -> llamada.getDuracionSegundos() != null)
                .mapToLong(LogLlamada::getDuracionSegundos)
                .sum();
    }

    /**
     * Buscar llamada por ID (verificando tenant) - CORREGIDO: usar String como en el modelo
     */
    public LogLlamada getLlamadaById(String id, String tenantId) {
        return logLlamadaRepository.findByIdAndTenantId(id, tenantId);
    }

    /**
     * Eliminar llamada (verificando tenant) - CORREGIDO: usar String como en el modelo
     */
    public void eliminarLlamada(String id, String tenantId) {
        LogLlamada llamada = logLlamadaRepository.findByIdAndTenantId(id, tenantId);
        if (llamada != null) {
            logLlamadaRepository.delete(llamada);
        }
    }

    /**
     * Métodos adicionales usando los enums helper para mayor flexibilidad
     */

    /**
     * Crear llamada usando enum de dirección
     */
    public LogLlamada crearLlamadaConEnum(
            String tenantId,
            String callSid,
            String numeroOrigen,
            String numeroDestino,
            LogLlamada.DireccionLlamada direccionEnum) {

        return crearLlamada(tenantId, callSid, numeroOrigen, numeroDestino,
                direccionEnum.name().toLowerCase());
    }

    /**
     * Actualizar estado usando enum
     */
    public LogLlamada actualizarEstadoConEnum(String callSid, String tenantId,
                                              LogLlamada.EstadoLlamada estadoEnum) {
        return actualizarEstado(callSid, tenantId, estadoEnum.name());
    }

    /**
     * Obtener llamadas por estado usando enum
     */
    public List<LogLlamada> getLlamadasByEstadoEnum(String tenantId,
                                                    LogLlamada.EstadoLlamada estadoEnum) {
        return getLlamadasByEstado(tenantId, estadoEnum.name());
    }

    /**
     * Obtener llamadas por dirección usando enum
     */
    public List<LogLlamada> getLlamadasByDireccionEnum(String tenantId,
                                                       LogLlamada.DireccionLlamada direccionEnum) {
        return getLlamadasByDireccion(tenantId, direccionEnum.name().toLowerCase());
    }
}