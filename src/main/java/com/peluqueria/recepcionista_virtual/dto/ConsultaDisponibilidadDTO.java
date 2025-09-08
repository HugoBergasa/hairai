package com.peluqueria.recepcionista_virtual.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para consultas de disponibilidad desde la IA
 *
 * ZERO HARDCODING: Solo datos técnicos de entrada
 * OpenAI CEREBRO: La IA usa este DTO para verificar antes de crear citas
 */
public class ConsultaDisponibilidadDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull
    private LocalDate fecha;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime hora;

    private String empleadoId; // Opcional
    private String servicioId; // Opcional

    // Contexto adicional para la IA
    private String contextoSolicitud; // Lo que pidió el cliente
    private boolean esEmergencia;
    private String canalOrigen; // "telefono", "web", "whatsapp"

    // Constructores
    public ConsultaDisponibilidadDTO() {}

    public ConsultaDisponibilidadDTO(LocalDate fecha, LocalTime hora) {
        this.fecha = fecha;
        this.hora = hora;
    }

    // Getters y Setters
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }

    public String getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(String empleadoId) { this.empleadoId = empleadoId; }

    public String getServicioId() { return servicioId; }
    public void setServicioId(String servicioId) { this.servicioId = servicioId; }

    public String getContextoSolicitud() { return contextoSolicitud; }
    public void setContextoSolicitud(String contextoSolicitud) { this.contextoSolicitud = contextoSolicitud; }

    public boolean isEsEmergencia() { return esEmergencia; }
    public void setEsEmergencia(boolean esEmergencia) { this.esEmergencia = esEmergencia; }

    public String getCanalOrigen() { return canalOrigen; }
    public void setCanalOrigen(String canalOrigen) { this.canalOrigen = canalOrigen; }
}