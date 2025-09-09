package com.peluqueria.recepcionista_virtual.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "configuracion_tenant",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "clave"}))
public class ConfiguracionTenant {

    @Id
    @Column(name = "id")
    private String id;  // ✅ CAMBIADO: De Long a String

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "clave", nullable = false)
    private String clave;

    @Column(name = "valor", columnDefinition = "TEXT")
    private String valor;

    @Column(name = "tipo_dato")
    @Enumerated(EnumType.STRING)
    private TipoDato tipoDato;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "editable")
    private Boolean editable;

    @Column(name = "categoria")
    private String categoria;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructor vacío
    public ConfiguracionTenant() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.editable = true;
        this.tipoDato = TipoDato.STRING;
    }

    // Constructor útil
    public ConfiguracionTenant(String tenantId, String clave, String valor, String categoria) {
        this();
        this.tenantId = tenantId;
        this.clave = clave;
        this.valor = valor;
        this.categoria = categoria;
    }

    // ✅ NUEVO: Generar ID automáticamente antes de persistir
    @PrePersist
    public void generarId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    // Enum para tipos de dato
    public enum TipoDato {
        STRING, INTEGER, BOOLEAN, JSON, DECIMAL, TIME, DATE
    }

    // Claves de configuración predefinidas
    public static class Claves {
        public static final String NOMBRE_NEGOCIO = "nombre_negocio";
        public static final String HORARIO_APERTURA = "horario_apertura";
        public static final String HORARIO_CIERRE = "horario_cierre";
        public static final String DIAS_TRABAJO = "dias_trabajo";
        public static final String MENSAJE_BIENVENIDA = "mensaje_bienvenida";
        public static final String TIEMPO_SLOT_MINUTOS = "tiempo_slot_minutos";
        public static final String ANTICIPACION_MAX_DIAS = "anticipacion_max_dias";
        public static final String PERMITIR_CANCELACIONES = "permitir_cancelaciones";
        public static final String HORAS_MIN_CANCELACION = "horas_min_cancelacion";
        public static final String NUMERO_TWILIO = "numero_twilio";
        public static final String WHATSAPP_HABILITADO = "whatsapp_habilitado";
        public static final String SMS_RECORDATORIO = "sms_recordatorio";
        public static final String MODELO_GPT = "modelo_gpt";
        public static final String TEMPERATURA_IA = "temperatura_ia";
        public static final String PROMPT_SISTEMA = "prompt_sistema";
    }

    // Categorías predefinidas
    public static class Categorias {
        public static final String GENERAL = "GENERAL";
        public static final String HORARIOS = "HORARIOS";
        public static final String MENSAJES = "MENSAJES";
        public static final String IA = "IA";
        public static final String NOTIFICACIONES = "NOTIFICACIONES";
        public static final String INTEGRACIONES = "INTEGRACIONES";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters y Setters
    public String getId() {  // ✅ CAMBIADO: De Long a String
        return id;
    }

    public void setId(String id) {  // ✅ CAMBIADO: De Long a String
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public TipoDato getTipoDato() {
        return tipoDato;
    }

    public void setTipoDato(TipoDato tipoDato) {
        this.tipoDato = tipoDato;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
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

    // Métodos útiles para conversión
    public Integer getValorAsInteger() {
        try {
            return Integer.parseInt(valor);
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean getValorAsBoolean() {
        return "true".equalsIgnoreCase(valor) || "1".equals(valor);
    }
}