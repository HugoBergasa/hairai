package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.TipoCierre;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO para crear/actualizar horarios especiales
 *
 * ZERO HARDCODING: No contiene textos fijos
 * MULTITENANT: Se usa con tenantId del RequestAttribute
 * OpenAI CEREBRO: mensaje_personalizado es opcional, si está vacío OpenAI genera
 */
public class HorarioEspecialDTO {

    private String id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    @NotNull(message = "El tipo de cierre es obligatorio")
    private TipoCierre tipoCierre;

    private String motivo;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horarioInicio;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horarioFin;

    // ZERO HARDCODING: Mensaje personalizado opcional - si está vacío, OpenAI genera
    private String mensajePersonalizado;

    private List<String> empleadosAfectados;

    private List<String> serviciosAfectados;

    private Boolean notificarClientesExistentes = true;

    private String creadoPor;

    // Constructores
    public HorarioEspecialDTO() {}

    public HorarioEspecialDTO(LocalDate fechaInicio, LocalDate fechaFin,
                              TipoCierre tipoCierre, String motivo) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.tipoCierre = tipoCierre;
        this.motivo = motivo;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public TipoCierre getTipoCierre() { return tipoCierre; }
    public void setTipoCierre(TipoCierre tipoCierre) { this.tipoCierre = tipoCierre; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalTime getHorarioInicio() { return horarioInicio; }
    public void setHorarioInicio(LocalTime horarioInicio) { this.horarioInicio = horarioInicio; }

    public LocalTime getHorarioFin() { return horarioFin; }
    public void setHorarioFin(LocalTime horarioFin) { this.horarioFin = horarioFin; }

    public String getMensajePersonalizado() { return mensajePersonalizado; }
    public void setMensajePersonalizado(String mensajePersonalizado) { this.mensajePersonalizado = mensajePersonalizado; }

    public List<String> getEmpleadosAfectados() { return empleadosAfectados; }
    public void setEmpleadosAfectados(List<String> empleadosAfectados) { this.empleadosAfectados = empleadosAfectados; }

    public List<String> getServiciosAfectados() { return serviciosAfectados; }
    public void setServiciosAfectados(List<String> serviciosAfectados) { this.serviciosAfectados = serviciosAfectados; }

    public Boolean getNotificarClientesExistentes() { return notificarClientesExistentes; }
    public void setNotificarClientesExistentes(Boolean notificarClientesExistentes) { this.notificarClientesExistentes = notificarClientesExistentes; }

    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }
}