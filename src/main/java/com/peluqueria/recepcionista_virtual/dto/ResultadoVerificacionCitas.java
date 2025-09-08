package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.Cita;
import java.util.List;

/**
 * DTO para resultado de verificación de citas antes de crear cierre
 *
 * MULTITENANT: Solo citas del tenant actual
 * ZERO HARDCODING: Mensajes configurables
 */
public class ResultadoVerificacionCitas {

    private boolean hayCitasAfectadas;
    private int numeroCitasAfectadas;
    private List<Cita> citasAfectadas;
    private String mensajeAviso;
    private boolean requiereConfirmacion;

    // Constructors
    public ResultadoVerificacionCitas() {}

    public static ResultadoVerificacionCitas sinCitasAfectadas() {
        ResultadoVerificacionCitas resultado = new ResultadoVerificacionCitas();
        resultado.setHayCitasAfectadas(false);
        resultado.setNumeroCitasAfectadas(0);
        resultado.setRequiereConfirmacion(false);
        resultado.setMensajeAviso("No hay citas programadas en estas fechas. Puede proceder con el cierre.");
        return resultado;
    }

    public static ResultadoVerificacionCitas conCitasAfectadas(List<Cita> citas) {
        ResultadoVerificacionCitas resultado = new ResultadoVerificacionCitas();
        resultado.setHayCitasAfectadas(true);
        resultado.setNumeroCitasAfectadas(citas.size());
        resultado.setCitasAfectadas(citas);
        resultado.setRequiereConfirmacion(true);

        String mensaje = String.format(
                "ATENCION: Hay %d cita%s programada%s en estas fechas. " +
                        "Si confirma el cierre, se enviaran notificaciones automaticas a todos los clientes afectados " +
                        "informandoles que deben reprogramar sus citas.",
                citas.size(),
                citas.size() == 1 ? "" : "s",
                citas.size() == 1 ? "" : "s"
        );
        resultado.setMensajeAviso(mensaje);

        return resultado;
    }

    // Getters y Setters
    public boolean isHayCitasAfectadas() { return hayCitasAfectadas; }
    public void setHayCitasAfectadas(boolean hayCitasAfectadas) { this.hayCitasAfectadas = hayCitasAfectadas; }

    public int getNumeroCitasAfectadas() { return numeroCitasAfectadas; }
    public void setNumeroCitasAfectadas(int numeroCitasAfectadas) { this.numeroCitasAfectadas = numeroCitasAfectadas; }

    public List<Cita> getCitasAfectadas() { return citasAfectadas; }
    public void setCitasAfectadas(List<Cita> citasAfectadas) { this.citasAfectadas = citasAfectadas; }

    public String getMensajeAviso() { return mensajeAviso; }
    public void setMensajeAviso(String mensajeAviso) { this.mensajeAviso = mensajeAviso; }

    public boolean isRequiereConfirmacion() { return requiereConfirmacion; }
    public void setRequiereConfirmacion(boolean requiereConfirmacion) { this.requiereConfirmacion = requiereConfirmacion; }
}