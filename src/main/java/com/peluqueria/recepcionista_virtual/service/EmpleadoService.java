package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.*;
import com.peluqueria.recepcionista_virtual.repository.*;
import com.peluqueria.recepcionista_virtual.dto.EmpleadoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private TenantRepository tenantRepository;

    public List<EmpleadoDTO> getEmpleadosByTenantId(String tenantId) {
        List<Empleado> empleados = empleadoRepository.findByTenantIdAndActivoTrue(tenantId);
        return empleados.stream()
                .map(EmpleadoDTO::fromEmpleado)
                .collect(Collectors.toList());
    }

    public EmpleadoDTO createEmpleado(String tenantId, EmpleadoDTO empleadoDTO) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        Empleado empleado = new Empleado();
        empleado.setTenant(tenant);
        empleado.setNombre(empleadoDTO.getNombre());
        empleado.setEmail(empleadoDTO.getEmail());
        empleado.setTelefono(empleadoDTO.getTelefono());
        empleado.setEspecialidad(empleadoDTO.getEspecialidad());
        empleado.setHoraEntrada(empleadoDTO.getHoraEntrada() != null ?
                empleadoDTO.getHoraEntrada() : "09:00");
        empleado.setHoraSalida(empleadoDTO.getHoraSalida() != null ?
                empleadoDTO.getHoraSalida() : "18:00");
        empleado.setDiasTrabajo(empleadoDTO.getDiasTrabajo() != null ?
                empleadoDTO.getDiasTrabajo() : "L,M,X,J,V");
        empleado.setActivo(true);

        Empleado empleadoGuardado = empleadoRepository.save(empleado);
        return EmpleadoDTO.fromEmpleado(empleadoGuardado);
    }

    public EmpleadoDTO updateEmpleado(String empleadoId, EmpleadoDTO empleadoDTO) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        if (empleadoDTO.getNombre() != null) {
            empleado.setNombre(empleadoDTO.getNombre());
        }
        if (empleadoDTO.getEmail() != null) {
            empleado.setEmail(empleadoDTO.getEmail());
        }
        if (empleadoDTO.getTelefono() != null) {
            empleado.setTelefono(empleadoDTO.getTelefono());
        }
        if (empleadoDTO.getEspecialidad() != null) {
            empleado.setEspecialidad(empleadoDTO.getEspecialidad());
        }
        if (empleadoDTO.getHoraEntrada() != null) {
            empleado.setHoraEntrada(empleadoDTO.getHoraEntrada());
        }
        if (empleadoDTO.getHoraSalida() != null) {
            empleado.setHoraSalida(empleadoDTO.getHoraSalida());
        }
        if (empleadoDTO.getDiasTrabajo() != null) {
            empleado.setDiasTrabajo(empleadoDTO.getDiasTrabajo());
        }
        if (empleadoDTO.getActivo() != null) {
            empleado.setActivo(empleadoDTO.getActivo());
        }

        Empleado empleadoActualizado = empleadoRepository.save(empleado);
        return EmpleadoDTO.fromEmpleado(empleadoActualizado);
    }

    public void deleteEmpleado(String empleadoId) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        // Soft delete - marcar como inactivo
        empleado.setActivo(false);
        empleadoRepository.save(empleado);
    }

    public EmpleadoDTO getEmpleadoById(String empleadoId) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));
        return EmpleadoDTO.fromEmpleado(empleado);
    }
}