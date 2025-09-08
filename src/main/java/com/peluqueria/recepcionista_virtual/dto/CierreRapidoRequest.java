package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.TipoCierre;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
/**
 * DTO para cierres rápidos de emergencia
 *
 * ZERO HARDCODING: Solo fecha y motivo, sin mensajes predefinidos
 * MULTITENANT: Se asocia automáticamente con el tenant del request
 */
public class CierreRapidoRequest {

    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotBlank(message = "El motivo es obligatorio")
    private String motivo;

    // OPCIONAL: Si el usuario quiere un mensaje específico, si no OpenAI lo genera
    private String mensajePersonalizado;

    // Constructores
    public CierreRapidoRequest() {}

    public CierreRapidoRequest(LocalDate fecha, String motivo) {
        this.fecha = fecha;
        this.motivo = motivo;
    }

    // Getters y Setters
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getMensajePersonalizado() { return mensajePersonalizado; }
    public void setMensajePersonalizado(String mensajePersonalizado) { this.mensajePersonalizado = mensajePersonalizado; }
}