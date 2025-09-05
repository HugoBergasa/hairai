package com.peluqueria.recepcionista_virtual.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversaciones_ia")
public class ConversacionIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "call_sid")
    private String callSid;

    @Column(name = "mensaje_usuario", columnDefinition = "TEXT")
    private String mensajeUsuario;

    @Column(name = "respuesta_ia", columnDefinition = "TEXT")
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

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "duracion_ms")
    private Integer duracionMs;

    @Column(name = "tokens_usados")
    private Integer tokensUsados;

    @Column(name = "exitoso")
    private Boolean exitoso;

    @Column(name = "error_mensaje")
    private String errorMensaje;

    // Constructor vacío
    public ConversacionIA() {
        this.timestamp = LocalDateTime.now();
        this.exitoso = true;
    }

    // Enum para Canal
    public enum CanalComunicacion {
        TELEFONO, WHATSAPP, SMS, WEB, API
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getDuracionMs() {
        return duracionMs;
    }

    public void setDuracionMs(Integer duracionMs) {
        this.duracionMs = duracionMs;
    }

    public Integer getTokensUsados() {
        return tokensUsados;
    }

    public void setTokensUsados(Integer tokensUsados) {
        this.tokensUsados = tokensUsados;
    }

    public Boolean getExitoso() {
        return exitoso;
    }

    public void setExitoso(Boolean exitoso) {
        this.exitoso = exitoso;
    }

    public String getErrorMensaje() {
        return errorMensaje;
    }

    public void setErrorMensaje(String errorMensaje) {
        this.errorMensaje = errorMensaje;
    }
}