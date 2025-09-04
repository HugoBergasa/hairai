package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.Tenant;
import lombok.Data;

@Data
public class TenantDTO {
    private String id;
    private String nombrePeluqueria;
    private String telefono;
    private String email;
    private String direccion;
    private String horaApertura;
    private String horaCierre;
    private String diasLaborables;

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
        return dto;
    }
}