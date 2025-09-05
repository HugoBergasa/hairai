package com.peluqueria.recepcionista_virtual.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * DTO para los datos extraídos de una cita
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatosCita implements Serializable {

    private String servicio;
    private String fecha; // String original del usuario
    private String hora;  // String original del usuario
    private String nombreCliente;
    private String telefono;
    private String email;
    private Long empleadoId;
    private String notasAdicionales;

    // Campos procesados
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaProcesada;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaProcesada;

    // Constructor vacío
    public DatosCita() {
    }

    // Métodos de validación y procesamiento
    public boolean tienesDatosMinimos() {
        return servicio != null && !servicio.isEmpty() &&
                fecha != null && !fecha.isEmpty() &&
                hora != null && !hora.isEmpty();
    }

    public boolean tienesDatosCliente() {
        return (nombreCliente != null && !nombreCliente.isEmpty()) ||
                (telefono != null && !telefono.isEmpty());
    }

    /**
     * Intenta procesar la fecha string a LocalDate
     */
    public LocalDate procesarFecha() {
        if (fecha == null || fecha.isEmpty()) {
            return null;
        }

        try {
            // Intentar varios formatos comunes
            String[] formatos = {
                    "yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy",
                    "d/M/yyyy", "yyyy/MM/dd"
            };

            for (String formato : formatos) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato);
                    this.fechaProcesada = LocalDate.parse(fecha, formatter);
                    return this.fechaProcesada;
                } catch (DateTimeParseException e) {
                    // Intentar siguiente formato
                }
            }

            // Si contiene "mañana", "hoy", etc.
            if (fecha.toLowerCase().contains("hoy")) {
                this.fechaProcesada = LocalDate.now();
                return this.fechaProcesada;
            } else if (fecha.toLowerCase().contains("mañana")) {
                this.fechaProcesada = LocalDate.now().plusDays(1);
                return this.fechaProcesada;
            }

        } catch (Exception e) {
            // Log error
        }

        return null;
    }

    /**
     * Intenta procesar la hora string a LocalTime
     */
    public LocalTime procesarHora() {
        if (hora == null || hora.isEmpty()) {
            return null;
        }

        try {
            // Limpiar la hora
            String horaLimpia = hora.replace(".", ":")
                    .replace("h", "")
                    .trim();

            // Intentar varios formatos
            String[] formatos = {"HH:mm", "H:mm", "HH:mm:ss"};

            for (String formato : formatos) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formato);
                    this.horaProcesada = LocalTime.parse(horaLimpia, formatter);
                    return this.horaProcesada;
                } catch (DateTimeParseException e) {
                    // Intentar siguiente formato
                }
            }

            // Si es solo un número (ej: "10" para las 10:00)
            if (horaLimpia.matches("\\d{1,2}")) {
                int h = Integer.parseInt(horaLimpia);
                if (h >= 0 && h <= 23) {
                    this.horaProcesada = LocalTime.of(h, 0);
                    return this.horaProcesada;
                }
            }

        } catch (Exception e) {
            // Log error
        }

        return null;
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
        procesarFecha(); // Procesar automáticamente
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
        procesarHora(); // Procesar automáticamente
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getEmpleadoId() {
        return empleadoId;
    }

    public void setEmpleadoId(Long empleadoId) {
        this.empleadoId = empleadoId;
    }

    public String getNotasAdicionales() {
        return notasAdicionales;
    }

    public void setNotasAdicionales(String notasAdicionales) {
        this.notasAdicionales = notasAdicionales;
    }

    public LocalDate getFechaProcesada() {
        if (fechaProcesada == null && fecha != null) {
            procesarFecha();
        }
        return fechaProcesada;
    }

    public void setFechaProcesada(LocalDate fechaProcesada) {
        this.fechaProcesada = fechaProcesada;
    }

    public LocalTime getHoraProcesada() {
        if (horaProcesada == null && hora != null) {
            procesarHora();
        }
        return horaProcesada;
    }

    public void setHoraProcesada(LocalTime horaProcesada) {
        this.horaProcesada = horaProcesada;
    }

    @Override
    public String toString() {
        return "DatosCita{" +
                "servicio='" + servicio + '\'' +
                ", fecha='" + fecha + '\'' +
                ", hora='" + hora + '\'' +
                ", nombreCliente='" + nombreCliente + '\'' +
                ", telefono='" + telefono + '\'' +
                '}';
    }
}