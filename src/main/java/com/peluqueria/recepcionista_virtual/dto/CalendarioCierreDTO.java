package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.TipoCierre;

import java.time.LocalDate;

public class CalendarioCierreDTO {

    private String id;
    private LocalDate fecha;
    private TipoCierre tipoCierre;
    private String motivo;

    // ZERO HARDCODING: Descripción generada dinámicamente, no hardcodeada
    private String descripcionCorta;

    // MULTITENANT: Estilos configurables por tenant desde BD
    private String colorCSS;
    private String estiloPersonalizado;

    private boolean esRangoCompleto;
    private LocalDate fechaFin;

    // Constructores
    public CalendarioCierreDTO() {}

    public CalendarioCierreDTO(String id, LocalDate fecha, TipoCierre tipoCierre,
                               String motivo, LocalDate fechaFin) {
        this.id = id;
        this.fecha = fecha;
        this.tipoCierre = tipoCierre;
        this.motivo = motivo;
        this.fechaFin = fechaFin;
        this.esRangoCompleto = !fecha.equals(fechaFin);
        // NO generar descripcionCorta hardcodeada aquí - se hace en el service
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public TipoCierre getTipoCierre() { return tipoCierre; }
    public void setTipoCierre(TipoCierre tipoCierre) { this.tipoCierre = tipoCierre; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getDescripcionCorta() { return descripcionCorta; }
    public void setDescripcionCorta(String descripcionCorta) { this.descripcionCorta = descripcionCorta; }

    public String getColorCSS() { return colorCSS; }
    public void setColorCSS(String colorCSS) { this.colorCSS = colorCSS; }

    public String getEstiloPersonalizado() { return estiloPersonalizado; }
    public void setEstiloPersonalizado(String estiloPersonalizado) { this.estiloPersonalizado = estiloPersonalizado; }

    public boolean isEsRangoCompleto() { return esRangoCompleto; }
    public void setEsRangoCompleto(boolean esRangoCompleto) { this.esRangoCompleto = esRangoCompleto; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
}