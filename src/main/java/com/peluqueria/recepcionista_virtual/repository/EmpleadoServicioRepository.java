package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.EmpleadoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para la tabla empleados_servicios
 * MULTITENANT: Todas las queries incluyen tenant_id
 * ZERO HARDCODING: Solo queries técnicas
 */
@Repository
public interface EmpleadoServicioRepository extends JpaRepository<EmpleadoServicio, String> {

    /**
     * CRITICO: Verificar si empleado esta autorizado para servicio especifico
     */
    @Query("SELECT es FROM EmpleadoServicio es WHERE es.empleadoId = :empleadoId " +
            "AND es.servicioId = :servicioId AND es.tenantId = :tenantId AND es.disponible = true")
    Optional<EmpleadoServicio> findByEmpleadoAndServicioAndTenant(
            @Param("empleadoId") String empleadoId,
            @Param("servicioId") String servicioId,
            @Param("tenantId") String tenantId
    );

    /**
     * CRITICO: Contar configuraciones especificas de empleado por tenant
     */
    @Query("SELECT COUNT(es) FROM EmpleadoServicio es WHERE es.empleadoId = :empleadoId " +
            "AND es.tenantId = :tenantId")
    int countByEmpleadoAndTenant(@Param("empleadoId") String empleadoId,
                                 @Param("tenantId") String tenantId);

    /**
     * Obtener todos los servicios autorizados para un empleado
     */
    @Query("SELECT es FROM EmpleadoServicio es WHERE es.empleadoId = :empleadoId " +
            "AND es.tenantId = :tenantId AND es.disponible = true " +
            "ORDER BY es.prioridad DESC")
    List<EmpleadoServicio> findServiciosAutorizados(@Param("empleadoId") String empleadoId,
                                                    @Param("tenantId") String tenantId);

    /**
     * Obtener empleados autorizados para un servicio especifico
     */
    @Query("SELECT es FROM EmpleadoServicio es WHERE es.servicioId = :servicioId " +
            "AND es.tenantId = :tenantId AND es.disponible = true " +
            "ORDER BY es.nivelExperiencia DESC, es.prioridad DESC")
    List<EmpleadoServicio> findEmpleadosAutorizados(@Param("servicioId") String servicioId,
                                                    @Param("tenantId") String tenantId);

    /**
     * Verificar si existe configuracion especifica para empleado
     */
    boolean existsByEmpleadoIdAndServicioIdAndTenantIdAndDisponible(
            String empleadoId, String servicioId, String tenantId, boolean disponible
    );

    /**
     * Obtener configuracion de empleado-servicio con precios personalizados
     */
    @Query("SELECT es FROM EmpleadoServicio es WHERE es.empleadoId = :empleadoId " +
            "AND es.servicioId = :servicioId AND es.tenantId = :tenantId " +
            "AND es.precioPersonalizado IS NOT NULL")
    Optional<EmpleadoServicio> findConfiguracionConPrecio(@Param("empleadoId") String empleadoId,
                                                          @Param("servicioId") String servicioId,
                                                          @Param("tenantId") String tenantId);
}