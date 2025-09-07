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
public interface ConversacionIARepository extends JpaRepository<ConversacionIA, String> {  // CORREGIDO: String ID

    // ===== MÉTODOS BÁSICOS MULTITENANT =====

    /**
     * Obtener todas las conversaciones de un tenant
     */
    List<ConversacionIA> findByTenantId(String tenantId);

    /**
     * Obtener conversaciones de un tenant ordenadas por timestamp (más recientes primero)
     */
    List<ConversacionIA> findByTenantIdOrderByTimestampDesc(String tenantId);

    /**
     * Obtener conversaciones de un tenant con paginación
     */
    Page<ConversacionIA> findByTenantIdOrderByTimestampDesc(String tenantId, Pageable pageable);

    /**
     * Buscar conversación por ID y TenantId (seguridad multitenant) - CORREGIDO: String ID
     */
    ConversacionIA findByIdAndTenantId(String id, String tenantId);

    /**
     * Verificar si existe conversación por ID y TenantId - CORREGIDO: String ID
     */
    boolean existsByIdAndTenantId(String id, String tenantId);

    // ===== MÉTODOS POR CALLSID =====

    /**
     * Buscar conversaciones por CallSid y tenant
     */
    List<ConversacionIA> findByTenantIdAndCallSid(String tenantId, String callSid);

    /**
     * Buscar conversaciones por CallSid con ordenamiento temporal
     */
    List<ConversacionIA> findByCallSidAndTenantIdOrderByTimestampAsc(String callSid, String tenantId);

    /**
     * Contar conversaciones por CallSid
     */
    Long countByTenantIdAndCallSid(String tenantId, String callSid);

    // ===== MÉTODOS POR CANAL =====

    /**
     * Buscar conversaciones por canal
     */
    List<ConversacionIA> findByTenantIdAndCanalOrderByTimestampDesc(String tenantId, ConversacionIA.CanalComunicacion canal);

    /**
     * Contar conversaciones por canal
     */
    Long countByTenantIdAndCanal(String tenantId, ConversacionIA.CanalComunicacion canal);

    // ===== MÉTODOS POR RANGO DE FECHAS =====

    /**
     * Buscar conversaciones en un rango de fechas
     */
    List<ConversacionIA> findByTenantIdAndTimestampBetweenOrderByTimestampDesc(
            String tenantId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );

    /**
     * Query personalizada para conversaciones por período
     */
    @Query("SELECT c FROM ConversacionIA c WHERE c.tenantId = :tenantId " +
            "AND c.timestamp BETWEEN :fechaInicio AND :fechaFin " +
            "ORDER BY c.timestamp DESC")
    List<ConversacionIA> findByTenantIdAndPeriodo(
            @Param("tenantId") String tenantId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    // ===== MÉTODOS POR INTENCIÓN Y ACCIÓN =====

    /**
     * Buscar conversaciones por intención detectada
     */
    List<ConversacionIA> findByTenantIdAndIntencionDetectadaOrderByTimestampDesc(
            String tenantId,
            String intencionDetectada
    );

    /**
     * Buscar conversaciones por acción ejecutada
     */
    List<ConversacionIA> findByTenantIdAndAccionEjecutadaOrderByTimestampDesc(
            String tenantId,
            String accionEjecutada
    );

    /**
     * Query para contar intenciones por tenant
     */
    @Query("SELECT c.intencionDetectada, COUNT(c) FROM ConversacionIA c " +
            "WHERE c.tenantId = :tenantId GROUP BY c.intencionDetectada")
    List<Object[]> contarIntencionesPorTenant(@Param("tenantId") String tenantId);

    // ===== MÉTODOS DE CONTEO =====

    /**
     * Contar total de conversaciones por tenant
     */
    Long countByTenantId(String tenantId);

    /**
     * Contar conversaciones exitosas/fallidas
     */
    Long countByTenantIdAndExitoso(String tenantId, Boolean exitoso);

    /**
     * Contar conversaciones exitosas en un período
     */
    Long countByTenantIdAndExitosoAndTimestampBetween(
            String tenantId,
            Boolean exitoso,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );

    // ===== MÉTODOS DE BÚSQUEDA AVANZADA =====

    /**
     * Buscar conversaciones por texto en mensaje de usuario
     */
    List<ConversacionIA> findByTenantIdAndMensajeUsuarioContainingIgnoreCaseOrderByTimestampDesc(
            String tenantId,
            String texto
    );

    /**
     * Obtener las 10 conversaciones más recientes
     */
    List<ConversacionIA> findFirst10ByTenantIdOrderByTimestampDesc(String tenantId);

    // ===== MÉTODOS DE ESTADÍSTICAS =====

    /**
     * Sumar tokens utilizados por tenant
     */
    @Query("SELECT COALESCE(SUM(c.tokensUsados), 0) FROM ConversacionIA c " +
            "WHERE c.tenantId = :tenantId AND c.tokensUsados IS NOT NULL")
    Long sumTokensUsadosByTenantId(@Param("tenantId") String tenantId);

    /**
     * Calcular duración promedio de conversaciones
     */
    @Query("SELECT AVG(c.duracionMs) FROM ConversacionIA c " +
            "WHERE c.tenantId = :tenantId AND c.duracionMs IS NOT NULL")
    Double avgDuracionByTenantId(@Param("tenantId") String tenantId);

    /**
     * Estadísticas por canal y período
     */
    @Query("SELECT c.canal, COUNT(c), AVG(c.duracionMs), SUM(c.tokensUsados) " +
            "FROM ConversacionIA c WHERE c.tenantId = :tenantId " +
            "AND c.timestamp BETWEEN :fechaInicio AND :fechaFin " +
            "GROUP BY c.canal")
    List<Object[]> estadisticasPorCanalYPeriodo(
            @Param("tenantId") String tenantId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    // ===== QUERIES PARA DASHBOARD Y ANALÍTICAS =====

    /**
     * Obtener conversaciones del día actual
     */
    @Query("SELECT c FROM ConversacionIA c WHERE c.tenantId = :tenantId " +
            "AND DATE(c.timestamp) = CURRENT_DATE ORDER BY c.timestamp DESC")
    List<ConversacionIA> findConversacionesDelDia(@Param("tenantId") String tenantId);

    /**
     * Obtener conversaciones de la semana actual
     */
    @Query("SELECT c FROM ConversacionIA c WHERE c.tenantId = :tenantId " +
            "AND c.timestamp >= :inicioSemana ORDER BY c.timestamp DESC")
    List<ConversacionIA> findConversacionesDeLaSemana(
            @Param("tenantId") String tenantId,
            @Param("inicioSemana") LocalDateTime inicioSemana
    );

    /**
     * Obtener estadísticas por estado para un período
     */
    @Query("SELECT c.estado, COUNT(c) FROM ConversacionIA c " +
            "WHERE c.tenantId = :tenantId " +
            "AND c.timestamp BETWEEN :fechaInicio AND :fechaFin " +
            "GROUP BY c.estado")
    List<Object[]> getEstadisticasPorEstado(
            @Param("tenantId") String tenantId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Top intenciones detectadas en un período
     */
    @Query("SELECT c.intencionDetectada, COUNT(c) FROM ConversacionIA c " +
            "WHERE c.tenantId = :tenantId " +
            "AND c.timestamp BETWEEN :fechaInicio AND :fechaFin " +
            "AND c.intencionDetectada IS NOT NULL " +
            "GROUP BY c.intencionDetectada ORDER BY COUNT(c) DESC")
    List<Object[]> getTopIntencionesPorPeriodo(
            @Param("tenantId") String tenantId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Conversaciones con errores para análisis
     */
    List<ConversacionIA> findByTenantIdAndExitosoFalseOrderByTimestampDesc(String tenantId);

    /**
     * Conversaciones por modelo de IA utilizado
     */
    List<ConversacionIA> findByTenantIdAndModeloIaOrderByTimestampDesc(String tenantId, String modeloIa);

    /**
     * Estadísticas de tokens por modelo de IA
     */
    @Query("SELECT c.modeloIa, COUNT(c), SUM(c.tokensUsados), AVG(c.tokensUsados) " +
            "FROM ConversacionIA c WHERE c.tenantId = :tenantId " +
            "AND c.tokensUsados IS NOT NULL " +
            "GROUP BY c.modeloIa")
    List<Object[]> getEstadisticasTokensPorModelo(@Param("tenantId") String tenantId);
}