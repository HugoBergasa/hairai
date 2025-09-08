package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.Cita;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO para resultado de verificación de citas antes de crear cierre
 *
 * MULTITENANT: Solo citas del tenant actual
 * ZERO HARDCODING: Mensajes configurables
 */
public class ResultadoVerificacionCitas {

    private boolean hayCitasAfectadas;
    private int numeroCitasAfectadas;

    // 🔧 CORRECCIÓN: Usar DTOs en lugar de entidades para evitar recursión
    private List<Map<String, Object>> citasAfectadas;

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

    // 🔧 MÉTODO ORIGINAL CORREGIDO: Convierte automáticamente a DTOs
    public static ResultadoVerificacionCitas conCitasAfectadas(List<Cita> citas) {
        ResultadoVerificacionCitas resultado = new ResultadoVerificacionCitas();
        resultado.setHayCitasAfectadas(true);
        resultado.setNumeroCitasAfectadas(citas.size());
        resultado.setRequiereConfirmacion(true);

        // Convertir entidades a DTOs para evitar recursión JSON
        List<Map<String, Object>> citasDTO = citas.stream()
                .map(cita -> {
                    Map<String, Object> citaData = new java.util.HashMap<>();
                    citaData.put("id", cita.getId());
                    citaData.put("clienteNombre", cita.getCliente() != null ? cita.getCliente().getNombre() : "Cliente");
                    citaData.put("clienteTelefono", cita.getCliente() != null ? cita.getCliente().getTelefono() : "");
                    citaData.put("servicio", cita.getServicio() != null ? cita.getServicio().getNombre() : "Servicio");
                    citaData.put("fechaHora", cita.getFechaHora().toString());
                    citaData.put("estado", cita.getEstado().toString());
                    return citaData;
                })
                .collect(Collectors.toList());

        resultado.setCitasAfectadas(citasDTO);

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

    // 🔧 NUEVO MÉTODO: Para cuando ya tienes DTOs
    public static ResultadoVerificacionCitas conCitasAfectadasDTO(List<Map<String, Object>> citasDTO) {
        ResultadoVerificacionCitas resultado = new ResultadoVerificacionCitas();
        resultado.setHayCitasAfectadas(true);
        resultado.setNumeroCitasAfectadas(citasDTO.size());
        resultado.setCitasAfectadas(citasDTO);
        resultado.setRequiereConfirmacion(true);

        String mensaje = String.format(
                "ATENCION: Hay %d cita%s programada%s en estas fechas. " +
                        "Si confirma el cierre, se enviaran notificaciones automaticas a todos los clientes afectados " +
                        "informandoles que deben reprogramar sus citas.",
                citasDTO.size(),
                citasDTO.size() == 1 ? "" : "s",
                citasDTO.size() == 1 ? "" : "s"
        );
        resultado.setMensajeAviso(mensaje);

        return resultado;
    }

    // Getters y Setters actualizados
    public boolean isHayCitasAfectadas() { return hayCitasAfectadas; }
    public void setHayCitasAfectadas(boolean hayCitasAfectadas) { this.hayCitasAfectadas = hayCitasAfectadas; }

    public int getNumeroCitasAfectadas() { return numeroCitasAfectadas; }
    public void setNumeroCitasAfectadas(int numeroCitasAfectadas) { this.numeroCitasAfectadas = numeroCitasAfectadas; }

    // 🔧 TIPO CAMBIADO: Ahora es List<Map<String, Object>> en lugar de List<Cita>
    public List<Map<String, Object>> getCitasAfectadas() { return citasAfectadas; }
    public void setCitasAfectadas(List<Map<String, Object>> citasAfectadas) { this.citasAfectadas = citasAfectadas; }

    public String getMensajeAviso() { return mensajeAviso; }
    public void setMensajeAviso(String mensajeAviso) { this.mensajeAviso = mensajeAviso; }

    public boolean isRequiereConfirmacion() { return requiereConfirmacion; }
    public void setRequiereConfirmacion(boolean requiereConfirmacion) { this.requiereConfirmacion = requiereConfirmacion; }
}