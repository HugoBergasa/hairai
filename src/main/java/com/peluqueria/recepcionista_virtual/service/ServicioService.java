package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.*;
import com.peluqueria.recepcionista_virtual.repository.*;
import com.peluqueria.recepcionista_virtual.dto.ServicioDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ServicioService {

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private TenantRepository tenantRepository;

    public List<ServicioDTO> getServiciosByTenantId(String tenantId) {
        List<Servicio> servicios = servicioRepository.findActivosByTenantId(tenantId);
        return servicios.stream()
                .map(ServicioDTO::fromServicio)
                .collect(Collectors.toList());
    }

    public ServicioDTO createServicio(String tenantId, ServicioDTO servicioDTO) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        Servicio servicio = new Servicio();
        servicio.setTenant(tenant);
        servicio.setNombre(servicioDTO.getNombre());
        servicio.setDescripcion(servicioDTO.getDescripcion());
        servicio.setPrecio(servicioDTO.getPrecio());
        servicio.setDuracionMinutos(servicioDTO.getDuracionMinutos());
        servicio.setActivo(true);

        Servicio servicioGuardado = servicioRepository.save(servicio);
        return ServicioDTO.fromServicio(servicioGuardado);
    }

    public ServicioDTO updateServicio(String servicioId, ServicioDTO servicioDTO) {
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        if (servicioDTO.getNombre() != null) {
            servicio.setNombre(servicioDTO.getNombre());
        }
        if (servicioDTO.getDescripcion() != null) {
            servicio.setDescripcion(servicioDTO.getDescripcion());
        }
        if (servicioDTO.getPrecio() != null) {
            servicio.setPrecio(servicioDTO.getPrecio());
        }
        if (servicioDTO.getDuracionMinutos() != null) {
            servicio.setDuracionMinutos(servicioDTO.getDuracionMinutos());
        }
        if (servicioDTO.getActivo() != null) {
            servicio.setActivo(servicioDTO.getActivo());
        }

        Servicio servicioActualizado = servicioRepository.save(servicio);
        return ServicioDTO.fromServicio(servicioActualizado);
    }

    public void deleteServicio(String servicioId) {
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        // Soft delete - marcar como inactivo
        servicio.setActivo(false);
        servicioRepository.save(servicio);
    }

    public ServicioDTO getServicioById(String servicioId) {
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
        return ServicioDTO.fromServicio(servicio);
    }
}