package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, String> {
    Optional<Cliente> findByTelefonoAndTenantId(String telefono, String tenantId);
}
