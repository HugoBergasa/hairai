package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, String> {
    List<Empleado> findByTenantId(String tenantId);
    List<Empleado> findByTenantIdAndActivoTrue(String tenantId);
}