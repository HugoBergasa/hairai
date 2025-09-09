package com.peluqueria.recepcionista_virtual.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class CitaConflictoDTO {
    private String tipoConflicto;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaHoraAfectada;
    private Map<String, String> detallesAdicionales;
    private String prioridad; // "BAJA", "MEDIA", "ALTA", "CRITICA"

    public CitaConflictoDTO() {}

    public CitaConflictoDTO(String tipoConflicto, String titulo, String descripcion,
                            LocalDateTime fechaHoraAfectada, Map<String, String> detallesAdicionales,
                            String prioridad) {
        this.tipoConflicto = tipoConflicto;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fechaHoraAfectada = fechaHoraAfectada;
        this.detallesAdicionales = detallesAdicionales;
        this.prioridad = prioridad;
    }

    // Getters y Setters
    public String getTipoConflicto() { return tipoConflicto; }
    public void setTipoConflicto(String tipoConflicto) { this.tipoConflicto = tipoConflicto; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFechaHoraAfectada() { return fechaHoraAfectada; }
    public void setFechaHoraAfectada(LocalDateTime fechaHoraAfectada) { this.fechaHoraAfectada = fechaHoraAfectada; }

    public Map<String, String> getDetallesAdicionales() { return detallesAdicionales; }
    public void setDetallesAdicionales(Map<String, String> detallesAdicionales) { this.detallesAdicionales = detallesAdicionales; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }
}