package com.peluqueria.recepcionista_virtual.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo para la tabla empleados_servicios del esquema de base de datos
 * MULTITENANT: Incluye tenantId para aislamiento perfecto
 * ZERO HARDCODING: Configuraciones flexibles por registro
 */
@Entity
@Table(name = "empleados_servicios")
public class EmpleadoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "empleado_id", nullable = false)
    private String empleadoId;

    @Column(name = "servicio_id", nullable = false)
    private String servicioId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_experiencia")
    private NivelExperiencia nivelExperiencia = NivelExperiencia.INTERMEDIO;

    @Column(name = "tiempo_extra_minutos")
    private Integer tiempoExtraMinutos = 0;

    @Column(name = "precio_personalizado", precision = 10, scale = 2)
    private BigDecimal precioPersonalizado;

    @Column(name = "comision_porcentaje", precision = 5, scale = 2)
    private BigDecimal comisionPorcentaje;

    @Column(name = "disponible")
    private Boolean disponible = true;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "certificaciones")
    private String certificaciones; // Array JSON como string

    @Column(name = "prioridad")
    private Integer prioridad = 50;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ========================================
    // ENUM PARA NIVEL DE EXPERIENCIA
    // ========================================

    public enum NivelExperiencia {
        PRINCIPIANTE,
        INTERMEDIO,
        AVANZADO,
        EXPERTO
    }

    // ========================================
    // CONSTRUCTORES
    // ========================================

    public EmpleadoServicio() {}

    public EmpleadoServicio(String empleadoId, String servicioId, String tenantId) {
        this.empleadoId = empleadoId;
        this.servicioId = servicioId;
        this.tenantId = tenantId;
    }

    // ========================================
    // MÉTODOS DE NEGOCIO
    // ========================================

    /**
     * UTIL: Verificar si empleado está autorizado y disponible
     */
    public boolean estaDisponible() {
        return disponible != null && disponible;
    }

    /**
     * UTIL: Calcular precio final considerando personalización
     */
    public BigDecimal calcularPrecioFinal(BigDecimal precioBase) {
        return precioPersonalizado != null ? precioPersonalizado : precioBase;
    }

    /**
     * UTIL: Calcular tiempo total incluyendo extra
     */
    public int calcularTiempoTotal(int tiempoBase) {
        int extra = tiempoExtraMinutos != null ? tiempoExtraMinutos : 0;
        return tiempoBase + extra;
    }

    /**
     * UTIL: Obtener nivel de prioridad para asignación automática
     */
    public int obtenerPuntuacionPrioridad() {
        int puntos = prioridad != null ? prioridad : 50;

        // Bonus por nivel de experiencia
        switch (nivelExperiencia) {
            case EXPERTO: puntos += 20; break;
            case AVANZADO: puntos += 15; break;
            case INTERMEDIO: puntos += 10; break;
            case PRINCIPIANTE: puntos += 5; break;
        }

        return puntos;
    }

    // ========================================
    // TRIGGERS JPA
    // ========================================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========================================
    // GETTERS Y SETTERS
    // ========================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmpleadoId() {
        return empleadoId;
    }

    public void setEmpleadoId(String empleadoId) {
        this.empleadoId = empleadoId;
    }

    public String getServicioId() {
        return servicioId;
    }

    public void setServicioId(String servicioId) {
        this.servicioId = servicioId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public NivelExperiencia getNivelExperiencia() {
        return nivelExperiencia;
    }

    public void setNivelExperiencia(NivelExperiencia nivelExperiencia) {
        this.nivelExperiencia = nivelExperiencia;
    }

    public Integer getTiempoExtraMinutos() {
        return tiempoExtraMinutos;
    }

    public void setTiempoExtraMinutos(Integer tiempoExtraMinutos) {
        this.tiempoExtraMinutos = tiempoExtraMinutos;
    }

    public BigDecimal getPrecioPersonalizado() {
        return precioPersonalizado;
    }

    public void setPrecioPersonalizado(BigDecimal precioPersonalizado) {
        this.precioPersonalizado = precioPersonalizado;
    }

    public BigDecimal getComisionPorcentaje() {
        return comisionPorcentaje;
    }

    public void setComisionPorcentaje(BigDecimal comisionPorcentaje) {
        this.comisionPorcentaje = comisionPorcentaje;
    }

    public Boolean getDisponible() {
        return disponible;
    }

    public void setDisponible(Boolean disponible) {
        this.disponible = disponible;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public String getCertificaciones() {
        return certificaciones;
    }

    public void setCertificaciones(String certificaciones) {
        this.certificaciones = certificaciones;
    }

    public Integer getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Integer prioridad) {
        this.prioridad = prioridad;
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

    @Override
    public String toString() {
        return "EmpleadoServicio{" +
                "id='" + id + '\'' +
                ", empleadoId='" + empleadoId + '\'' +
                ", servicioId='" + servicioId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", nivelExperiencia=" + nivelExperiencia +
                ", disponible=" + disponible +
                '}';
    }
}