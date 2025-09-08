package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.*;
import com.peluqueria.recepcionista_virtual.repository.*;
import com.peluqueria.recepcionista_virtual.dto.DatosCita;
import com.peluqueria.recepcionista_virtual.dto.CitaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private OpenAIService openAIService;

    // ========================================================================================
    // 🤖 MÉTODOS IA EXISTENTES - MANTENIDOS COMPLETAMENTE
    // ========================================================================================

    /**
     * 🔄 MÉTODO CREAR CITA DESDE IA ACTUALIZADO - CON VALIDACIÓN DE HORARIOS
     */
    public Cita crearCita(String tenantId, String telefono, DatosCita datos) {
        try {
            // Parsear fecha y hora
            LocalDateTime fechaHora = parsearFechaHora(datos.getFecha(), datos.getHora());

            // 🕐 VALIDACIÓN CRÍTICA: Verificar horario de trabajo ANTES de crear
            validarHorarioTrabajo(tenantId, fechaHora);

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

            // 5. 🤖 IA AUTOMÁTICA: Buscar empleado disponible inteligentemente
            Empleado empleado = buscarEmpleadoDisponibleConIA(tenantId, fechaHora, servicio);

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

    /**
     * MÉTODO LEGACY (mantener para compatibilidad)
     */
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

    // ========================================================================================
    // 🎯 NUEVOS MÉTODOS DTO PARA DASHBOARD - CON IA INTEGRADA
    // ========================================================================================

    /**
     * 📋 OBTENER CITAS POR TENANT
     */
    public List<CitaDTO> getCitasByTenantId(String tenantId) {
        List<Cita> citas = citaRepository.findByTenantIdOrderByFechaHoraDesc(tenantId);
        return citas.stream()
                .map(CitaDTO::fromCita)
                .collect(Collectors.toList());
    }

    /**
     * 📅 OBTENER CITAS POR FECHA
     */
    public List<CitaDTO> getCitasByTenantIdAndFecha(String tenantId, String fecha) {
        try {
            LocalDate fechaParsed = LocalDate.parse(fecha);
            LocalDateTime inicio = fechaParsed.atStartOfDay();
            LocalDateTime fin = fechaParsed.atTime(23, 59, 59);

            List<Cita> citas = citaRepository.findByTenantIdAndFechaHoraBetween(tenantId, inicio, fin);
            return citas.stream()
                    .map(CitaDTO::fromCita)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return getCitasByTenantId(tenantId);
        }
    }

    /**
     * 🔄 OBTENER CITAS POR ESTADO
     */
    public List<CitaDTO> getCitasByTenantIdAndEstado(String tenantId, String estado) {
        try {
            EstadoCita estadoCita = EstadoCita.valueOf(estado.toUpperCase());
            List<Cita> citas = citaRepository.findByTenantIdAndEstado(tenantId, estadoCita);
            return citas.stream()
                    .map(CitaDTO::fromCita)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return getCitasByTenantId(tenantId);
        }
    }

    /**
     * 📅 OBTENER CITAS DE HOY
     */
    public List<CitaDTO> getCitasHoyByTenantId(String tenantId) {
        LocalDateTime inicio = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime fin = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        List<Cita> citas = citaRepository.findByTenantIdAndFechaHoraBetween(tenantId, inicio, fin);
        return citas.stream()
                .map(CitaDTO::fromCita)
                .collect(Collectors.toList());
    }

    /**
     * 🔄 MÉTODO CREATECITA ACTUALIZADO - CON VALIDACIÓN DE HORARIOS
     */
    public CitaDTO createCita(String tenantId, CitaDTO citaDTO) {
        try {
            // 🕐 VALIDACIÓN CRÍTICA: Verificar horario de trabajo ANTES de crear
            validarHorarioTrabajo(tenantId, citaDTO.getFechaHora());

            // Validar tenant
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

            // Obtener cliente
            Cliente cliente = clienteRepository.findById(citaDTO.getClienteId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            // Validar que el cliente pertenece al tenant
            if (!cliente.getTenant().getId().equals(tenantId)) {
                throw new RuntimeException("Cliente no pertenece al tenant");
            }

            // Obtener servicio
            Servicio servicio = servicioRepository.findById(citaDTO.getServicioId())
                    .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

            // Validar que el servicio pertenece al tenant
            if (!servicio.getTenant().getId().equals(tenantId)) {
                throw new RuntimeException("Servicio no pertenece al tenant");
            }

            // 🤖 IA AUTOMÁTICA: Si no se especifica empleado, la IA elige el mejor
            Empleado empleado = null;
            if (citaDTO.getEmpleadoId() != null) {
                empleado = empleadoRepository.findById(citaDTO.getEmpleadoId())
                        .orElse(null);
            }

            if (empleado == null) {
                empleado = buscarEmpleadoDisponibleConIA(tenantId, citaDTO.getFechaHora(), servicio);
            }

            // 🤖 IA AUTOMÁTICA: Optimizar horario si hay conflictos (solo ajustes menores)
            LocalDateTime fechaHoraOptimizada = optimizarHorarioConIA(tenantId, citaDTO.getFechaHora(), servicio, empleado);

            // 🕐 VALIDACIÓN FINAL: Verificar horario optimizado también
            if (!fechaHoraOptimizada.equals(citaDTO.getFechaHora())) {
                validarHorarioTrabajo(tenantId, fechaHoraOptimizada);
            }

            // Crear cita
            Cita cita = new Cita();
            cita.setTenant(tenant);
            cita.setCliente(cliente);
            cita.setServicio(servicio);
            cita.setEmpleado(empleado);
            cita.setFechaHora(fechaHoraOptimizada);
            cita.setEstado(EstadoCita.CONFIRMADA);
            cita.setOrigen(OrigenCita.MANUAL);
            cita.setDuracionMinutos(servicio.getDuracionMinutos());
            cita.setPrecio(servicio.getPrecio());
            cita.setNotas(citaDTO.getNotas());

            Cita citaGuardada = citaRepository.save(cita);

            // 🤖 IA AUTOMÁTICA: Enviar confirmación inteligente
            enviarConfirmacionPersonalizada(citaGuardada);

            return CitaDTO.fromCita(citaGuardada);

        } catch (Exception e) {
            throw new RuntimeException("Error creando cita: " + e.getMessage(), e);
        }
    }

    /**
     * 📋 OBTENER CITA POR ID
     */
    public CitaDTO getCitaById(String citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        return CitaDTO.fromCita(cita);
    }

    /**
     * ✏️ ACTUALIZAR CITA - CON IA AUTOMÁTICA
     */
    public CitaDTO updateCita(String citaId, CitaDTO citaDTO) {
        try {
            Cita cita = citaRepository.findById(citaId)
                    .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

            String tenantId = cita.getTenant().getId();

            // Actualizar campos si están presentes
            if (citaDTO.getFechaHora() != null && !citaDTO.getFechaHora().equals(cita.getFechaHora())) {
                // 🤖 IA AUTOMÁTICA: Optimizar nuevo horario
                LocalDateTime fechaHoraOptimizada = optimizarHorarioConIA(tenantId, citaDTO.getFechaHora(), cita.getServicio(), cita.getEmpleado());
                cita.setFechaHora(fechaHoraOptimizada);
            }

            if (citaDTO.getServicioId() != null && !citaDTO.getServicioId().equals(cita.getServicio().getId())) {
                Servicio nuevoServicio = servicioRepository.findById(citaDTO.getServicioId())
                        .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
                cita.setServicio(nuevoServicio);
                cita.setDuracionMinutos(nuevoServicio.getDuracionMinutos());
                cita.setPrecio(nuevoServicio.getPrecio());
            }

            if (citaDTO.getEmpleadoId() != null && (cita.getEmpleado() == null || !citaDTO.getEmpleadoId().equals(cita.getEmpleado().getId()))) {
                Empleado nuevoEmpleado = empleadoRepository.findById(citaDTO.getEmpleadoId())
                        .orElse(null);
                cita.setEmpleado(nuevoEmpleado);
            }

            if (citaDTO.getEstado() != null) {
                cita.setEstado(citaDTO.getEstado());

                // 🤖 IA AUTOMÁTICA: Enviar notificaciones según el estado
                enviarNotificacionCambioEstado(cita, citaDTO.getEstado());  // ← Usar directamente el estado del DTO
            }

            if (citaDTO.getNotas() != null) {
                cita.setNotas(citaDTO.getNotas());
            }

            Cita citaActualizada = citaRepository.save(cita);
            return CitaDTO.fromCita(citaActualizada);

        } catch (Exception e) {
            throw new RuntimeException("Error actualizando cita: " + e.getMessage(), e);
        }
    }

    /**
     * 🗑️ ELIMINAR CITA (CANCELAR)
     */
    public void deleteCita(String citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        cita.setEstado(EstadoCita.CANCELADA);
        citaRepository.save(cita);

        // 🤖 IA AUTOMÁTICA: Enviar notificación de cancelación
        enviarNotificacionCancelacion(cita);
    }

    // ========================================================================================
    // 🤖 MÉTODOS IA AUTOMÁTICA - CEREBRO INTELIGENTE
    // ========================================================================================

    /**
     * 🤖 IA: Buscar empleado disponible con inteligencia artificial
     */
    private Empleado buscarEmpleadoDisponibleConIA(String tenantId, LocalDateTime fechaHora, Servicio servicio) {
        try {
            List<Empleado> empleados = empleadoRepository.findByTenantIdAndActivoTrue(tenantId);

            if (empleados.isEmpty()) {
                return null;
            }

            // 🤖 IA: Filtrar empleados por especialidad si hay coincidencia
            if (servicio != null && servicio.getNombre() != null) {
                List<Empleado> empleadosEspecializados = empleados.stream()
                        .filter(emp -> emp.getEspecialidad() != null &&
                                emp.getEspecialidad().toLowerCase().contains(servicio.getNombre().toLowerCase()))
                        .collect(Collectors.toList());

                if (!empleadosEspecializados.isEmpty()) {
                    empleados = empleadosEspecializados;
                }
            }

            // 🤖 IA: Verificar disponibilidad y cargar de trabajo
            Map<Empleado, Integer> cargaTrabajo = new HashMap<>();

            for (Empleado empleado : empleados) {
                // Contar citas del día para este empleado
                List<Cita> citasDelDia = citaRepository.findByEmpleadoIdAndFechaHoraBetween(
                        empleado.getId(),
                        fechaHora.toLocalDate().atStartOfDay(),
                        fechaHora.toLocalDate().atTime(23, 59)
                );

                cargaTrabajo.put(empleado, citasDelDia.size());
            }

            // 🤖 IA: Seleccionar empleado con menor carga de trabajo
            return cargaTrabajo.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(empleados.get(0));

        } catch (Exception e) {
            // Fallback: primer empleado disponible
            List<Empleado> empleados = empleadoRepository.findByTenantIdAndActivoTrue(tenantId);
            return empleados.isEmpty() ? null : empleados.get(0);
        }
    }

    /**
     * 🤖 IA: Optimizar horario para evitar conflictos
     */
    private LocalDateTime optimizarHorarioConIA(String tenantId, LocalDateTime fechaHoraDeseada, Servicio servicio, Empleado empleado) {
        try {
            // Verificar si el horario está disponible
            List<Cita> citasConflicto = citaRepository.findByTenantIdAndFechaHoraBetween(
                    tenantId,
                    fechaHoraDeseada.minusMinutes(30),
                    fechaHoraDeseada.plusMinutes(servicio != null ? servicio.getDuracionMinutos() : 60)
            );

            // Si no hay conflictos, usar horario deseado
            if (citasConflicto.isEmpty()) {
                return fechaHoraDeseada;
            }

            // 🤖 IA: Buscar siguiente slot disponible
            LocalDateTime horarioOptimo = fechaHoraDeseada;
            for (int i = 0; i < 48; i++) { // Buscar en próximas 24 horas (slots de 30min)
                horarioOptimo = horarioOptimo.plusMinutes(30);

                List<Cita> conflictos = citaRepository.findByTenantIdAndFechaHoraBetween(
                        tenantId,
                        horarioOptimo.minusMinutes(15),
                        horarioOptimo.plusMinutes(servicio != null ? servicio.getDuracionMinutos() : 60)
                );

                if (conflictos.isEmpty()) {
                    return horarioOptimo;
                }
            }

            // Fallback: horario original
            return fechaHoraDeseada;

        } catch (Exception e) {
            return fechaHoraDeseada;
        }
    }

    /**
     * 🤖 IA: Enviar notificación según cambio de estado
     */
    private void enviarNotificacionCambioEstado(Cita cita, EstadoCita nuevoEstado) {
        try {
            Tenant tenant = cita.getTenant();
            String mensaje = "";

            switch (nuevoEstado) {
                case CONFIRMADA:
                    mensaje = String.format("✅ Su cita en %s ha sido confirmada para el %s.",
                            tenant.getNombrePeluqueria(),
                            cita.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    break;
                case CANCELADA:
                    mensaje = String.format("❌ Su cita en %s ha sido cancelada. Para reagendar contáctenos.",
                            tenant.getNombrePeluqueria());
                    break;
                case COMPLETADA:
                    mensaje = String.format("🎉 Gracias por visitar %s. ¿Cómo calificaría nuestro servicio? Responda del 1 al 5.",
                            tenant.getNombrePeluqueria());
                    break;
            }

            if (!mensaje.isEmpty()) {
                twilioService.enviarSMS(cita.getCliente().getTelefono(), mensaje);
            }

        } catch (Exception e) {
            System.err.println("Error enviando notificación de cambio de estado: " + e.getMessage());
        }
    }

    /**
     * 🤖 IA: Notificación inteligente de cancelación
     */
    private void enviarNotificacionCancelacion(Cita cita) {
        try {
            Tenant tenant = cita.getTenant();
            String mensaje = String.format(
                    "❌ Su cita en %s del %s ha sido cancelada. Para reagendar%s visite nuestras instalaciones.",
                    tenant.getNombrePeluqueria(),
                    cita.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    tenant.getTelefono() != null ? " llame al " + tenant.getTelefono() + " o" : ""
            );

            twilioService.enviarSMS(cita.getCliente().getTelefono(), mensaje);

        } catch (Exception e) {
            System.err.println("Error enviando notificación de cancelación: " + e.getMessage());
        }
    }

    // ========================================================================================
    // 🔧 MÉTODOS LEGACY MANTENIDOS
    // ========================================================================================

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

    private Empleado buscarEmpleadoDisponible(String tenantId, LocalDateTime fechaHora) {
        return buscarEmpleadoDisponibleConIA(tenantId, fechaHora, null);
    }

    /**
     * ✅ ENVIAR CONFIRMACIÓN PERSONALIZADA POR TENANT - NO MÁS HARDCODING
     */
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

    /**
     * ✅ CANCELAR CITA CON MENSAJE PERSONALIZADO POR TENANT
     */
    public void cancelarCita(String citaId) {
        deleteCita(citaId); // Usa el nuevo método inteligente
    }

    /**
     * ✅ ENVIAR RECORDATORIO PERSONALIZADO POR TENANT
     */
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


    /**
     * 🕐 VALIDAR HORARIO DE TRABAJO DEL TENANT - SEGURIDAD BACKEND
     */
    private void validarHorarioTrabajo(String tenantId, LocalDateTime fechaHora) {
        try {
            // Obtener configuración del tenant
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

            // Validar día laborable
            DayOfWeek diaSeleccionado = fechaHora.getDayOfWeek();
            String diasLaborables = tenant.getDiasLaborables();

            if (diasLaborables != null && !diasLaborables.isEmpty()) {
                Map<String, DayOfWeek> mapaDias = Map.of(
                        "L", DayOfWeek.MONDAY,
                        "M", DayOfWeek.TUESDAY,
                        "X", DayOfWeek.WEDNESDAY,
                        "J", DayOfWeek.THURSDAY,
                        "V", DayOfWeek.FRIDAY,
                        "S", DayOfWeek.SATURDAY,
                        "D", DayOfWeek.SUNDAY
                );

                Set<DayOfWeek> diasPermitidos = Arrays.stream(diasLaborables.split(","))
                        .map(String::trim)
                        .map(mapaDias::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                if (!diasPermitidos.contains(diaSeleccionado)) {
                    throw new RuntimeException(
                            String.format("No se pueden crear citas los %s. Días laborables: %s",
                                    diaSeleccionado.getDisplayName(TextStyle.FULL, new Locale("es")),
                                    diasLaborables)
                    );
                }
            }

            // Validar horario de apertura y cierre
            String horaApertura = tenant.getHoraApertura();
            String horaCierre = tenant.getHoraCierre();

            if (horaApertura != null && horaCierre != null) {
                LocalTime horaAperturaTime = LocalTime.parse(horaApertura);
                LocalTime horaCierreTime = LocalTime.parse(horaCierre);
                LocalTime horaCita = fechaHora.toLocalTime();

                if (horaCita.isBefore(horaAperturaTime) || horaCita.isAfter(horaCierreTime) || horaCita.equals(horaCierreTime)) {
                    throw new RuntimeException(
                            String.format("Horario fuera del horario de trabajo. Horario disponible: %s - %s",
                                    horaApertura, horaCierre)
                    );
                }
            }

            // Validar que no sea fecha pasada
            if (fechaHora.isBefore(LocalDateTime.now().withSecond(0).withNano(0))) {
                throw new RuntimeException("No se pueden crear citas en fechas u horas pasadas");
            }

        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw e; // Re-lanzar errores de validación
            }
            // En caso de error de parsing u otro, permitir (fallback)
            System.err.println("Error validando horario de trabajo: " + e.getMessage());
        }
    }
}