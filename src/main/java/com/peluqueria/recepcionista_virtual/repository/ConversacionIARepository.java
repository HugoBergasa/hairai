package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.ConversacionIA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConversacionIARepository extends JpaRepository<ConversacionIA, Long> {

    List<ConversacionIA> findByTenantId(String tenantId);

    List<ConversacionIA> findByTenantIdAndCallSid(String tenantId, String callSid);

    List<ConversacionIA> findByTenantIdAndCanal(String tenantId, ConversacionIA.CanalComunicacion canal);

    @Query("SELECT c FROM ConversacionIA c WHERE c.tenantId = :tenantId " +
            "AND c.timestamp BETWEEN :fechaInicio AND :fechaFin")
    List<ConversacionIA> findByTenantIdAndPeriodo(
            @Param("tenantId") String tenantId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    @Query("SELECT c.intencionDetectada, COUNT(c) FROM ConversacionIA c " +
            "WHERE c.tenantId = :tenantId GROUP BY c.intencionDetectada")
    List<Object[]> contarIntencionesPorTenant(@Param("tenantId") String tenantId);
}