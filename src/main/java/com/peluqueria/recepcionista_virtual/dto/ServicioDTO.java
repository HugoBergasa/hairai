package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.Servicio;
import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class ServicioDTO {
    private String id;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcion;

    @NotNull(message = "El precio es requerido")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @DecimalMax(value = "9999.99", message = "El precio no puede exceder 9999.99")
    @Digits(integer = 4, fraction = 2, message = "El precio debe tener máximo 4 dígitos enteros y 2 decimales")
    private BigDecimal precio;

    @NotNull(message = "La duración es requerida")
    @Min(value = 15, message = "La duración debe ser al menos 15 minutos")
    @Max(value = 480, message = "La duración no puede exceder 480 minutos (8 horas)")
    private Integer duracionMinutos;

    private Boolean activo = true;
    private String tenantId;

    public static ServicioDTO fromServicio(Servicio servicio) {
        ServicioDTO dto = new ServicioDTO();
        dto.setId(servicio.getId());
        dto.setNombre(servicio.getNombre());
        dto.setDescripcion(servicio.getDescripcion());
        dto.setPrecio(servicio.getPrecio());
        dto.setDuracionMinutos(servicio.getDuracionMinutos());
        dto.setActivo(servicio.getActivo());
        dto.setTenantId(servicio.getTenant().getId());
        return dto;
    }
}