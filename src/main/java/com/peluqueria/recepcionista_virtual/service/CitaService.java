package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.*;
import com.peluqueria.recepcionista_virtual.repository.*;
import com.peluqueria.recepcionista_virtual.dto.DatosCita;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TwilioAIService twilioService;

    // ✅ MÉTODO PRINCIPAL para crear cita desde IA - MENSAJES DINÁMICOS
    public Cita crearCita(String tenantId, String telefono, DatosCita datos) {
        try {
            // 1. Obtener el Tenant
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

            // 2. Buscar o crear cliente
            Cliente cliente = clienteRepository
                    .findByTelefonoAndTenantId(telefono, tenantId)
                    .orElseGet(() -> {
                        Cliente nuevo = new Cliente();
                        nuevo.setTenant(tenant);
                        nuevo.setTelefono(telefono);
                        nuevo.setNombre(datos.getNombreCliente() != null ?
                                datos.getNombreCliente() : "Cliente");
                        return clienteRepository.save(nuevo);
                    });

            // 3. Buscar servicio
            Servicio servicio = null;
            if (datos.getServicio() != null) {
                List<Servicio> servicios = servicioRepository
                        .findByNombreContainingIgnoreCaseAndTenantId(
                                datos.getServicio(), tenantId);
                if (!servicios.isEmpty()) {
                    servicio = servicios.get(0);
                }
            }

            // 4. Parsear fecha y hora
            LocalDateTime fechaHora = parsearFechaHora(datos.getFecha(), datos.getHora());

            // 5. Buscar empleado disponible
            Empleado empleado = buscarEmpleadoDisponible(tenantId, fechaHora);

            // 6. Crear la cita
            Cita cita = new Cita();
            cita.setTenant(tenant);
            cita.setCliente(cliente);
            cita.setServicio(servicio);
            cita.setEmpleado(empleado);
            cita.setFechaHora(fechaHora);
            cita.setEstado(EstadoCita.CONFIRMADA);
            cita.setOrigen(OrigenCita.TELEFONO);
            cita.setNotas("Cita creada por IA - Tel: " + telefono);

            if (servicio != null) {
                cita.setDuracionMinutos(servicio.getDuracionMinutos());
                cita.setPrecio(servicio.getPrecio());
            }

            Cita citaGuardada = citaRepository.save(cita);

            // 7. ✅ ENVIAR SMS DE CONFIRMACIÓN PERSONALIZADO POR TENANT
            enviarConfirmacionPersonalizada(citaGuardada);

            return citaGuardada;

        } catch (Exception e) {
            throw new RuntimeException("Error creando cita: " + e.getMessage(), e);
        }
    }

    // MÉTODO LEGACY (mantener para compatibilidad)
    public Cita crearCita(String tenantId, String telefonoCliente,
                          String servicioId, LocalDateTime fechaHora) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        Cliente cliente = clienteRepository
                .findByTelefonoAndTenantId(telefonoCliente, tenantId)
                .orElseGet(() -> {
                    Cliente nuevo = new Cliente();
                    nuevo.setTenant(tenant);
                    nuevo.setTelefono(telefonoCliente);
                    return clienteRepository.save(nuevo);
                });

        Cita cita = new Cita();
        cita.setTenant(tenant);
        cita.setCliente(cliente);
        cita.setFechaHora(fechaHora);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setOrigen(OrigenCita.TELEFONO);

        Cita citaGuardada = citaRepository.save(cita);
        enviarConfirmacionPersonalizada(citaGuardada); // ✅ Usar método personalizado

        return citaGuardada;
    }

    private LocalDateTime parsearFechaHora(String fecha, String hora) {
        try {
            LocalDate fechaParsed = LocalDate.now();
            LocalTime horaParsed = LocalTime.of(10, 0);

            if (fecha != null && !fecha.isEmpty()) {
                if (fecha.toLowerCase().contains("hoy")) {
                    fechaParsed = LocalDate.now();
                } else if (fecha.toLowerCase().contains("mañana")) {
                    fechaParsed = LocalDate.now().plusDays(1);
                } else {
                    try {
                        fechaParsed = LocalDate.parse(fecha);
                    } catch (Exception e) {
                        // Usar fecha de mañana por defecto
                        fechaParsed = LocalDate.now().plusDays(1);
                    }
                }
            }

            if (hora != null && !hora.isEmpty()) {
                try {
                    horaParsed = LocalTime.parse(hora);
                } catch (Exception e) {
                    // Intentar parsear "10" como "10:00"
                    if (hora.matches("\\d{1,2}")) {
                        int h = Integer.parseInt(hora);
                        if (h >= 0 && h <= 23) {
                            horaParsed = LocalTime.of(h, 0);
                        }
                    }
                }
            }

            return LocalDateTime.of(fechaParsed, horaParsed);

        } catch (Exception e) {
            return LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        }
    }

    private Empleado buscarEmpleadoDisponible(String tenantId, LocalDateTime fechaHora) {
        try {
            List<Empleado> empleados = empleadoRepository.findByTenantIdAndActivoTrue(tenantId);
            if (!empleados.isEmpty()) {
                // Por ahora, asignar el primer empleado disponible
                return empleados.get(0);
            }
        } catch (Exception e) {
            // Log pero continuar sin empleado
        }
        return null;
    }

    /**
     * ✅ ENVIAR CONFIRMACIÓN PERSONALIZADA POR TENANT - NO MÁS HARDCODING
     */
    private void enviarConfirmacionPersonalizada(Cita cita) {
        try {
            Tenant tenant = cita.getTenant();
            DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("HH:mm");

            // ✅ MENSAJE PERSONALIZADO CON DATOS REALES DEL TENANT
            String mensaje = String.format(
                    "✅ Cita confirmada en %s para el %s a las %s.%s Para cancelar responda CANCELAR. %s",
                    tenant.getNombrePeluqueria(),
                    cita.getFechaHora().format(fechaFormatter),
                    cita.getFechaHora().format(horaFormatter),
                    cita.getServicio() != null ? " Servicio: " + cita.getServicio().getNombre() + "." : "",
                    tenant.getTelefono() != null ? "Info: " + tenant.getTelefono() : ""
            );

            twilioService.enviarSMS(cita.getCliente().getTelefono(), mensaje);

        } catch (Exception e) {
            // Log pero no fallar la creación de cita
            System.err.println("Error enviando SMS de confirmación: " + e.getMessage());
        }
    }

    /**
     * ✅ CANCELAR CITA CON MENSAJE PERSONALIZADO POR TENANT
     */
    public void cancelarCita(String citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        cita.setEstado(EstadoCita.CANCELADA);
        citaRepository.save(cita);

        // ✅ MENSAJE DE CANCELACIÓN PERSONALIZADO
        Tenant tenant = cita.getTenant();
        String mensaje = String.format(
                "❌ Su cita en %s ha sido cancelada. Para reagendar%s visite nuestras instalaciones.",
                tenant.getNombrePeluqueria(),
                tenant.getTelefono() != null ? " llame al " + tenant.getTelefono() + " o" : ""
        );

        twilioService.enviarSMS(cita.getCliente().getTelefono(), mensaje);
    }

    /**
     * ✅ ENVIAR RECORDATORIO PERSONALIZADO POR TENANT
     */
    public void enviarRecordatorio(Cita cita) {
        try {
            if (cita.getRecordatorioEnviado()) {
                return; // Ya se envió
            }

            Tenant tenant = cita.getTenant();
            DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("HH:mm");

            String mensaje = String.format(
                    "🔔 Recordatorio: Tiene cita en %s mañana %s a las %s.%s %s",
                    tenant.getNombrePeluqueria(),
                    cita.getFechaHora().format(fechaFormatter),
                    cita.getFechaHora().format(horaFormatter),
                    cita.getServicio() != null ? " Servicio: " + cita.getServicio().getNombre() + "." : "",
                    tenant.getTelefono() != null ? "Info: " + tenant.getTelefono() : ""
            );

            twilioService.enviarSMS(cita.getCliente().getTelefono(), mensaje);

            // Marcar recordatorio como enviado
            cita.setRecordatorioEnviado(true);
            citaRepository.save(cita);

        } catch (Exception e) {
            System.err.println("Error enviando recordatorio: " + e.getMessage());
        }
    }

    public List<Cita> obtenerCitasDelDia(String tenantId) {
        LocalDateTime inicio = LocalDateTime.now().withHour(0).withMinute(0);
        LocalDateTime fin = LocalDateTime.now().withHour(23).withMinute(59);

        return citaRepository.findByTenantIdAndFechaHoraBetween(
                tenantId, inicio, fin
        );
    }
}