package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.Empleado;
import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class EmpleadoDTO {
    private String id;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Email(message = "El email debe tener un formato válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "El teléfono debe tener entre 9 y 15 dígitos")
    private String telefono;

    @Size(max = 200, message = "La especialidad no puede exceder 200 caracteres")
    private String especialidad;

    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Formato de hora inválido (HH:mm)")
    private String horaEntrada;

    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Formato de hora inválido (HH:mm)")
    private String horaSalida;

    @Pattern(regexp = "^[LMXJVSD](,[LMXJVSD])*$", message = "Formato de días inválido (L,M,X,J,V,S,D)")
    private String diasTrabajo;

    private Boolean activo;
    private String tenantId;

    public static EmpleadoDTO fromEmpleado(Empleado empleado) {
        EmpleadoDTO dto = new EmpleadoDTO();
        dto.setId(empleado.getId());
        dto.setNombre(empleado.getNombre());
        dto.setEmail(empleado.getEmail());
        dto.setTelefono(empleado.getTelefono());
        dto.setEspecialidad(empleado.getEspecialidad());
        dto.setHoraEntrada(empleado.getHoraEntrada());
        dto.setHoraSalida(empleado.getHoraSalida());
        dto.setDiasTrabajo(empleado.getDiasTrabajo());
        dto.setActivo(empleado.getActivo());
        dto.setTenantId(empleado.getTenant().getId());
        return dto;
    }
}