package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, String> {

    // MÉTODO EXISTENTE - NO TOCAR
    @Query("SELECT c FROM Cita c WHERE c.tenant.id = :tenantId AND c.fechaHora BETWEEN :inicio AND :fin")
    List<Cita> findByTenantIdAndFechaHoraBetween(@Param("tenantId") String tenantId,
                                                 @Param("inicio") LocalDateTime inicio,
                                                 @Param("fin") LocalDateTime fin);

    // MÉTODO EXISTENTE - NO TOCAR
    List<Cita> findByClienteTelefonoAndEstado(String telefono, EstadoCita estado);

    // NUEVO MÉTODO: Solo agregar COUNT para StatsService
    @Query("SELECT COUNT(c) FROM Cita c WHERE c.tenant.id = :tenantId AND c.fechaHora BETWEEN :inicio AND :fin")
    long countByTenantIdAndFechaHoraBetween(@Param("tenantId") String tenantId,
                                            @Param("inicio") LocalDateTime inicio,
                                            @Param("fin") LocalDateTime fin);
}