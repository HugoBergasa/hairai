package com.peluqueria.recepcionista_virtual.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "conversaciones_ia")
public class ConversacionIA {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(255)")
    private String id; // VARCHAR(255) en BD - SIN @GeneratedValue

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "cliente_id")
    private String clienteId; // VARCHAR(255) en BD

    @Column(name = "call_sid")
    private String callSid;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "numero_telefono")
    private String numeroTelefono;

    @Column(name = "mensaje_cliente", columnDefinition = "TEXT", nullable = false)
    private String mensajeCliente;

    @Column(name = "mensaje_usuario", columnDefinition = "TEXT")
    private String mensajeUsuario;

    @Column(name = "respuesta_ia", columnDefinition = "TEXT", nullable = false)
    private String respuestaIA;

    @Column(name = "intencion_detectada")
    private String intencionDetectada;

    @Column(name = "accion_ejecutada")
    private String accionEjecutada;

    @Column(name = "canal", nullable = false)
    @Enumerated(EnumType.STRING)
    private CanalComunicacion canal;

    @Column(name = "contexto", columnDefinition = "JSONB")
    private String contexto;

    @Column(name = "contexto_json", columnDefinition = "JSONB")
    private String contextoJson;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "duracion_ms")
    private Integer duracionMs;

    @Column(name = "tiempo_respuesta_ms")
    private Integer tiempoRespuestaMs;

    @Column(name = "tokens_usados")
    private Integer tokensUsados;

    @Column(name = "modelo_ia")
    private String modeloIa;

    @Column(name = "satisfaccion_score", precision = 3, scale = 2)
    private BigDecimal satisfaccionScore;

    @Column(name = "exitoso")
    private Boolean exitoso;

    @Column(name = "estado")
    private String estado;

    @Column(name = "error_mensaje")
    private String errorMensaje;

    // Constructor vacío
    public ConversacionIA() {
        this.timestamp = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.exitoso = true;
        this.estado = "completado";
        this.modeloIa = "gpt-4";
        // Generar ID único
        this.id = java.util.UUID.randomUUID().toString();
    }

    // Enum para Canal
    public enum CanalComunicacion {
        TELEFONO, WHATSAPP, SMS, WEB, API
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

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getCallSid() {
        return callSid;
    }

    public void setCallSid(String callSid) {
        this.callSid = callSid;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNumeroTelefono() {
        return numeroTelefono;
    }

    public void setNumeroTelefono(String numeroTelefono) {
        this.numeroTelefono = numeroTelefono;
    }

    public String getMensajeCliente() {
        return mensajeCliente;
    }

    public void setMensajeCliente(String mensajeCliente) {
        this.mensajeCliente = mensajeCliente;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }

    public void setMensajeUsuario(String mensajeUsuario) {
        this.mensajeUsuario = mensajeUsuario;
    }

    public String getRespuestaIA() {
        return respuestaIA;
    }

    public void setRespuestaIA(String respuestaIA) {
        this.respuestaIA = respuestaIA;
    }

    public String getIntencionDetectada() {
        return intencionDetectada;
    }

    public void setIntencionDetectada(String intencionDetectada) {
        this.intencionDetectada = intencionDetectada;
    }

    public String getAccionEjecutada() {
        return accionEjecutada;
    }

    public void setAccionEjecutada(String accionEjecutada) {
        this.accionEjecutada = accionEjecutada;
    }

    public CanalComunicacion getCanal() {
        return canal;
    }

    public void setCanal(CanalComunicacion canal) {
        this.canal = canal;
    }

    public String getContexto() {
        return contexto;
    }

    public void setContexto(String contexto) {
        this.contexto = contexto;
    }

    public String getContextoJson() {
        return contextoJson;
    }

    public void setContextoJson(String contextoJson) {
        this.contextoJson = contextoJson;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDuracionMs() {
        return duracionMs;
    }

    public void setDuracionMs(Integer duracionMs) {
        this.duracionMs = duracionMs;
    }

    public Integer getTiempoRespuestaMs() {
        return tiempoRespuestaMs;
    }

    public void setTiempoRespuestaMs(Integer tiempoRespuestaMs) {
        this.tiempoRespuestaMs = tiempoRespuestaMs;
    }

    public Integer getTokensUsados() {
        return tokensUsados;
    }

    public void setTokensUsados(Integer tokensUsados) {
        this.tokensUsados = tokensUsados;
    }

    public String getModeloIa() {
        return modeloIa;
    }

    public void setModeloIa(String modeloIa) {
        this.modeloIa = modeloIa;
    }

    public BigDecimal getSatisfaccionScore() {
        return satisfaccionScore;
    }

    public void setSatisfaccionScore(BigDecimal satisfaccionScore) {
        this.satisfaccionScore = satisfaccionScore;
    }

    public Boolean getExitoso() {
        return exitoso;
    }

    public void setExitoso(Boolean exitoso) {
        this.exitoso = exitoso;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getErrorMensaje() {
        return errorMensaje;
    }

    public void setErrorMensaje(String errorMensaje) {
        this.errorMensaje = errorMensaje;
    }
}