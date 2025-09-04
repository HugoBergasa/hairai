package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.*;
import com.peluqueria.recepcionista_virtual.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class CitaService {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private TwilioAIService twilioService;

    public Cita crearCita(String tenantId, String telefonoCliente,
                          String servicioId, LocalDateTime fechaHora) {

        // Buscar o crear cliente
        Cliente cliente = clienteRepository
                .findByTelefonoAndTenantId(telefonoCliente, tenantId)
                .orElseGet(() -> {
                    Cliente nuevo = new Cliente();
                    nuevo.setTelefono(telefonoCliente);
                    // Asociar tenant
                    return clienteRepository.save(nuevo);
                });

        // Crear cita
        Cita cita = new Cita();
        cita.setCliente(cliente);
        cita.setFechaHora(fechaHora);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setOrigen(OrigenCita.TELEFONO);

        Cita citaGuardada = citaRepository.save(cita);

        // Enviar SMS de confirmación
        String mensaje = String.format(
                "✅ Cita confirmada para el %s a las %s. " +
                        "Para cancelar responda CANCELAR. Peluquería Style",
                fechaHora.toLocalDate(),
                fechaHora.toLocalTime()
        );

        twilioService.enviarSMS(telefonoCliente, mensaje);

        return citaGuardada;
    }

    public List<Cita> obtenerCitasDelDia(String tenantId) {
        LocalDateTime inicio = LocalDateTime.now().withHour(0).withMinute(0);
        LocalDateTime fin = LocalDateTime.now().withHour(23).withMinute(59);

        return citaRepository.findByTenantIdAndFechaHoraBetween(
                tenantId, inicio, fin
        );
    }

    public void cancelarCita(String citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        cita.setEstado(EstadoCita.CANCELADA);
        citaRepository.save(cita);

        // Enviar SMS de cancelación
        String mensaje = "❌ Su cita ha sido cancelada. " +
                "Para reagendar llame al 900-123-456";

        twilioService.enviarSMS(cita.getCliente().getTelefono(), mensaje);
    }
}