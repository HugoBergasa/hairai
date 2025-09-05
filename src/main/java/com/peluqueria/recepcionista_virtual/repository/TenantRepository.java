package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {

    /**
     * Encuentra todos los tenants activos
     */
    List<Tenant> findByActivo(boolean activo);

    /**
     * Busca un tenant por su nombre de peluquería
     */
    Optional<Tenant> findByNombrePeluqueria(String nombrePeluqueria);

    /**
     * Busca tenants que contengan un texto en el nombre
     */
    List<Tenant> findByNombrePeluqueriaContainingIgnoreCase(String texto);

    /**
     * Cuenta los tenants activos
     */
    Long countByActivo(boolean activo);

    /**
     * Verifica si existe un tenant con ese ID
     */
    boolean existsByIdAndActivo(String id, boolean activo);

    /**
     * Obtener tenants con estadísticas básicas
     */
    @Query("SELECT t FROM Tenant t WHERE t.activo = true ORDER BY t.fechaCreacion DESC")
    List<Tenant> findActivosOrdenadosPorFecha();
}