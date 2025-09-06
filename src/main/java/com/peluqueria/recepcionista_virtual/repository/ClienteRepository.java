package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.Cliente;
import com.peluqueria.recepcionista_virtual.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, String> {

    // ✅ MÉTODO SEGURO - Maneja duplicados devolviendo el más reciente
    @Query("SELECT c FROM Cliente c WHERE c.telefono = :telefono AND c.tenant.id = :tenantId ORDER BY c.fechaRegistro DESC")
    Optional<Cliente> findByTelefonoAndTenantId(@Param("telefono") String telefono,
                                                @Param("tenantId") String tenantId);

    // ✅ MÉTODO AUXILIAR - Para detectar duplicados (opcional)
    @Query("SELECT c FROM Cliente c WHERE c.telefono = :telefono AND c.tenant.id = :tenantId ORDER BY c.fechaRegistro DESC")
    List<Cliente> findAllByTelefonoAndTenantId(@Param("telefono") String telefono,
                                               @Param("tenantId") String tenantId);

    @Query("SELECT c FROM Cliente c WHERE c.tenant.id = :tenantId")
    List<Cliente> findByTenantId(@Param("tenantId") String tenantId);
}