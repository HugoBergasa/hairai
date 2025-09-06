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

    // ✅ MÉTODO PRINCIPAL para crear cita desde IA - CON LOGGING DETALLADO
    public Cita crearCita(String tenantId, String telefono, DatosCita datos) {
        try {
            System.out.println("🔍 [DEBUG] === INICIANDO CREACIÓN DE CITA ===");
            System.out.println("🔍 [DEBUG] TenantId: " + tenantId);
            System.out.println("🔍 [DEBUG] Teléfono: " + telefono);
            System.out.println("🔍 [DEBUG] Datos: " + datos);

            // 1. Obtener el Tenant
            System.out.println("🔍 [DEBUG] PASO 1: Buscando tenant...");
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));
            System.out.println("✅ [DEBUG] Tenant encontrado: " + tenant.getNombrePeluqueria());

            // 2. Buscar o crear cliente - AQUÍ PUEDE ESTAR EL PROBLEMA
            System.out.println("🔍 [DEBUG] PASO 2: Buscando cliente...");
            System.out.println("🔍 [DEBUG] Ejecutando: clienteRepository.findByTelefonoAndTenantId(" + telefono + ", " + tenantId + ")");

            Optional<Cliente> clienteExistente;
            try {
                clienteExistente = clienteRepository.findByTelefonoAndTenantId(telefono, tenantId);
                System.out.println("✅ [DEBUG] Consulta cliente ejecutada - Presente: " + clienteExistente.isPresent());
            } catch (Exception e) {
                System.err.println("❌ [ERROR] Error en consulta cliente: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            Cliente cliente;
            if (clienteExistente.isPresent()) {
                cliente = clienteExistente.get();
                System.out.println("✅ [DEBUG] Cliente existente: " + cliente.getNombre() + " (ID: " + cliente.getId() + ")");
            } else {
                System.out.println("🔍 [DEBUG] Cliente no existe, creando nuevo...");
                cliente = new Cliente();
                cliente.setTenant(tenant);
                cliente.setTelefono(telefono);
                cliente.setNombre(datos.getNombreCliente() != null ? datos.getNombreCliente() : "Cliente");

                try {
                    cliente = clienteRepository.save(cliente);
                    System.out.println("✅ [DEBUG] Nuevo cliente creado: " + cliente.getNombre() + " (ID: " + cliente.getId() + ")");
                } catch (Exception e) {
                    System.err.println("❌ [ERROR] Error guardando cliente: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }

            // 3. Buscar servicio
            System.out.println("🔍 [DEBUG] PASO 3: Buscando servicio...");
            Servicio servicio = null;
            if (datos.getServicio() != null) {
                System.out.println("🔍 [DEBUG] Ejecutando: servicioRepository.findByNombreContainingIgnoreCaseAndTenantId(" + datos.getServicio() + ", " + tenantId + ")");
                try {
                    List<Servicio> servicios = servicioRepository.findByNombreContainingIgnoreCaseAndTenantId(datos.getServicio(), tenantId);
                    System.out.println("✅ [DEBUG] Servicios encontrados: " + servicios.size());
                    if (!servicios.isEmpty()) {
                        servicio = servicios.get(0);
                        System.out.println("✅ [DEBUG] Servicio seleccionado: " + servicio.getNombre() + " (ID: " + servicio.getId() + ")");
                    }
                } catch (Exception e) {
                    System.err.println("❌ [ERROR] Error buscando servicio: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }

            // 4. Parsear fecha y hora
            System.out.println("🔍 [DEBUG] PASO 4: Parseando fecha/hora...");
            LocalDateTime fechaHora = parsearFechaHora(datos.getFecha(), datos.getHora());
            System.out.println("✅ [DEBUG] Fecha/hora: " + fechaHora);

            // 5. Buscar empleado disponible
            System.out.println("🔍 [DEBUG] PASO 5: Buscando empleado...");
            Empleado empleado = buscarEmpleadoDisponible(tenantId, fechaHora);
            if (empleado != null) {
                System.out.println("✅ [DEBUG] Empleado asignado: " + empleado.getNombre() + " (ID: " + empleado.getId() + ")");
            } else {
                System.out.println("⚠️ [DEBUG] No se encontró empleado disponible");
            }

            // 6. Crear la cita
            System.out.println("🔍 [DEBUG] PASO 6: Creando objeto cita...");
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
                System.out.println("✅ [DEBUG] Precio asignado: €" + servicio.getPrecio());
            }

            System.out.println("🔍 [DEBUG] PASO 7: Guardando cita en BD...");
            try {
                Cita citaGuardada = citaRepository.save(cita);
                System.out.println("✅ [DEBUG] Cita guardada exitosamente (ID: " + citaGuardada.getId() + ")");

                // 7. ✅ ENVIAR SMS DE CONFIRMACIÓN PERSONALIZADO POR TENANT
                System.out.println("🔍 [DEBUG] PASO 8: Enviando SMS...");
                enviarConfirmacionPersonalizada(citaGuardada);

                return citaGuardada;

            } catch (Exception e) {
                System.err.println("❌ [ERROR] Error guardando cita: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

        } catch (Exception e) {
            System.err.println("❌ [ERROR] Error general en crearCita: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error creando cita: " + e.getMessage(), e);
        }
    }

    // MÉTODO EMPLEADO CON LOGGING DETALLADO
    private Empleado buscarEmpleadoDisponible(String tenantId, LocalDateTime fechaHora) {
        try {
            System.out.println("🔍 [DEBUG] Ejecutando: empleadoRepository.findByTenantIdAndActivoTrue(" + tenantId + ")");
            List<Empleado> empleados = empleadoRepository.findByTenantIdAndActivoTrue(tenantId);
            System.out.println("✅ [DEBUG] Empleados encontrados: " + empleados.size());

            for (int i = 0; i < empleados.size(); i++) {
                Empleado emp = empleados.get(i);
                System.out.println("🔍 [DEBUG] Empleado " + (i+1) + ": " + emp.getNombre() + " (ID: " + emp.getId() + ", Activo: " + emp.getActivo() + ")");
            }

            if (!empleados.isEmpty()) {
                return empleados.get(0);
            }
        } catch (Exception e) {
            System.err.println("❌ [ERROR] Error en buscarEmpleadoDisponible: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
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
        enviarConfirmacionPersonalizada(citaGuardada);

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
                        fechaParsed = LocalDate.now().plusDays(1);
                    }
                }
            }

            if (hora != null && !hora.isEmpty()) {
                try {
                    horaParsed = LocalTime.parse(hora);
                } catch (Exception e) {
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

    private void enviarConfirmacionPersonalizada(Cita cita) {
        try {
            Tenant tenant = cita.getTenant();
            DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("HH:mm");

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
            System.err.println("Error enviando SMS de confirmación: " + e.getMessage());
        }
    }

    public void cancelarCita(String citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        cita.setEstado(EstadoCita.CANCELADA);
        citaRepository.save(cita);

        Tenant tenant = cita.getTenant();
        String mensaje = String.format(
                "❌ Su cita en %s ha sido cancelada. Para reagendar%s visite nuestras instalaciones.",
                tenant.getNombrePeluqueria(),
                tenant.getTelefono() != null ? " llame al " + tenant.getTelefono() + " o" : ""
        );

        twilioService.enviarSMS(cita.getCliente().getTelefono(), mensaje);
    }

    public void enviarRecordatorio(Cita cita) {
        try {
            if (cita.getRecordatorioEnviado()) {
                return;
            }

            Tenant tenant = cita.getTenant();
            DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("HH:mm");

            String mensaje = String.format(
                    "📅 Recordatorio: Tiene cita en %s mañana %s a las %s.%s %s",
                    tenant.getNombrePeluqueria(),
                    cita.getFechaHora().format(fechaFormatter),
                    cita.getFechaHora().format(horaFormatter),
                    cita.getServicio() != null ? " Servicio: " + cita.getServicio().getNombre() + "." : "",
                    tenant.getTelefono() != null ? "Info: " + tenant.getTelefono() : ""
            );

            twilioService.enviarSMS(cita.getCliente().getTelefono(), mensaje);

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