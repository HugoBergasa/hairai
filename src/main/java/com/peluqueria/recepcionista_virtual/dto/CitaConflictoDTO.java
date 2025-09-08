package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.Cita;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CitaConflictoDTO {
    private String id;
    private String clienteNombre;
    private String clienteTelefono;
    private String servicio;
    private String fechaHora;
    private String estado;

    // Constructor vacío
    public CitaConflictoDTO() {}

    // Constructor desde entidad
    public CitaConflictoDTO(Cita cita) {
        this.id = cita.getId();
        this.clienteNombre = cita.getCliente() != null ? cita.getCliente().getNombre() : "Cliente";
        this.clienteTelefono = cita.getCliente() != null ? cita.getCliente().getTelefono() : "";
        this.servicio = cita.getServicio() != null ? cita.getServicio().getNombre() : "Servicio";
        this.fechaHora = cita.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        this.estado = cita.getEstado().toString();
    }

    // Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getClienteTelefono() { return clienteTelefono; }
    public void setClienteTelefono(String clienteTelefono) { this.clienteTelefono = clienteTelefono; }

    public String getServicio() { return servicio; }
    public void setServicio(String servicio) { this.servicio = servicio; }

    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}