package com.peluqueria.recepcionista_virtual.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class RegisterRequest {
    @NotBlank
    private String nombre;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;

    // SOLUCIÓN: Mapear ambos nombres
    @JsonProperty("isNewTenant")
    private boolean newTenant = true;

    // Si es nuevo tenant
    private String nombrePeluqueria;
    private String telefono;
    private String direccion;

    // Si es tenant existente
    private String tenantId;

    // Método adicional para compatibilidad
    public boolean isNewTenant() {
        return newTenant;
    }
}