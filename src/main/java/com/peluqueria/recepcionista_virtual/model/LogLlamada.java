package com.peluqueria.recepcionista_virtual.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "logs_llamadas")
public class LogLlamada {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(255)")
    private String id; // VARCHAR(255) en BD - SIN @GeneratedValue

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "call_sid", nullable = false)
    private String callSid;

    @Column(name = "twilio_call_sid")
    private String twilioCallSid;

    @Column(name = "numero_origen", nullable = false)
    private String numeroOrigen;

    @Column(name = "numero_destino", nullable = false)
    private String numeroDestino;

    @Column(name = "cliente_id")
    private String clienteId; // VARCHAR(255) en BD, no Long

    @Column(name = "empleado_id")
    private String empleadoId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    @Column(name = "estado", nullable = false)
    private String estado; // String en BD, no enum

    @Column(name = "estado_detalle", columnDefinition = "TEXT")
    private String estadoDetalle;

    @Column(name = "direccion")
    private String direccion; // String en BD, no enum

    @Column(name = "grabacion_url", columnDefinition = "TEXT")
    private String grabacionUrl;

    @Column(name = "transcripcion", columnDefinition = "TEXT")
    private String transcripcion;

    @Column(name = "costo", precision = 10, scale = 4)
    private BigDecimal costo;

    @Column(name = "costo_estimado", precision = 10, scale = 4)
    private BigDecimal costoEstimado;

    @Column(name = "moneda")
    private String moneda;

    @Column(name = "cita_creada_id")
    private String citaCreadaId; // VARCHAR(255) en BD, no Long

    @Column(name = "conversacion_ia_id")
    private String conversacionIaId;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "metadata_json", columnDefinition = "JSONB")
    private String metadataJson;

    // Constructor vacío
    public LogLlamada() {
        this.fechaInicio = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.estado = "INICIADA";
        this.direccion = "entrante";
        this.moneda = "EUR";
        // Generar ID único
        this.id = java.util.UUID.randomUUID().toString();
    }

    // Enums para compatibilidad (pero se guardan como String)
    public enum EstadoLlamada {
        INICIADA, EN_PROGRESO, COMPLETADA, FALLIDA, ABANDONADA, OCUPADO, NO_CONTESTA
    }

    public enum DireccionLlamada {
        ENTRANTE, SALIENTE
    }

    // Métodos helper para enums
    public EstadoLlamada getEstadoEnum() {
        try {
            return EstadoLlamada.valueOf(estado.toUpperCase());
        } catch (Exception e) {
            return EstadoLlamada.INICIADA;
        }
    }

    public void setEstadoEnum(EstadoLlamada estadoEnum) {
        this.estado = estadoEnum.name().toLowerCase();
    }

    public DireccionLlamada getDireccionEnum() {
        try {
            return DireccionLlamada.valueOf(direccion.toUpperCase());
        } catch (Exception e) {
            return DireccionLlamada.ENTRANTE;
        }
    }

    public void setDireccionEnum(DireccionLlamada direccionEnum) {
        this.direccion = direccionEnum.name().toLowerCase();
    }

    // Método útil para calcular duración
    public void finalizarLlamada() {
        this.fechaFin = LocalDateTime.now();
        this.endedAt = LocalDateTime.now();
        if (this.fechaInicio != null) {
            this.duracionSegundos = (int) java.time.Duration.between(
                    this.fechaInicio, this.fechaFin
            ).getSeconds();
        }
        if ("INICIADA".equals(this.estado) || "EN_PROGRESO".equals(this.estado)) {
            this.estado = "COMPLETADA";
        }
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCallSid() {
        return callSid;
    }

    public void setCallSid(String callSid) {
        this.callSid = callSid;
    }

    public String getTwilioCallSid() {
        return twilioCallSid;
    }

    public void setTwilioCallSid(String twilioCallSid) {
        this.twilioCallSid = twilioCallSid;
    }

    public String getNumeroOrigen() {
        return numeroOrigen;
    }

    public void setNumeroOrigen(String numeroOrigen) {
        this.numeroOrigen = numeroOrigen;
    }

    public String getNumeroDestino() {
        return numeroDestino;
    }

    public void setNumeroDestino(String numeroDestino) {
        this.numeroDestino = numeroDestino;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getEmpleadoId() {
        return empleadoId;
    }

    public void setEmpleadoId(String empleadoId) {
        this.empleadoId = empleadoId;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Integer getDuracionSegundos() {
        return duracionSegundos;
    }

    public void setDuracionSegundos(Integer duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstadoDetalle() {
        return estadoDetalle;
    }

    public void setEstadoDetalle(String estadoDetalle) {
        this.estadoDetalle = estadoDetalle;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getGrabacionUrl() {
        return grabacionUrl;
    }

    public void setGrabacionUrl(String grabacionUrl) {
        this.grabacionUrl = grabacionUrl;
    }

    public String getTranscripcion() {
        return transcripcion;
    }

    public void setTranscripcion(String transcripcion) {
        this.transcripcion = transcripcion;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public BigDecimal getCostoEstimado() {
        return costoEstimado;
    }

    public void setCostoEstimado(BigDecimal costoEstimado) {
        this.costoEstimado = costoEstimado;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getCitaCreadaId() {
        return citaCreadaId;
    }

    public void setCitaCreadaId(String citaCreadaId) {
        this.citaCreadaId = citaCreadaId;
    }

    public String getConversacionIaId() {
        return conversacionIaId;
    }

    public void setConversacionIaId(String conversacionIaId) {
        this.conversacionIaId = conversacionIaId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}