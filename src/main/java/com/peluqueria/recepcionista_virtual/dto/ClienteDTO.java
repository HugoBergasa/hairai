package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.Cliente;
import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Data
public class ClienteDTO {
    private String id;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El teléfono es requerido")
    @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "El teléfono debe tener entre 9 y 15 dígitos")
    private String telefono;

    @Email(message = "El email debe tener un formato válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;

    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    private String notas;

    private LocalDateTime fechaRegistro;
    private LocalDateTime ultimaVisita;
    private String tenantId;

    public static ClienteDTO fromCliente(Cliente cliente) {
        ClienteDTO dto = new ClienteDTO();
        dto.setId(cliente.getId());
        dto.setNombre(cliente.getNombre());
        dto.setTelefono(cliente.getTelefono());
        dto.setEmail(cliente.getEmail());
        dto.setNotas(cliente.getNotas());
        dto.setFechaRegistro(cliente.getFechaRegistro());
        dto.setUltimaVisita(cliente.getUltimaVisita());
        dto.setTenantId(cliente.getTenant().getId());
        return dto;
    }
}