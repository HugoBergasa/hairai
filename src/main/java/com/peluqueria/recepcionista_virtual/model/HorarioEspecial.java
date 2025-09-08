package com.peluqueria.recepcionista_virtual.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para manejar horarios especiales y cierres del salón
 *
 * MULTITENANT: Cada registro pertenece a un tenant específico mediante tenant_id
 * ZERO HARDCODING: Todos los textos y configuraciones vienen de BD
 * OpenAI CEREBRO: Los mensajes se generan dinámicamente o se toman del campo mensaje_personalizado
 */
@Entity
@Table(name = "horarios_especiales")
public class HorarioEspecial {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(255)")
    private String id = UUID.randomUUID().toString();

    // ========================================
    // MULTITENANT: Campo obligatorio
    // ========================================

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // ========================================
    // DATOS DEL CIERRE
    // ========================================

    @Column(name = "fecha_inicio", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cierre", nullable = false)
    private TipoCierre tipoCierre;

    @Column(name = "motivo")
    private String motivo;

    // ========================================
    // HORARIO REDUCIDO (solo si aplica)
    // ========================================

    @Column(name = "horario_inicio")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horarioInicio;

    @Column(name = "horario_fin")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horarioFin;

    // ========================================
    // PERSONALIZACIÓN POR TENANT
    // ========================================

    @Column(name = "mensaje_personalizado", columnDefinition = "TEXT")
    private String mensajePersonalizado;

    // Arrays JSON de empleados/servicios afectados (si aplica)
    @Column(name = "empleados_afectados", columnDefinition = "TEXT")
    private String empleadosAfectados; // JSON: ["emp1", "emp2"]

    @Column(name = "servicios_afectados", columnDefinition = "TEXT")
    private String serviciosAfectados; // JSON: ["serv1", "serv2"]

    // ========================================
    // CONFIGURACIÓN
    // ========================================

    @Column(name = "notificar_clientes_existentes")
    private Boolean notificarClientesExistentes = true;

    @Column(name = "creado_por")
    private String creadoPor;

    // ========================================
    // AUDITORIA AUTOMÁTICA
    // ========================================

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion = LocalDateTime.now();

    @Column(name = "activo")
    private Boolean activo = true;

    // ========================================
    // CONSTRUCTORES
    // ========================================

    public HorarioEspecial() {}

    public HorarioEspecial(String tenantId, LocalDate fechaInicio, LocalDate fechaFin,
                           TipoCierre tipoCierre, String motivo) {
        this.tenantId = tenantId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.tipoCierre = tipoCierre;
        this.motivo = motivo;
    }

    // ========================================
    // FACTORY METHODS (Zero Hardcoding)
    // ========================================

    /**
     * Crear cierre de emergencia sin textos hardcodeados
     * El mensaje se generará con OpenAI según el tenant
     */
    public static HorarioEspecial crearCierreEmergencia(String tenantId, LocalDate fecha, String motivo) {
        HorarioEspecial horario = new HorarioEspecial();
        horario.setTenantId(tenantId);
        horario.setFechaInicio(fecha);
        horario.setFechaFin(fecha);
        horario.setTipoCierre(TipoCierre.CERRADO_COMPLETO);
        horario.setMotivo(motivo);
        horario.setCreadoPor("cierre_rapido");
        // NO mensaje hardcodeado - se generará dinámicamente
        return horario;
    }

    /**
     * Crear horario reducido sin textos hardcodeados
     */
    public static HorarioEspecial crearHorarioReducido(String tenantId, LocalDate fechaInicio,
                                                       LocalDate fechaFin, LocalTime inicio,
                                                       LocalTime fin, String motivo) {
        HorarioEspecial horario = new HorarioEspecial();
        horario.setTenantId(tenantId);
        horario.setFechaInicio(fechaInicio);
        horario.setFechaFin(fechaFin);
        horario.setTipoCierre(TipoCierre.HORARIO_REDUCIDO);
        horario.setHorarioInicio(inicio);
        horario.setHorarioFin(fin);
        horario.setMotivo(motivo);
        // NO mensaje hardcodeado - se generará dinámicamente
        return horario;
    }

    // ========================================
    // MÉTODOS DE NEGOCIO (Zero Hardcoding)
    // ========================================

    /**
     * Verifica si este cierre está activo en una fecha específica
     */
    public boolean estaActivoEnFecha(LocalDate fecha) {
        return activo &&
                (fecha.equals(fechaInicio) || fecha.equals(fechaFin) ||
                        (fecha.isAfter(fechaInicio) && fecha.isBefore(fechaFin)));
    }

    /**
     * Verifica si un horario específico está afectado por este cierre
     */
    public boolean afectaHorario(LocalTime hora) {
        if (tipoCierre == TipoCierre.CERRADO_COMPLETO) {
            return true; // Bloquea todo el día
        }
        if (tipoCierre == TipoCierre.HORARIO_REDUCIDO && horarioInicio != null && horarioFin != null) {
            return hora.isBefore(horarioInicio) || hora.isAfter(horarioFin);
        }
        return false; // Otros tipos no bloquean por horario
    }

    /**
     * Obtiene el mensaje para la IA sin hardcoding
     * Si hay mensaje personalizado lo usa, si no, retorna null para que OpenAI genere uno
     */
    public String obtenerMensajeParaIA() {
        if (mensajePersonalizado != null && !mensajePersonalizado.trim().isEmpty()) {
            return mensajePersonalizado;
        }

        // NO retornar mensajes hardcodeados
        // OpenAI generará el mensaje según el tenant y contexto
        return null;
    }

    /**
     * Verifica si es un cierre que afecta a todo el salón
     */
    public boolean afectaTodoElSalon() {
        return tipoCierre == TipoCierre.CERRADO_COMPLETO ||
                tipoCierre == TipoCierre.HORARIO_REDUCIDO ||
                tipoCierre == TipoCierre.SOLO_EMERGENCIAS;
    }

    /**
     * Verifica si es un cierre específico (empleado o servicio)
     */
    public boolean esCierreEspecifico() {
        return tipoCierre == TipoCierre.EMPLEADO_AUSENTE ||
                tipoCierre == TipoCierre.SERVICIO_NO_DISPONIBLE;
    }

    // ========================================
    // TRIGGERS JPA
    // ========================================

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        fechaModificacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDateTime.now();
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public TipoCierre getTipoCierre() {
        return tipoCierre;
    }

    public void setTipoCierre(TipoCierre tipoCierre) {
        this.tipoCierre = tipoCierre;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalTime getHorarioInicio() {
        return horarioInicio;
    }

    public void setHorarioInicio(LocalTime horarioInicio) {
        this.horarioInicio = horarioInicio;
    }

    public LocalTime getHorarioFin() {
        return horarioFin;
    }

    public void setHorarioFin(LocalTime horarioFin) {
        this.horarioFin = horarioFin;
    }

    public String getMensajePersonalizado() {
        return mensajePersonalizado;
    }

    public void setMensajePersonalizado(String mensajePersonalizado) {
        this.mensajePersonalizado = mensajePersonalizado;
    }

    public String getEmpleadosAfectados() {
        return empleadosAfectados;
    }

    public void setEmpleadosAfectados(String empleadosAfectados) {
        this.empleadosAfectados = empleadosAfectados;
    }

    public String getServiciosAfectados() {
        return serviciosAfectados;
    }

    public void setServiciosAfectados(String serviciosAfectados) {
        this.serviciosAfectados = serviciosAfectados;
    }

    public Boolean getNotificarClientesExistentes() {
        return notificarClientesExistentes;
    }

    public void setNotificarClientesExistentes(Boolean notificarClientesExistentes) {
        this.notificarClientesExistentes = notificarClientesExistentes;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return "HorarioEspecial{" +
                "id='" + id + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", fechaInicio=" + fechaInicio +
                ", fechaFin=" + fechaFin +
                ", tipoCierre=" + tipoCierre +
                ", motivo='" + motivo + '\'' +
                ", activo=" + activo +
                '}';
    }
}