package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.ConversacionIA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversacionIARepository extends JpaRepository<ConversacionIA, Long> {

    List<ConversacionIA> findByTenantId(String tenantId);

    List<ConversacionIA> findByCallSidOrderByTimestamp(String callSid);

    List<ConversacionIA> findByTenantIdAndTimestampBetween(
            String tenantId, Instant start, Instant end
    );

    @Query("SELECT c FROM ConversacionIA c WHERE c.tenantId = :tenantId " +
            "AND c.intencion = :intencion ORDER BY c.timestamp DESC")
    List<ConversacionIA> findByTenantIdAndIntencion(
            @Param("tenantId") String tenantId,
            @Param("intencion") String intencion
    );

    @Query("SELECT COUNT(c) FROM ConversacionIA c WHERE c.tenantId = :tenantId " +
            "AND c.timestamp >= :desde")
    Long countConversacionesDesde(
            @Param("tenantId") String tenantId,
            @Param("desde") Instant desde
    );

    @Query("SELECT SUM(c.tokensUsados) FROM ConversacionIA c " +
            "WHERE c.tenantId = :tenantId AND c.timestamp >= :desde")
    Long sumTokensUsados(
            @Param("tenantId") String tenantId,
            @Param("desde") Instant desde
    );

    @Query(value = "SELECT intencion, COUNT(*) as total FROM conversaciones_ia " +
            "WHERE tenant_id = :tenantId GROUP BY intencion",
            nativeQuery = true)
    List<Object[]> estadisticasIntencionPorTenant(@Param("tenantId") String tenantId);
}