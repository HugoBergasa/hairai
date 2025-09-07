package com.peluqueria.recepcionista_virtual.repository;

import com.peluqueria.recepcionista_virtual.model.ConversacionIA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConversacionIARepository extends JpaRepository<ConversacionIA, Long> {

    // ===== MÉTODOS EXISTENTES =====
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

    // ===== MÉTODOS AGREGADOS BÁSICOS (SIN QUERIES PROBLEMÁTICAS) =====

    // Métodos con ordenamiento
    List<ConversacionIA> findByTenantIdOrderByTimestampDesc(String tenantId);

    Page<ConversacionIA> findByTenantIdOrderByTimestampDesc(String tenantId, Pageable pageable);

    // CallSid con ordenamiento
    List<ConversacionIA> findByCallSidAndTenantIdOrderByTimestampAsc(String callSid, String tenantId);

    // Buscar por ID y TenantId (seguridad multitenant)
    ConversacionIA findByIdAndTenantId(Long id, String tenantId);

    // Canal con ordenamiento
    List<ConversacionIA> findByTenantIdAndCanalOrderByTimestampDesc(String tenantId, ConversacionIA.CanalComunicacion canal);

    // Rango de fechas con ordenamiento
    List<ConversacionIA> findByTenantIdAndTimestampBetweenOrderByTimestampDesc(
            String tenantId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );

    // ===== MÉTODOS DE CONTEO BÁSICOS =====

    Long countByTenantId(String tenantId);

    Long countByTenantIdAndExitoso(String tenantId, Boolean exitoso);

    Long countByTenantIdAndCanal(String tenantId, ConversacionIA.CanalComunicacion canal);

    // ===== MÉTODOS ADICIONALES SEGUROS =====

    List<ConversacionIA> findByTenantIdAndIntencionDetectadaOrderByTimestampDesc(
            String tenantId,
            String intencionDetectada
    );

    List<ConversacionIA> findByTenantIdAndAccionEjecutadaOrderByTimestampDesc(
            String tenantId,
            String accionEjecutada
    );

    List<ConversacionIA> findFirst10ByTenantIdOrderByTimestampDesc(String tenantId);

    Long countByTenantIdAndExitosoAndTimestampBetween(
            String tenantId,
            Boolean exitoso,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );



    @Query("SELECT COALESCE(SUM(c.tokensUsados), 0) FROM ConversacionIA c " +
            "WHERE c.tenantId = :tenantId AND c.tokensUsados IS NOT NULL")
    Long sumTokensUsadosByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT AVG(c.duracionMs) FROM ConversacionIA c " +
            "WHERE c.tenantId = :tenantId AND c.duracionMs IS NOT NULL")
    Double avgDuracionByTenantId(@Param("tenantId") String tenantId);

    Long countByTenantIdAndCallSid(String tenantId, String callSid);

    boolean existsByIdAndTenantId(Long id, String tenantId);

    List<ConversacionIA> findByTenantIdAndMensajeUsuarioContainingIgnoreCaseOrderByTimestampDesc(
            String tenantId,
            String texto
    );

    @Query("SELECT c.canal, COUNT(c), AVG(c.duracionMs), SUM(c.tokensUsados) " +
            "FROM ConversacionIA c WHERE c.tenantId = :tenantId " +
            "AND c.timestamp BETWEEN :fechaInicio AND :fechaFin " +
            "GROUP BY c.canal")
    List<Object[]> estadisticasPorCanalYPeriodo(
            @Param("tenantId") String tenantId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );
}