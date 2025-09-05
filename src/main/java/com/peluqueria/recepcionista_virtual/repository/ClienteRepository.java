package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.Cliente;
import com.peluqueria.recepcionista_virtual.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, String> {
    @Query("SELECT c FROM Cliente c WHERE c.telefono = :telefono AND c.tenant.id = :tenantId")
    Optional<Cliente> findByTelefonoAndTenantId(@Param("telefono") String telefono,
                                                @Param("tenantId") String tenantId);
}