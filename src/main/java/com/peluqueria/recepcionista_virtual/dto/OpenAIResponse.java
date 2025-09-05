// OpenAIResponse.java
package com.peluqueria.recepcionista_virtual.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * DTO para la respuesta procesada de OpenAI
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAIResponse implements Serializable {

    private String mensaje;
    private String intencion;
    private boolean requiereAccion;
    private String accion;
    private DatosCita datosCita;
    private double confianza;
    private Long citaId; // Para acciones de modificación/cancelación

    // Constructor vacío
    public OpenAIResponse() {
        this.requiereAccion = false;
        this.accion = "NINGUNA";
        this.confianza = 0.0;
    }

    // Métodos útiles
    public boolean esReservaCita() {
        return "RESERVAR_CITA".equals(intencion) && requiereAccion;
    }

    public boolean esCancelacion() {
        return "CANCELAR_CITA".equals(intencion) && requiereAccion;
    }

    public boolean esConsulta() {
        return "CONSULTAR_INFO".equals(intencion);
    }

    public boolean tienesDatosCompletos() {
        if (datosCita == null) return false;
        return datosCita.tienesDatosMinimos();
    }

    // Getters y Setters
    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getIntencion() {
        return intencion;
    }

    public void setIntencion(String intencion) {
        this.intencion = intencion;
    }

    public boolean isRequiereAccion() {
        return requiereAccion;
    }

    public void setRequiereAccion(boolean requiereAccion) {
        this.requiereAccion = requiereAccion;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public DatosCita getDatosCita() {
        return datosCita;
    }

    public void setDatosCita(DatosCita datosCita) {
        this.datosCita = datosCita;
    }

    public double getConfianza() {
        return confianza;
    }

    public void setConfianza(double confianza) {
        this.confianza = confianza;
    }

    public Long getCitaId() {
        return citaId;
    }

    public void setCitaId(Long citaId) {
        this.citaId = citaId;
    }

    public boolean requiereAccion() {
        return requiereAccion;
    }
}