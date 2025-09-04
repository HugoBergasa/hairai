package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.*;
import com.peluqueria.recepcionista_virtual.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class TenantService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    public Tenant save(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    public Tenant findById(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));
    }

    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    public void createDefaultServices(String tenantId) {
        Tenant tenant = findById(tenantId);

        // Servicios predeterminados
        String[][] serviciosDefault = {
                {"Corte de pelo", "Corte profesional con lavado y secado", "20", "30"},
                {"Tinte completo", "Coloración completa del cabello", "50", "90"},
                {"Mechas", "Mechas con papel de aluminio", "60", "120"},
                {"Peinado evento", "Peinado especial para eventos", "35", "45"},
                {"Tratamiento keratina", "Alisado con keratina", "80", "150"},
                {"Manicura", "Manicura completa con esmaltado", "25", "45"},
                {"Pedicura", "Pedicura completa con esmaltado", "30", "60"}
        };

        for (String[] s : serviciosDefault) {
            Servicio servicio = new Servicio();
            servicio.setTenant(tenant);
            servicio.setNombre(s[0]);
            servicio.setDescripcion(s[1]);
            servicio.setPrecio(new BigDecimal(s[2]));
            servicio.setDuracionMinutos(Integer.parseInt(s[3]));
            servicio.setActivo(true);
            servicioRepository.save(servicio);
        }

        // Empleado predeterminado
        Empleado empleado = new Empleado();
        empleado.setTenant(tenant);
        empleado.setNombre("Empleado General");
        empleado.setEspecialidad("Servicios generales");
        empleado.setActivo(true);
        empleadoRepository.save(empleado);
    }

    public Tenant updateTenant(String id, Tenant tenantData) {
        Tenant tenant = findById(id);

        if (tenantData.getNombrePeluqueria() != null) {
            tenant.setNombrePeluqueria(tenantData.getNombrePeluqueria());
        }
        if (tenantData.getTelefono() != null) {
            tenant.setTelefono(tenantData.getTelefono());
        }
        if (tenantData.getEmail() != null) {
            tenant.setEmail(tenantData.getEmail());
        }
        if (tenantData.getDireccion() != null) {
            tenant.setDireccion(tenantData.getDireccion());
        }
        if (tenantData.getHoraApertura() != null) {
            tenant.setHoraApertura(tenantData.getHoraApertura());
        }
        if (tenantData.getHoraCierre() != null) {
            tenant.setHoraCierre(tenantData.getHoraCierre());
        }
        if (tenantData.getDiasLaborables() != null) {
            tenant.setDiasLaborables(tenantData.getDiasLaborables());
        }
        if (tenantData.getMensajeBienvenida() != null) {
            tenant.setMensajeBienvenida(tenantData.getMensajeBienvenida());
        }

        return tenantRepository.save(tenant);
    }
}