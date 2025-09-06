package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.Tenant;
import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class TenantDTO {
    private String id;

    @NotBlank(message = "El nombre de la peluquería es requerido")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombrePeluqueria;

    @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "El teléfono debe tener formato válido")
    private String telefono;

    @Email(message = "El email debe tener un formato válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;

    @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
    private String direccion;

    @NotBlank(message = "La hora de apertura es requerida")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Formato de hora inválido (HH:mm)")
    private String horaApertura;

    @NotBlank(message = "La hora de cierre es requerida")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Formato de hora inválido (HH:mm)")
    private String horaCierre;

    @NotBlank(message = "Los días laborables son requeridos")
    @Pattern(regexp = "^[LMXJVSD](,[LMXJVSD])*$", message = "Formato de días inválido (L,M,X,J,V,S,D)")
    private String diasLaborables;

    @Size(max = 500, message = "El mensaje de bienvenida no puede exceder 500 caracteres")
    private String mensajeBienvenida;

    @Min(value = 15, message = "La duración mínima de cita es 15 minutos")
    @Max(value = 240, message = "La duración máxima de cita es 240 minutos")
    private Integer duracionCitaMinutos;

    public static TenantDTO fromTenant(Tenant tenant) {
        TenantDTO dto = new TenantDTO();
        dto.setId(tenant.getId());
        dto.setNombrePeluqueria(tenant.getNombrePeluqueria());
        dto.setTelefono(tenant.getTelefono());
        dto.setEmail(tenant.getEmail());
        dto.setDireccion(tenant.getDireccion());
        dto.setHoraApertura(tenant.getHoraApertura());
        dto.setHoraCierre(tenant.getHoraCierre());
        dto.setDiasLaborables(tenant.getDiasLaborables());
        dto.setMensajeBienvenida(tenant.getMensajeBienvenida());
        dto.setDuracionCitaMinutos(tenant.getDuracionCitaMinutos());
        return dto;
    }
}