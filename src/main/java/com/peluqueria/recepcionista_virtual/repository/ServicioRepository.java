package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServicioRepository extends JpaRepository<Servicio, String> {
    List<Servicio> findByTenantIdAndActivoTrue(String tenantId);
}