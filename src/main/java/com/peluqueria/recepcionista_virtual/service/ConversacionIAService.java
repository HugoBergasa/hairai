package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.ConversacionIA;
import com.peluqueria.recepcionista_virtual.repository.ConversacionIARepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ConversacionIAService {

    @Autowired
    private ConversacionIARepository conversacionIARepository;

    /**
     * Obtener todas las conversaciones de un tenant específico
     */
    public List<ConversacionIA> getConversacionesByTenantId(String tenantId) {
        return conversacionIARepository.findByTenantIdOrderByTimestampDesc(tenantId);
    }

    /**
     * Obtener conversaciones de un tenant con paginación
     */
    public Page<ConversacionIA> getConversacionesByTenantId(String tenantId, Pageable pageable) {
        return conversacionIARepository.findByTenantIdOrderByTimestampDesc(tenantId, pageable);
    }

    /**
     * Obtener conversaciones por CallSid
     */
    public List<ConversacionIA> getConversacionesByCallSid(String callSid, String tenantId) {
        return conversacionIARepository.findByCallSidAndTenantIdOrderByTimestampAsc(callSid, tenantId);
    }

    /**
     * Obtener conversaciones por canal de comunicación
     */
    public List<ConversacionIA> getConversacionesByCanal(
            String tenantId,
            ConversacionIA.CanalComunicacion canal) {
        return conversacionIARepository.findByTenantIdAndCanalOrderByTimestampDesc(tenantId, canal);
    }

    /**
     * Obtener conversaciones en un rango de fechas
     */
    public List<ConversacionIA> getConversacionesByFecha(
            String tenantId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {
        return conversacionIARepository.findByTenantIdAndTimestampBetweenOrderByTimestampDesc(
                tenantId, fechaInicio, fechaFin);
    }

    /**
     * Guardar nueva conversación
     */
    public ConversacionIA guardarConversacion(ConversacionIA conversacion) {
        if (conversacion.getTimestamp() == null) {
            conversacion.setTimestamp(LocalDateTime.now());
        }
        if (conversacion.getId() == null) {
            conversacion.setId(java.util.UUID.randomUUID().toString());
        }
        return conversacionIARepository.save(conversacion);
    }

    /**
     * Crear conversación básica
     */
    public ConversacionIA crearConversacion(
            String tenantId,
            String callSid,
            String mensajeUsuario,
            String respuestaIA,
            ConversacionIA.CanalComunicacion canal) {

        ConversacionIA conversacion = new ConversacionIA();
        conversacion.setTenantId(tenantId);
        conversacion.setCallSid(callSid);
        conversacion.setMensajeUsuario(mensajeUsuario);
        conversacion.setMensajeCliente(mensajeUsuario); // Para compatibilidad BD
        conversacion.setRespuestaIA(respuestaIA);
        conversacion.setCanal(canal);
        conversacion.setTipo(canal.name().toLowerCase());
        conversacion.setTimestamp(LocalDateTime.now());
        conversacion.setExitoso(true);

        return conversacionIARepository.save(conversacion);
    }

    /**
     * Obtener estadísticas de conversaciones por tenant
     */
    public Long getTotalConversaciones(String tenantId) {
        return conversacionIARepository.countByTenantId(tenantId);
    }

    public Long getConversacionesExitosas(String tenantId) {
        return conversacionIARepository.countByTenantIdAndExitoso(tenantId, true);
    }

    public Long getConversacionesPorCanal(String tenantId, ConversacionIA.CanalComunicacion canal) {
        return conversacionIARepository.countByTenantIdAndCanal(tenantId, canal);
    }

    /**
     * Buscar conversación por ID (verificando tenant) - CORREGIDO: String ID
     */
    public ConversacionIA getConversacionById(String id, String tenantId) {
        return conversacionIARepository.findByIdAndTenantId(id, tenantId);
    }

    /**
     * Eliminar conversación (verificando tenant) - CORREGIDO: String ID
     */
    public void eliminarConversacion(String id, String tenantId) {
        ConversacionIA conversacion = conversacionIARepository.findByIdAndTenantId(id, tenantId);
        if (conversacion != null) {
            conversacionIARepository.delete(conversacion);
        }
    }
}