package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.repository.CitaRepository;
import com.peluqueria.recepcionista_virtual.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class StatsService {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    public Map<String, Object> getStatsByTenant(String tenantId) {
        Map<String, Object> stats = new HashMap<>();

        // Citas de hoy
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59);

        if (tenantId != null) {
            stats.put("citasHoy", citaRepository.findByTenantIdAndFechaHoraBetween(
                    tenantId, startOfDay, endOfDay
            ).size());
        } else {
            stats.put("citasHoy", 0);
        }

        // Ingresos del mes (placeholder)
        stats.put("ingresosMes", 0);

        // Clientes nuevos (placeholder)
        stats.put("clientesNuevos", 0);

        // Tasa de cancelación (placeholder)
        stats.put("tasaCancelacion", 0);

        return stats;
    }
}