package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.User;
import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private String id;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe tener un formato válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;

    @NotBlank(message = "El rol es requerido")
    @Pattern(regexp = "^(USER|ADMIN|SUPER_ADMIN)$", message = "El rol debe ser USER, ADMIN o SUPER_ADMIN")
    private String role;

    private String tenantId;
    private LocalDateTime ultimoAcceso;

    public static UserDTO fromUser(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setNombre(user.getNombre());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setTenantId(user.getTenant().getId());
        dto.setUltimoAcceso(user.getUltimoAcceso());
        return dto;
    }
}