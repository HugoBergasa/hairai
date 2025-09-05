package com.peluqueria.recepcionista_virtual.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * DTO para datos de cita extraídos por OpenAI
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatosCita implements Serializable {

    private String servicio;
    private String fecha;
    private String hora;
    private String nombreCliente;
    private String telefono;
    private String notas;

    // Constructor vacío
    public DatosCita() {}

    // Constructor con datos mínimos
    public DatosCita(String servicio, String fecha, String hora, String nombreCliente) {
        this.servicio = servicio;
        this.fecha = fecha;
        this.hora = hora;
        this.nombreCliente = nombreCliente;
    }

    /**
     * VALIDACIÓN - Verifica si tiene datos mínimos para crear cita
     */
    public boolean tienesDatosMinimos() {
        return servicio != null && !servicio.trim().isEmpty() &&
                fecha != null && !fecha.trim().isEmpty() &&
                hora != null && !hora.trim().isEmpty();
    }

    /**
     * VALIDACIÓN - Verifica si está completo
     */
    public boolean isCompleto() {
        return tienesDatosMinimos() &&
                nombreCliente != null && !nombreCliente.trim().isEmpty();
    }

    // Getters y Setters
    public String getServicio() {
        return servicio;
    }

    public void setServicio(String servicio) {
        this.servicio = servicio;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    @Override
    public String toString() {
        return "DatosCita{" +
                "servicio='" + servicio + '\'' +
                ", fecha='" + fecha + '\'' +
                ", hora='" + hora + '\'' +
                ", nombreCliente='" + nombreCliente + '\'' +
                ", telefono='" + telefono + '\'' +
                ", notas='" + notas + '\'' +
                '}';
    }
}