package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.User;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private String id;
    private String nombre;
    private String email;
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