package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, String> {
    List<Cita> findByTenantIdAndFechaHoraBetween(
            String tenantId,
            LocalDateTime inicio,
            LocalDateTime fin
    );

    List<Cita> findByClienteTelefonoAndEstado(
            String telefono,
            EstadoCita estado
    );
}
