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
     * Obtener llamadas por estado
     */
    public List<LogLlamada> getLlamadasByEstado(
            String tenantId,
            LogLlamada.EstadoLlamada estado) {
        return logLlamadaRepository.findByTenantIdAndEstadoOrderByFechaInicioDesc(tenantId, estado);
    }

    /**
     * Obtener llamadas por dirección (entrantes/salientes)
     */
    public List<LogLlamada> getLlamadasByDireccion(
            String tenantId,
            LogLlamada.DireccionLlamada direccion) {
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
     * Crear nueva llamada
     */
    public LogLlamada crearLlamada(
            String tenantId,
            String callSid,
            String numeroOrigen,
            String numeroDestino,
            LogLlamada.DireccionLlamada direccion) {

        LogLlamada logLlamada = new LogLlamada();
        logLlamada.setTenantId(tenantId);
        logLlamada.setCallSid(callSid);
        logLlamada.setNumeroOrigen(numeroOrigen);
        logLlamada.setNumeroDestino(numeroDestino);
        logLlamada.setDireccion(direccion);
        logLlamada.setFechaInicio(LocalDateTime.now());
        logLlamada.setEstado(LogLlamada.EstadoLlamada.INICIADA);

        return logLlamadaRepository.save(logLlamada);
    }

    /**
     * Guardar o actualizar llamada
     */
    public LogLlamada guardarLlamada(LogLlamada logLlamada) {
        return logLlamadaRepository.save(logLlamada);
    }

    /**
     * Actualizar estado de llamada
     */
    public LogLlamada actualizarEstado(String callSid, String tenantId, LogLlamada.EstadoLlamada nuevoEstado) {
        LogLlamada llamada = logLlamadaRepository.findByCallSidAndTenantId(callSid, tenantId);
        if (llamada != null) {
            llamada.setEstado(nuevoEstado);
            if (nuevoEstado == LogLlamada.EstadoLlamada.COMPLETADA ||
                    nuevoEstado == LogLlamada.EstadoLlamada.FALLIDA ||
                    nuevoEstado == LogLlamada.EstadoLlamada.ABANDONADA) {
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
     * Vincular llamada con cita creada
     */
    public LogLlamada vincularConCita(String callSid, String tenantId, Long citaId) {
        LogLlamada llamada = logLlamadaRepository.findByCallSidAndTenantId(callSid, tenantId);
        if (llamada != null) {
            llamada.setCitaCreadaId(citaId);
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
        return logLlamadaRepository.countByTenantIdAndDireccion(tenantId, LogLlamada.DireccionLlamada.ENTRANTE);
    }

    public Long getLlamadasSalientes(String tenantId) {
        return logLlamadaRepository.countByTenantIdAndDireccion(tenantId, LogLlamada.DireccionLlamada.SALIENTE);
    }

    public Long getLlamadasCompletadas(String tenantId) {
        return logLlamadaRepository.countByTenantIdAndEstado(tenantId, LogLlamada.EstadoLlamada.COMPLETADA);
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
     * Buscar llamada por ID (verificando tenant)
     */
    public LogLlamada getLlamadaById(Long id, String tenantId) {
        return logLlamadaRepository.findByIdAndTenantId(id, tenantId);
    }

    /**
     * Eliminar llamada (verificando tenant)
     */
    public void eliminarLlamada(Long id, String tenantId) {
        LogLlamada llamada = logLlamadaRepository.findByIdAndTenantId(id, tenantId);
        if (llamada != null) {
            logLlamadaRepository.delete(llamada);
        }
    }
}