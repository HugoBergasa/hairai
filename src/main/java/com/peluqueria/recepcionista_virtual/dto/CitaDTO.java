package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.Cita;
import com.peluqueria.recepcionista_virtual.model.EstadoCita;
import com.peluqueria.recepcionista_virtual.model.OrigenCita;
import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CitaDTO {
    private String id;

    @NotNull(message = "La fecha y hora son requeridas")
    @Future(message = "La fecha debe ser en el futuro")
    private LocalDateTime fechaHora;

    @NotNull(message = "El estado es requerido")
    private EstadoCita estado;

    @NotNull(message = "El origen es requerido")
    private OrigenCita origen;

    @Min(value = 15, message = "La duración mínima es 15 minutos")
    @Max(value = 480, message = "La duración máxima es 480 minutos")
    private Integer duracionMinutos;

    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @DecimalMax(value = "9999.99", message = "El precio no puede exceder 9999.99")
    private BigDecimal precio;

    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    private String notas;

    private Boolean recordatorioEnviado;

    // Relaciones - Solo IDs para validaciones básicas
    @NotBlank(message = "El cliente es requerido")
    private String clienteId;

    @Size(max = 100, message = "El nombre del cliente no puede exceder 100 caracteres")
    private String clienteNombre;

    @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "El teléfono debe tener formato válido")
    private String clienteTelefono;

    @NotBlank(message = "El servicio es requerido")
    private String servicioId;

    private String servicioNombre;
    private String empleadoId;
    private String empleadoNombre;
    private String tenantId;

    public static CitaDTO fromCita(Cita cita) {
        CitaDTO dto = new CitaDTO();
        dto.setId(cita.getId());
        dto.setFechaHora(cita.getFechaHora());
        dto.setEstado(cita.getEstado());
        dto.setOrigen(cita.getOrigen());
        dto.setDuracionMinutos(cita.getDuracionMinutos());
        dto.setPrecio(cita.getPrecio());
        dto.setNotas(cita.getNotas());
        dto.setRecordatorioEnviado(cita.getRecordatorioEnviado());
        dto.setTenantId(cita.getTenant().getId());

        if (cita.getCliente() != null) {
            dto.setClienteId(cita.getCliente().getId());
            dto.setClienteNombre(cita.getCliente().getNombre());
            dto.setClienteTelefono(cita.getCliente().getTelefono());
        }

        if (cita.getServicio() != null) {
            dto.setServicioId(cita.getServicio().getId());
            dto.setServicioNombre(cita.getServicio().getNombre());
        }

        if (cita.getEmpleado() != null) {
            dto.setEmpleadoId(cita.getEmpleado().getId());
            dto.setEmpleadoNombre(cita.getEmpleado().getNombre());
        }

        return dto;
    }
}