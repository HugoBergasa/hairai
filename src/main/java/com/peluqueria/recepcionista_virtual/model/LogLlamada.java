package com.peluqueria.recepcionista_virtual.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "logs_llamadas")
public class LogLlamada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "call_sid", unique = true, nullable = false)
    private String callSid;

    @Column(name = "numero_origen")
    private String numeroOrigen;

    @Column(name = "numero_destino")
    private String numeroDestino;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    @Column(name = "estado")
    @Enumerated(EnumType.STRING)
    private EstadoLlamada estado;

    @Column(name = "direccion")
    @Enumerated(EnumType.STRING)
    private DireccionLlamada direccion;

    @Column(name = "grabacion_url")
    private String grabacionUrl;

    @Column(name = "transcripcion", columnDefinition = "TEXT")
    private String transcripcion;

    @Column(name = "costo", precision = 10, scale = 4)
    private BigDecimal costo;

    @Column(name = "cita_creada_id")
    private Long citaCreadaId;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    // Constructor vacío
    public LogLlamada() {
        this.fechaInicio = LocalDateTime.now();
        this.estado = EstadoLlamada.INICIADA;
        this.direccion = DireccionLlamada.ENTRANTE;
    }

    // Enums
    public enum EstadoLlamada {
        INICIADA, EN_PROGRESO, COMPLETADA, FALLIDA, ABANDONADA, OCUPADO, NO_CONTESTA
    }

    public enum DireccionLlamada {
        ENTRANTE, SALIENTE
    }

    // Método útil para calcular duración
    public void finalizarLlamada() {
        this.fechaFin = LocalDateTime.now();
        if (this.fechaInicio != null) {
            this.duracionSegundos = (int) java.time.Duration.between(
                    this.fechaInicio, this.fechaFin
            ).getSeconds();
        }
        if (this.estado == EstadoLlamada.INICIADA || this.estado == EstadoLlamada.EN_PROGRESO) {
            this.estado = EstadoLlamada.COMPLETADA;
        }
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public Integer getDuracionSegundos() {
        return duracionSegundos;
    }

    public void setDuracionSegundos(Integer duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }

    public EstadoLlamada getEstado() {
        return estado;
    }

    public void setEstado(EstadoLlamada estado) {
        this.estado = estado;
    }

    public DireccionLlamada getDireccion() {
        return direccion;
    }

    public void setDireccion(DireccionLlamada direccion) {
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

    public Long getCitaCreadaId() {
        return citaCreadaId;
    }

    public void setCitaCreadaId(Long citaCreadaId) {
        this.citaCreadaId = citaCreadaId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}