package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.EmpleadosServicios;
import com.peluqueria.recepcionista_virtual.model.EmpleadoServicioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpleadosServiciosRepository extends JpaRepository<EmpleadosServicios, EmpleadoServicioId> {

    List<EmpleadosServicios> findByTenantId(String tenantId);

    List<EmpleadosServicios> findByEmpleadoId(Long empleadoId);

    List<EmpleadosServicios> findByServicioId(Long servicioId);

    List<EmpleadosServicios> findByTenantIdAndActivo(String tenantId, Boolean activo);

    @Query("SELECT es FROM EmpleadosServicios es WHERE es.tenantId = :tenantId " +
            "AND es.empleadoId = :empleadoId AND es.activo = true")
    List<EmpleadosServicios> findServiciosActivosEmpleado(
            @Param("tenantId") String tenantId,
            @Param("empleadoId") Long empleadoId
    );

    @Query("SELECT es FROM EmpleadosServicios es WHERE es.tenantId = :tenantId " +
            "AND es.servicioId = :servicioId AND es.activo = true")
    List<EmpleadosServicios> findEmpleadosActivosServicio(
            @Param("tenantId") String tenantId,
            @Param("servicioId") Long servicioId
    );
}