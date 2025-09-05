package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, String> {
    @Query("SELECT e FROM Empleado e WHERE e.tenant.id = :tenantId")
    List<Empleado> findByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT e FROM Empleado e WHERE e.tenant.id = :tenantId AND e.activo = true")
    List<Empleado> findByTenantIdAndActivoTrue(@Param("tenantId") String tenantId);
}