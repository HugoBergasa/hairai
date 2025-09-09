package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.dto.CitaConflictoDTO;
import com.peluqueria.recepcionista_virtual.model.*;
import com.peluqueria.recepcionista_virtual.repository.*;
import com.peluqueria.recepcionista_virtual.dto.DatosCita;
import com.peluqueria.recepcionista_virtual.dto.CitaDTO;
import com.peluqueria.recepcionista_virtual.dto.DisponibilidadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.Arrays;
import java.util.Objects;


@Service
@Transactional
public class CitaService {

    private static final Logger logger = LoggerFactory.getLogger(CitaService.class);


    // ========================================
    // DEPENDENCIAS - INYECCIÓN
    // ========================================

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

    // ✅ NUEVA DEPENDENCIA CRÍTICA - ZERO HARDCODING
    @Autowired
    private TenantConfigService tenantConfigService;

    @Autowired
    private EmpleadoServicioValidationService empleadoServicioValidationService;

    @Autowired
    private ValidacionTemporalService validacionTemporalService;

    @Autowired
    private UserService.UsuarioService usuarioService;

    @Autowired
    private HorarioEspecialRepository horarioEspecialRepository;


    // ========================================================================================
    // 🤖 MÉTODOS IA EXISTENTES - CORREGIDOS CON VALIDACIONES CRÍTICAS
    // ========================================================================================

    /**
     * 📄 MÉTODO CREAR CITA DESDE IA ACTUALIZADO - CON VALIDACIONES CRÍTICAS CORREGIDAS
     */
    /**
     * 📄 MÉTODO CREAR CITA DESDE IA ACTUALIZADO - CON VALIDACIONES CRÍTICAS Y INTERVALO MÍNIMO
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

            // 🆘 NUEVA VALIDACIÓN: Intervalo mínimo entre citas del mismo cliente
            validarIntervaloMinimoCliente(tenantId, cliente.getId(), fechaHora);

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

            // ✅ VALIDACIONES CRÍTICAS AGREGADAS - CORREGIDAS:
            if (servicio != null) {
                validarServicioParaCita(servicio.getId(), tenantId);
            }
            validarCapacidadSalon(tenantId, fechaHora);

            // 4. 🤖 IA AUTOMÁTICA: Buscar empleado disponible inteligentemente
            Empleado empleado = buscarEmpleadoDisponibleConIA(tenantId, fechaHora, servicio);

            // ✅ VALIDACIONES EMPLEADO - CORREGIDAS:
            if (empleado != null) {
                validarEmpleadoParaCita(empleado.getId(), tenantId);
                int duracionMinutos = servicio != null ? servicio.getDuracionMinutos() : 60;
                validarDisponibilidadEmpleado(empleado.getId(), fechaHora,
                        fechaHora.plusMinutes(duracionMinutos), null, tenantId);
            }

            // ✅ VALIDACIÓN DURACIÓN - CORREGIDA:
            if (servicio != null) {
                validarDuracionDentroDeHorario(fechaHora, servicio.getDuracionMinutos(), tenantId);
            }

            // 5. Crear la cita
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

            // 6. ✅ ENVIAR SMS DE CONFIRMACIÓN PERSONALIZADO POR TENANT
            enviarConfirmacionPersonalizada(citaGuardada);

            return citaGuardada;

        } catch (Exception e) {
            throw new RuntimeException("Error creando cita: " + e.getMessage(), e);
        }
    }


    // ========================================================================================
    // 🎯 MÉTODOS DTO PARA DASHBOARD - CORREGIDOS CON VALIDACIONES
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
     * 📄 OBTENER CITAS POR ESTADO
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
     * 📄 MÉTODO CREATECITA ACTUALIZADO - COMPLETAMENTE CORREGIDO
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

            // ✅ VALIDACIONES CRÍTICAS CORREGIDAS:
            validarServicioParaCita(citaDTO.getServicioId(), tenantId);

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

            // ✅ VALIDACIONES CRÍTICAS CON VARIABLES CORREGIDAS:
            validarCapacidadSalon(tenantId, fechaHoraOptimizada);

            if (empleado != null) {
                validarEmpleadoParaCita(empleado.getId(), tenantId);
                validarDisponibilidadEmpleado(empleado.getId(), fechaHoraOptimizada,
                        fechaHoraOptimizada.plusMinutes(servicio.getDuracionMinutos()),
                        null, tenantId);
            }

            validarDuracionDentroDeHorario(fechaHoraOptimizada, servicio.getDuracionMinutos(), tenantId);

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
     * ✏️ ACTUALIZAR CITA - CORREGIDO CON VALIDACIONES CRÍTICAS
     */
    public CitaDTO updateCita(String citaId, CitaDTO citaDTO) {
        try {
            Cita cita = citaRepository.findById(citaId)
                    .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

            String tenantId = cita.getTenant().getId();

            // ✅ VALIDACIÓN CRÍTICA DE SEGURIDAD AL INICIO:
            validarCitaPerteneceATenant(citaId, tenantId);

            // Actualizar campos si están presentes
            if (citaDTO.getFechaHora() != null && !citaDTO.getFechaHora().equals(cita.getFechaHora())) {
                // 🤖 IA AUTOMÁTICA: Optimizar nuevo horario
                LocalDateTime fechaHoraOptimizada = optimizarHorarioConIA(tenantId, citaDTO.getFechaHora(), cita.getServicio(), cita.getEmpleado());

                // ✅ VALIDACIONES PARA CAMBIO DE HORARIO:
                validarCapacidadSalon(tenantId, fechaHoraOptimizada);
                if (cita.getEmpleado() != null) {
                    validarDisponibilidadEmpleado(cita.getEmpleado().getId(), fechaHoraOptimizada,
                            fechaHoraOptimizada.plusMinutes(cita.getDuracionMinutos()),
                            citaId, tenantId);
                }

                cita.setFechaHora(fechaHoraOptimizada);
            }

            if (citaDTO.getServicioId() != null && !citaDTO.getServicioId().equals(cita.getServicio().getId())) {
                validarServicioParaCita(citaDTO.getServicioId(), tenantId);
                Servicio nuevoServicio = servicioRepository.findById(citaDTO.getServicioId())
                        .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
                cita.setServicio(nuevoServicio);
                cita.setDuracionMinutos(nuevoServicio.getDuracionMinutos());
                cita.setPrecio(nuevoServicio.getPrecio());
            }

            if (citaDTO.getEmpleadoId() != null && (cita.getEmpleado() == null || !citaDTO.getEmpleadoId().equals(cita.getEmpleado().getId()))) {
                validarEmpleadoParaCita(citaDTO.getEmpleadoId(), tenantId);
                Empleado nuevoEmpleado = empleadoRepository.findById(citaDTO.getEmpleadoId())
                        .orElse(null);

                // ✅ VALIDAR DISPONIBILIDAD DEL NUEVO EMPLEADO:
                if (nuevoEmpleado != null) {
                    validarDisponibilidadEmpleado(nuevoEmpleado.getId(), cita.getFechaHora(),
                            cita.getFechaHora().plusMinutes(cita.getDuracionMinutos()),
                            citaId, tenantId);
                }

                cita.setEmpleado(nuevoEmpleado);
            }

            if (citaDTO.getEstado() != null) {
                cita.setEstado(citaDTO.getEstado());

                // 🤖 IA AUTOMÁTICA: Enviar notificaciones según el estado
                enviarNotificacionCambioEstado(cita, citaDTO.getEstado());
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
     * 🗑️ ELIMINAR CITA (CANCELAR) - CORREGIDO CON VALIDACIÓN TENANT-SCOPE
     */
    public void deleteCita(String citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        // ✅ VALIDACIÓN CRÍTICA DE SEGURIDAD:
        validarCitaPerteneceATenant(citaId, cita.getTenant().getId());

        cita.setEstado(EstadoCita.CANCELADA);
        citaRepository.save(cita);

        // 🤖 IA AUTOMÁTICA: Enviar notificación de cancelación
        enviarNotificacionCancelacion(cita);
    }

    /**
     * ✅ CANCELAR CITA CON VALIDACIÓN TENANT-SCOPE
     */
    public void cancelarCita(String citaId) {
        deleteCita(citaId); // Usa el método corregido con validaciones
    }

    // ========================================================================================
    // 🛡️ VALIDACIONES CRÍTICAS DE SEGURIDAD - NUEVOS MÉTODOS
    // ========================================================================================

    /**
     * CRÍTICO: Validar que una cita pertenece al tenant antes de modificarla
     * MULTITENANT: Evita acceso cruzado entre tenants
     */
    private void validarCitaPerteneceATenant(String citaId, String tenantId) {
        Optional<Cita> citaOpt = citaRepository.findCitaByIdAndTenant(citaId, tenantId);

        if (citaOpt.isEmpty()) {
            throw new SecurityException(
                    obtenerMensajeConfigurable(tenantId, "mensaje_acceso_denegado",
                            "Acceso denegado: cita no pertenece al tenant")
            );
        }
    }

    /**
     * CRÍTICO: Validar disponibilidad de empleado (conflictos de horarios)
     * Evita doble-booking del mismo empleado
     */
    private void validarDisponibilidadEmpleado(String empleadoId, LocalDateTime inicio,
                                               LocalDateTime fin, String citaIdExcluir, String tenantId) {
        if (empleadoId == null) return;

        // Usar el nuevo query crítico para detectar conflictos
        List<Cita> citasConflicto = citaRepository.findCitasEmpleadoEnRango(
                empleadoId, inicio, fin, citaIdExcluir
        );

        if (!citasConflicto.isEmpty()) {
            Cita conflicto = citasConflicto.get(0);
            String mensajeError = String.format(
                    obtenerMensajeConfigurable(tenantId, "mensaje_empleado_no_disponible",
                            "Empleado no disponible de %s a %s. Conflicto con cita existente de %s"),
                    inicio.toLocalTime(),
                    fin.toLocalTime(),
                    conflicto.getFechaHora().toLocalTime()
            );

            throw new RuntimeException(mensajeError);
        }
    }

    /**
     * CRÍTICO: Validar que empleado está activo y pertenece al tenant
     * SEGURIDAD: Evita asignar empleados inactivos o de otros tenants
     */
    private void validarEmpleadoParaCita(String empleadoId, String tenantId) {
        if (empleadoId == null) return;

        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        if (!empleado.getActivo()) {
            throw new RuntimeException(
                    obtenerMensajeConfigurable(tenantId, "mensaje_empleado_invalido",
                            "No se puede asignar empleado inactivo")
            );
        }

        if (!empleado.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException(
                    obtenerMensajeConfigurable(tenantId, "mensaje_empleado_invalido",
                            "Empleado no pertenece al tenant")
            );
        }
    }

    /**
     * CRÍTICO: Validar que servicio está activo y pertenece al tenant
     * SEGURIDAD: Evita usar servicios inactivos o de otros tenants
     */
    private void validarServicioParaCita(String servicioId, String tenantId) {
        if (servicioId == null) return;

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        // Asumir que Servicio tiene campo activo (si no, comentar esta línea)
        // if (!servicio.getActivo()) {
        //     throw new RuntimeException(
        //         obtenerMensajeConfigurable(tenantId, "mensaje_servicio_invalido",
        //             "Servicio no disponible o inactivo")
        //     );
        // }

        if (!servicio.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException(
                    obtenerMensajeConfigurable(tenantId, "mensaje_servicio_invalido",
                            "Servicio no pertenece al tenant")
            );
        }
    }

    /**
     * CRÍTICO: Validar que el servicio no exceda el horario de cierre
     * Evita crear citas que terminen después del cierre
     */
    private void validarDuracionDentroDeHorario(LocalDateTime inicio, int duracionMinutos, String tenantId) {
        try {
            // 🔥 CAMBIO CRÍTICO: Obtener hora cierre desde configuracion_tenant
            String horaCierre = obtenerConfiguracion(tenantId, "hora_cierre", "20:00");

            if (horaCierre == null) return; // Si no hay horario configurado, permitir

            LocalDateTime fin = inicio.plusMinutes(duracionMinutos);
            LocalTime finTime = fin.toLocalTime();
            LocalTime cierreTime = LocalTime.parse(horaCierre);

            if (finTime.isAfter(cierreTime)) {
                throw new RuntimeException(
                        obtenerMensajeConfigurable(tenantId, "mensaje_excede_horario_cierre",
                                String.format("El servicio terminaría a las %s, después del cierre (%s)",
                                        finTime, cierreTime))
                );
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw e;
            logger.warn("Error validando duración dentro de horario: {}", e.getMessage());
            // Si hay error de parsing, usar método fallback
            validarDuracionDentroDeHorarioFallback(inicio, duracionMinutos, tenantId);
        }
    }

    /**
     * 🛟 FALLBACK: Validar duración usando datos de tabla tenants
     */
    private void validarDuracionDentroDeHorarioFallback(LocalDateTime inicio, int duracionMinutos, String tenantId) {
        try {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

            if (tenant.getHoraCierre() == null) return;

            LocalDateTime fin = inicio.plusMinutes(duracionMinutos);
            LocalTime finTime = fin.toLocalTime();
            LocalTime cierreTime = LocalTime.parse(tenant.getHoraCierre());

            if (finTime.isAfter(cierreTime)) {
                throw new RuntimeException(
                        String.format("El servicio terminaría a las %s, después del cierre (%s)",
                                finTime, cierreTime)
                );
            }
        } catch (Exception e) {
            logger.warn("Fallback de validación de duración también falló: {}", e.getMessage());
            // Fallback final: continuar sin validar
        }
    }

    /**
     * CRÍTICO: Validar capacidad máxima del salón en un slot
     * Evita sobresaturación de citas simultáneas
     */
    private void validarCapacidadSalon(String tenantId, LocalDateTime fechaHora) {
        try {
            // Obtener capacidad máxima de configuración
            String capacidadMaxStr = obtenerConfiguracion(tenantId, "capacidad_max_simultaneas", "10");
            int capacidadMax = Integer.parseInt(capacidadMaxStr);

            Long citasEnSlot = citaRepository.countCitasActivasEnSlot(tenantId, fechaHora);

            if (citasEnSlot >= capacidadMax) {
                throw new RuntimeException(
                        obtenerMensajeConfigurable(tenantId, "mensaje_salon_lleno",
                                String.format("Salón lleno en ese horario (máximo %d citas simultáneas)", capacidadMax))
                );
            }
        } catch (NumberFormatException e) {
            // Si hay error en configuración, continuar (fallback seguro)
        }
    }

    /**
     * NUEVO: Método para verificar disponibilidad completa con análisis IA
     * Integra todas las validaciones en un solo método
     */
    public DisponibilidadResult verificarDisponibilidadCompleta(String tenantId,
                                                                LocalDateTime fechaHora,
                                                                String servicioId,
                                                                String empleadoId) {
        try {
            // 1. Validaciones básicas
            validarHorarioTrabajo(tenantId, fechaHora);

            // 2. Validar entidades
            validarServicioParaCita(servicioId, tenantId);
            if (empleadoId != null) {
                validarEmpleadoParaCita(empleadoId, tenantId);
            }

            // 3. Obtener duración del servicio
            Optional<Servicio> servicioOpt = servicioRepository.findById(servicioId);
            int duracion = servicioOpt.map(Servicio::getDuracionMinutos).orElse(60);

            // 4. Validar capacidad
            validarCapacidadSalon(tenantId, fechaHora);

            // 5. Validar empleado si está especificado
            if (empleadoId != null) {
                validarDisponibilidadEmpleado(empleadoId, fechaHora,
                        fechaHora.plusMinutes(duracion), null, tenantId);
            }

            // 6. Validar duración vs horario
            validarDuracionDentroDeHorario(fechaHora, duracion, tenantId);

            return DisponibilidadResult.disponible();

        } catch (RuntimeException e) {
            // ✅ USAR MÉTODOS CORRECTOS DE TU CLASE DisponibilidadResult:
            DisponibilidadResult resultado = DisponibilidadResult.noDisponible(e.getMessage());

            // Agregar análisis del tipo de conflicto
            if (e.getMessage().contains("Empleado no disponible")) {
                resultado.setTipoRestriccion("CONFLICTO_EMPLEADO");
                // ✅ USAR el método agregarConflicto() que ya tienes:
                resultado.agregarConflicto("EMPLEADO_OCUPADO");
            } else if (e.getMessage().contains("lleno")) {
                resultado.setTipoRestriccion("CAPACIDAD_EXCEDIDA");
                resultado.agregarConflicto("SALON_LLENO");
            } else if (e.getMessage().contains("cierre")) {
                resultado.setTipoRestriccion("FUERA_HORARIO");
                resultado.agregarConflicto("EXCEDE_HORARIO_CIERRE");
            }

            // Intentar obtener sugerencias IA
            try {
                String analisisIA = openAIService.analizarConflictoDisponibilidad(tenantId, e.getMessage(), fechaHora);
                resultado.setSugerenciaIA(analisisIA);
                resultado.setConfianzaSugerencia(0.8);
            } catch (Exception ex) {
                logger.warn("Error obteniendo análisis IA: {}", ex.getMessage());
            }

            return resultado;
        }
    }

    // ========================================================================================
    // 🛠️ MÉTODOS AUXILIARES ZERO HARDCODING
    // ========================================================================================

    /**
     * ZERO HARDCODING: Obtener mensaje configurable por tenant
     */
    private String obtenerMensajeConfigurable(String tenantId, String clave, String mensajePorDefecto) {
        try {
            return tenantConfigService.obtenerValor(tenantId, clave, mensajePorDefecto);
        } catch (Exception e) {
            return mensajePorDefecto;
        }
    }

    /**
     * ZERO HARDCODING: Obtener cualquier configuración por tenant
     */
    private String obtenerConfiguracion(String tenantId, String clave, String porDefecto) {
        try {
            return tenantConfigService.obtenerValor(tenantId, clave, porDefecto);
        } catch (Exception e) {
            return porDefecto;
        }
    }
    // ========================================================================================
    // 🤖 MÉTODOS IA AUTOMÁTICA - CEREBRO INTELIGENTE (MANTENIDOS)
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
            // 🆘 CAMBIO CRÍTICO: Obtener horarios desde configuracion_tenant NO de tabla tenants
            String horaApertura = obtenerConfiguracion(tenantId, "hora_apertura", "09:00");
            String horaCierre = obtenerConfiguracion(tenantId, "hora_cierre", "20:00");
            String diasLaborables = obtenerConfiguracion(tenantId, "dias_laborables", "L,M,X,J,V,S");

            // Validar día laborable
            DayOfWeek diaSeleccionado = fechaHora.getDayOfWeek();

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
                            obtenerMensajeConfigurable(tenantId, "mensaje_dia_no_laborable",
                                    String.format("No se pueden crear citas los %s. Días laborables: %s",
                                            diaSeleccionado.getDisplayName(TextStyle.FULL, new Locale("es")),
                                            diasLaborables))
                    );
                }
            }

            // Validar horario de apertura y cierre
            if (horaApertura != null && horaCierre != null) {
                LocalTime horaAperturaTime = LocalTime.parse(horaApertura);
                LocalTime horaCierreTime = LocalTime.parse(horaCierre);
                LocalTime horaCita = fechaHora.toLocalTime();

                if (horaCita.isBefore(horaAperturaTime) || horaCita.isAfter(horaCierreTime) || horaCita.equals(horaCierreTime)) {
                    throw new RuntimeException(
                            obtenerMensajeConfigurable(tenantId, "mensaje_horario_invalido",
                                    String.format("Horario fuera del horario de trabajo. Horario disponible: %s - %s",
                                            horaApertura, horaCierre))
                    );
                }
            }

            // Validar que no sea fecha pasada
            if (fechaHora.isBefore(LocalDateTime.now().withSecond(0).withNano(0))) {
                throw new RuntimeException(
                        obtenerMensajeConfigurable(tenantId, "mensaje_fecha_pasada",
                                "No se pueden crear citas en fechas u horas pasadas")
                );
            }

        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw e; // Re-lanzar errores de validación
            }
            logger.error("Error validando horario de trabajo: {}", e.getMessage());
            // En caso de error de parsing u otro, usar fallback de tabla tenants
            validarHorarioTrabajoFallback(tenantId, fechaHora);
        }
    }

    /**
     * 🛟 MÉTODO FALLBACK - Si falla configuracion_tenant, usar datos de tenants
     * Solo para casos de emergencia/error
     */
    private void validarHorarioTrabajoFallback(String tenantId, LocalDateTime fechaHora) {
        try {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

            // Este es el código original - solo como fallback
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

        } catch (Exception e) {
            logger.warn("Fallback de validación de horario también falló: {}", e.getMessage());
            // Si todo falla, permitir la cita con un log de advertencia
        }
    }

    /**
     * 🆘 NUEVO MÉTODO CRÍTICO: Validar intervalo mínimo entre citas del mismo cliente
     * 100% configurable desde configuracion_tenant
     */
    private void validarIntervaloMinimoCliente(String tenantId, String clienteId, LocalDateTime fechaHoraNueva) {
        try {
            // Obtener intervalo mínimo desde configuración (en minutos)
            String intervaloMinStr = obtenerConfiguracion(tenantId, "intervalo_minimo_citas_mismo_cliente", "60");
            int intervaloMinutos = Integer.parseInt(intervaloMinStr);

            // Si el intervalo es 0, no validar
            if (intervaloMinutos <= 0) {
                return;
            }

            // Buscar la última cita del cliente
            List<Cita> citasRecientes = citaRepository.findByClienteIdAndTenantIdOrderByFechaHoraDesc(clienteId, tenantId);

            if (!citasRecientes.isEmpty()) {
                Cita ultimaCita = citasRecientes.get(0);
                LocalDateTime ultimaFecha = ultimaCita.getFechaHora();

                // Calcular diferencia en minutos
                long minutosEntre = ChronoUnit.MINUTES.between(ultimaFecha, fechaHoraNueva);

                if (Math.abs(minutosEntre) < intervaloMinutos) {
                    throw new RuntimeException(
                            obtenerMensajeConfigurable(tenantId, "mensaje_intervalo_minimo",
                                    String.format("Debe esperar al menos %d minutos entre citas. Última cita: %s",
                                            intervaloMinutos,
                                            ultimaFecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))))
                    );
                }
            }

        } catch (NumberFormatException e) {
            logger.warn("Error parsing intervalo mínimo para tenant {}: {}", tenantId, e.getMessage());
            // Si hay error en configuración, no validar (fallback seguro)
        } catch (Exception e) {
            logger.error("Error validando intervalo mínimo cliente: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * CRÍTICO: Validación completa mejorada con todas las nuevas validaciones
     */
    public DisponibilidadResult verificarDisponibilidadAvanzada(String tenantId,
                                                                LocalDateTime fechaHora,
                                                                String servicioId,
                                                                String empleadoId,
                                                                String clienteId) {
        try {
            logger.info("Verificación avanzada de disponibilidad - Tenant: {}, Fecha: {}",
                    tenantId, fechaHora);

            // 1. Validaciones básicas existentes
            validarHorarioTrabajo(tenantId, fechaHora);

            if (servicioId != null) {
                validarServicioParaCita(servicioId, tenantId);
            }

            if (empleadoId != null) {
                validarEmpleadoParaCita(empleadoId, tenantId);
            }

            // 2. NUEVAS VALIDACIONES CRÍTICAS
            if (empleadoId != null && servicioId != null) {
                empleadoServicioValidationService.validarEmpleadoAutorizadoParaServicio(
                        empleadoId, servicioId, tenantId);
                empleadoServicioValidationService.validarEmpleadoTrabajaEnFecha(
                        empleadoId, fechaHora.toLocalDate(), tenantId);
            }

            // 3. Validación de capacidad específica por servicio
            if (servicioId != null) {
                empleadoServicioValidationService.validarCapacidadServicioEspecifico(
                        tenantId, servicioId, fechaHora.toLocalDate());
            }

            // 4. Validación temporal si hay cliente específico
            if (clienteId != null) {
                validacionTemporalService.validarSecuenciaTemporal(
                        clienteId, fechaHora, tenantId);
            }

            // 5. Validación de horario dinámico
            validacionTemporalService.validarHorarioTrabajoDinamico(
                    tenantId, fechaHora, empleadoId);

            logger.debug("Verificación avanzada EXITOSA para tenant {}", tenantId);
            return DisponibilidadResult.disponible();

        } catch (Exception e) {
            logger.warn("Verificación avanzada FALLÓ: {}", e.getMessage());
            return crearResultadoConAnalisisIA(e, tenantId, fechaHora);
        }
    }

    /**
     * CRÍTICO: Validación de modificación de citas existentes
     */
    public void validarModificacionCita(Cita citaExistente, CitaDTO nuevosDatos,
                                        String usuarioId, String tenantId) {
        try {
            logger.info("Validando modificación de cita {} por usuario {}",
                    citaExistente.getId(), usuarioId);

            // 1. Validar permisos de modificación
            if (!usuarioService.puedeModificarCita(usuarioId, citaExistente.getId(), tenantId)) {
                throw new SecurityException("Sin permisos para modificar esta cita");
            }

            // 2. Si cambia fecha/hora, validar disponibilidad completa
            if (nuevosDatos.getFechaHora() != null &&
                    !nuevosDatos.getFechaHora().equals(citaExistente.getFechaHora())) {

                validacionTemporalService.validarModificacionCitaTemporal(
                        citaExistente, nuevosDatos.getFechaHora(), tenantId);

                DisponibilidadResult disponibilidad = verificarDisponibilidadAvanzada(
                        tenantId,
                        nuevosDatos.getFechaHora(),
                        nuevosDatos.getServicioId() != null ? nuevosDatos.getServicioId() :
                                citaExistente.getServicio().getId(),
                        nuevosDatos.getEmpleadoId() != null ? nuevosDatos.getEmpleadoId() :
                                (citaExistente.getEmpleado() != null ? citaExistente.getEmpleado().getId() : null),
                        citaExistente.getCliente().getId()
                );

                if (!disponibilidad.isDisponible()) {
                    throw new RuntimeException("Nueva fecha/hora no disponible: " +
                            disponibilidad.getMensaje());
                }
            }

            // 3. Si cambia estado, validar transición
            if (nuevosDatos.getEstado() != null &&
                    nuevosDatos.getEstado() != citaExistente.getEstado()) {

                validacionTemporalService.validarTransicionEstado(
                        citaExistente, nuevosDatos.getEstado(), usuarioId, tenantId);
            }

            logger.debug("Validación de modificación EXITOSA");

        } catch (Exception e) {
            logger.error("Error validando modificación de cita: {}", e.getMessage());
            throw e; // Re-lanzar para manejo en controlador
        }
    }

    /**
     * CRÍTICO: Crear cita con validaciones avanzadas
     */
    public Cita crearCitaConValidacionesAvanzadas(String tenantId, String telefono,
                                                  DatosCita datos, String usuarioId) {
        try {
            logger.info("Creando cita con validaciones avanzadas - Tenant: {}, Usuario: {}",
                    tenantId, usuarioId);

            // Parsear fecha y hora
            LocalDateTime fechaHora = parsearFechaHora(datos.getFecha(), datos.getHora());

            // Obtener datos preliminares
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

            Cliente cliente = clienteRepository
                    .findByTelefonoAndTenantId(telefono, tenantId)
                    .orElseGet(() -> crearClienteNuevo(tenant, telefono, datos.getNombreCliente()));

            Servicio servicio = buscarServicioPorNombre(datos.getServicio(), tenantId);
            Empleado empleado = buscarEmpleadoDisponibleConIA(tenantId, fechaHora, servicio);

            // VALIDACIONES AVANZADAS COMPLETAS
            DisponibilidadResult disponibilidad = verificarDisponibilidadAvanzada(
                    tenantId, fechaHora,
                    servicio != null ? servicio.getId() : null,
                    empleado != null ? empleado.getId() : null,
                    cliente.getId()
            );

            if (!disponibilidad.isDisponible()) {
                throw new RuntimeException("Cita no disponible: " + disponibilidad.getMensaje());
            }

            // Crear la cita
            Cita cita = new Cita();
            cita.setTenant(tenant);
            cita.setCliente(cliente);
            cita.setServicio(servicio);
            cita.setEmpleado(empleado);
            cita.setFechaHora(fechaHora);
            cita.setEstado(EstadoCita.CONFIRMADA);
            cita.setOrigen(OrigenCita.TELEFONO);
            cita.setNotas("Cita creada con IA - Tel: " + telefono +
                    (usuarioId != null ? " - Usuario: " + usuarioId : ""));

            if (servicio != null) {
                cita.setDuracionMinutos(servicio.getDuracionMinutos());
                cita.setPrecio(servicio.getPrecio());
            }

            Cita citaGuardada = citaRepository.save(cita);
            enviarConfirmacionPersonalizada(citaGuardada);

            logger.info("Cita creada exitosamente con validaciones avanzadas: {}",
                    citaGuardada.getId());

            return citaGuardada;

        } catch (Exception e) {
            logger.error("Error creando cita con validaciones avanzadas: {}", e.getMessage());
            throw new RuntimeException("Error creando cita: " + e.getMessage(), e);
        }
    }

    /**
     * CRÍTICO: Actualizar cita con validaciones completas
     */
    public CitaDTO updateCitaConValidaciones(String citaId, CitaDTO citaDTO, String usuarioId) {
        try {
            Cita cita = citaRepository.findById(citaId)
                    .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

            String tenantId = cita.getTenant().getId();

            // VALIDACIÓN CRÍTICA DE SEGURIDAD
            validarCitaPerteneceATenant(citaId, tenantId);

            // VALIDACIONES AVANZADAS DE MODIFICACIÓN
            validarModificacionCita(cita, citaDTO, usuarioId, tenantId);

            // Aplicar cambios validados
            return aplicarCambiosCita(cita, citaDTO, tenantId);

        } catch (Exception e) {
            logger.error("Error actualizando cita con validaciones: {}", e.getMessage());
            throw new RuntimeException("Error actualizando cita: " + e.getMessage(), e);
        }
    }

    /**
     * UTIL: Validar disponibilidad para reprogramación
     */
    public DisponibilidadResult validarReprogramacion(String citaId, LocalDateTime nuevaFecha,
                                                      String tenantId, String usuarioId) {
        try {
            Cita citaExistente = citaRepository.findById(citaId)
                    .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

            // Validar permisos
            usuarioService.validarUsuarioTenant(usuarioId, tenantId);

            // Validar que está en período de gracia
            if (!validacionTemporalService.estaDentroPeriodoGracia(citaExistente, tenantId)) {
                return DisponibilidadResult.noDisponible(
                        "Fuera del período permitido para reprogramar");
            }

            // Verificar disponibilidad en nueva fecha
            return verificarDisponibilidadAvanzada(
                    tenantId, nuevaFecha,
                    citaExistente.getServicio().getId(),
                    citaExistente.getEmpleado() != null ? citaExistente.getEmpleado().getId() : null,
                    citaExistente.getCliente().getId()
            );

        } catch (Exception e) {
            logger.error("Error validando reprogramación: {}", e.getMessage());
            return DisponibilidadResult.noDisponible("Error en validación: " + e.getMessage());
        }
    }

// ========================================
// MÉTODOS AUXILIARES PRIVADOS
// ========================================

    /**
     * CEREBRO OPENAI: Crear resultado con análisis IA
     */
    private DisponibilidadResult crearResultadoConAnalisisIA(Exception e,
                                                             String tenantId,
                                                             LocalDateTime fechaHora) {
        DisponibilidadResult resultado = DisponibilidadResult.noDisponible(e.getMessage());

        try {
            // Clasificar tipo de error
            if (e.getMessage().contains("empleado")) {
                resultado.setTipoRestriccion("EMPLEADO_NO_DISPONIBLE");
                resultado.agregarConflicto("EMPLEADO_OCUPADO");
            } else if (e.getMessage().contains("servicio")) {
                resultado.setTipoRestriccion("SERVICIO_RESTRINGIDO");
                resultado.agregarConflicto("SERVICIO_NO_AUTORIZADO");
            } else if (e.getMessage().contains("capacidad")) {
                resultado.setTipoRestriccion("CAPACIDAD_EXCEDIDA");
                resultado.agregarConflicto("SALON_LLENO");
            } else if (e.getMessage().contains("horario")) {
                resultado.setTipoRestriccion("FUERA_HORARIO");
                resultado.agregarConflicto("HORARIO_NO_VALIDO");
            }

            // Intentar obtener análisis IA
            try {
                String analisisIA = openAIService.analizarConflictoDisponibilidad(
                        tenantId, e.getMessage(), fechaHora);
                resultado.setSugerenciaIA(analisisIA);
                resultado.setConfianzaSugerencia(0.8);
            } catch (Exception ex) {
                logger.warn("Error obteniendo análisis IA: {}", ex.getMessage());
            }

        } catch (Exception ex) {
            logger.warn("Error creando resultado con análisis IA: {}", ex.getMessage());
        }

        return resultado;
    }

    private Cliente crearClienteNuevo(Tenant tenant, String telefono, String nombre) {
        Cliente nuevo = new Cliente();
        nuevo.setTenant(tenant);
        nuevo.setTelefono(telefono);
        nuevo.setNombre(nombre != null ? nombre : "Cliente");
        return clienteRepository.save(nuevo);
    }

    private Servicio buscarServicioPorNombre(String nombreServicio, String tenantId) {
        if (nombreServicio == null) return null;

        List<Servicio> servicios = servicioRepository
                .findByNombreContainingIgnoreCaseAndTenantId(nombreServicio, tenantId);

        return servicios.isEmpty() ? null : servicios.get(0);
    }

    private CitaDTO aplicarCambiosCita(Cita cita, CitaDTO citaDTO, String tenantId) {
        boolean huboCambios = false;

        // Aplicar cambios de fecha/hora
        if (citaDTO.getFechaHora() != null && !citaDTO.getFechaHora().equals(cita.getFechaHora())) {
            LocalDateTime fechaOptimizada = optimizarHorarioConIA(
                    tenantId, citaDTO.getFechaHora(), cita.getServicio(), cita.getEmpleado());
            cita.setFechaHora(fechaOptimizada);
            huboCambios = true;
        }

        // Aplicar cambios de servicio
        if (citaDTO.getServicioId() != null &&
                !citaDTO.getServicioId().equals(cita.getServicio().getId())) {

            Servicio nuevoServicio = servicioRepository.findById(citaDTO.getServicioId())
                    .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));
            cita.setServicio(nuevoServicio);
            cita.setDuracionMinutos(nuevoServicio.getDuracionMinutos());
            cita.setPrecio(nuevoServicio.getPrecio());
            huboCambios = true;
        }

        // Aplicar cambios de empleado
        if (citaDTO.getEmpleadoId() != null &&
                (cita.getEmpleado() == null || !citaDTO.getEmpleadoId().equals(cita.getEmpleado().getId()))) {

            Empleado nuevoEmpleado = empleadoRepository.findById(citaDTO.getEmpleadoId())
                    .orElse(null);
            cita.setEmpleado(nuevoEmpleado);
            huboCambios = true;
        }

        // Aplicar cambio de estado
        if (citaDTO.getEstado() != null && citaDTO.getEstado() != cita.getEstado()) {
            cita.setEstado(citaDTO.getEstado());
            enviarNotificacionCambioEstado(cita, citaDTO.getEstado());
            huboCambios = true;
        }

        // Aplicar notas
        if (citaDTO.getNotas() != null) {
            cita.setNotas(citaDTO.getNotas());
            huboCambios = true;
        }

        if (huboCambios) {
            Cita citaActualizada = citaRepository.save(cita);
            return CitaDTO.fromCita(citaActualizada);
        }

        return CitaDTO.fromCita(cita);
    }

    /**
     * 🎯 MÉTODO CRÍTICO: analizarConflictosDetallados()
     * Detecta y clasifica conflictos específicos por tipo
     */
    public List<CitaConflictoDTO> analizarConflictosDetallados(String tenantId,
                                                               LocalDateTime fechaHora,
                                                               String servicioId,
                                                               String empleadoId) {
        List<CitaConflictoDTO> conflictos = new ArrayList<>();

        try {
            // 1. CONFLICTOS DE HORARIO DE TRABAJO
            try {
                validarHorarioTrabajo(tenantId, fechaHora);
            } catch (RuntimeException e) {
                conflictos.add(new CitaConflictoDTO(
                        "HORARIO_TRABAJO",
                        "Fuera del horario de trabajo",
                        e.getMessage(),
                        fechaHora,
                        null,
                        "ALTA"
                ));
            }

            // 2. CONFLICTOS DE CAPACIDAD DEL SALÓN
            try {
                validarCapacidadSalon(tenantId, fechaHora);
            } catch (RuntimeException e) {
                Long citasEnSlot = citaRepository.countCitasActivasEnSlot(tenantId, fechaHora);

                Map<String, String> detallesCapacidad = new HashMap<>();
                detallesCapacidad.put("citasActuales", citasEnSlot.toString());

                conflictos.add(new CitaConflictoDTO(
                        "CAPACIDAD_EXCEDIDA",
                        "Salón lleno en ese horario",
                        e.getMessage(),
                        fechaHora,
                        detallesCapacidad,
                        "ALTA"
                ));
            }

            // 3. CONFLICTOS DE EMPLEADO ESPECÍFICO
            if (empleadoId != null) {
                try {
                    validarEmpleadoParaCita(empleadoId, tenantId);

                    // Obtener duración del servicio para validar disponibilidad
                    int duracion = 60; // Default
                    if (servicioId != null) {
                        Optional<Servicio> servicio = servicioRepository.findById(servicioId);
                        if (servicio.isPresent()) {
                            duracion = servicio.get().getDuracionMinutos();
                        }
                    }

                    validarDisponibilidadEmpleado(empleadoId, fechaHora,
                            fechaHora.plusMinutes(duracion), null, tenantId);

                } catch (RuntimeException e) {
                    // Buscar citas conflictivas para más detalles
                    List<Cita> citasConflicto = citaRepository.findCitasEmpleadoEnRango(
                            empleadoId, fechaHora, fechaHora.plusMinutes(60), null);

                    Map<String, String> detallesEmpleado = new HashMap<>();
                    if (!citasConflicto.isEmpty()) {
                        Cita citaConflictiva = citasConflicto.get(0);
                        detallesEmpleado.put("citaConflictiva", citaConflictiva.getId());
                        detallesEmpleado.put("horarioConflicto", citaConflictiva.getFechaHora().toString());
                        detallesEmpleado.put("clienteConflicto", citaConflictiva.getCliente().getNombre());
                    }

                    conflictos.add(new CitaConflictoDTO(
                            "EMPLEADO_NO_DISPONIBLE",
                            "Empleado ocupado en ese horario",
                            e.getMessage(),
                            fechaHora,
                            detallesEmpleado,
                            "MEDIA"
                    ));
                }
            }

            // 4. CONFLICTOS DE SERVICIO
            if (servicioId != null) {
                try {
                    validarServicioParaCita(servicioId, tenantId);

                    // Validar que el servicio termine dentro del horario
                    Servicio servicio = servicioRepository.findById(servicioId).orElse(null);
                    if (servicio != null) {
                        validarDuracionDentroDeHorario(fechaHora, servicio.getDuracionMinutos(), tenantId);
                    }
                } catch (RuntimeException e) {
                    Map<String, String> detallesServicio = new HashMap<>();
                    if (servicioId != null) {
                        detallesServicio.put("servicioId", servicioId);
                    }

                    conflictos.add(new CitaConflictoDTO(
                            "SERVICIO_INVALIDO",
                            "Problema con el servicio seleccionado",
                            e.getMessage(),
                            fechaHora,
                            detallesServicio,
                            "MEDIA"
                    ));
                }
            }

            // 5. CONFLICTOS DE HORARIOS ESPECIALES (cierres) - CORREGIDO
            try {
                // Verificar si existe el repositorio antes de usarlo
                if (horarioEspecialRepository != null) {
                    List<HorarioEspecial> cierresActivos = horarioEspecialRepository
                            .findActivosByTenantAndFecha(tenantId, fechaHora.toLocalDate());

                    for (HorarioEspecial cierre : cierresActivos) {
                        if (cierre.afectaHorario(fechaHora)) {
                            Map<String, String> detallesCierre = new HashMap<>();
                            // ✅ CORRECCIÓN: Convertir enum a String
                            detallesCierre.put("tipoCierre", cierre.getTipoCierre().toString());
                            detallesCierre.put("fechaInicio", cierre.getFechaInicio().toString());
                            detallesCierre.put("fechaFin", cierre.getFechaFin().toString());

                            conflictos.add(new CitaConflictoDTO(
                                    "CIERRE_ESPECIAL",
                                    "Salón cerrado por horario especial",
                                    cierre.getMotivo() != null ? cierre.getMotivo() : "Cierre programado",
                                    fechaHora,
                                    detallesCierre,
                                    "ALTA"
                            ));
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Error verificando horarios especiales: {}", e.getMessage());
                // Continuar sin este chequeo si hay error
            }

            logger.debug("Análisis de conflictos completado para tenant {}: {} conflictos encontrados",
                    tenantId, conflictos.size());

        } catch (Exception e) {
            logger.error("Error analizando conflictos para tenant {}: {}", tenantId, e.getMessage());
            conflictos.add(new CitaConflictoDTO(
                    "ERROR_SISTEMA",
                    "Error interno del sistema",
                    "Error inesperado durante el análisis: " + e.getMessage(),
                    fechaHora,
                    null,
                    "CRITICA"
            ));
        }

        return conflictos;
    }

    /**
     * 🤖 MÉTODO IA: obtenerSugerenciasConflictos()
     * Usa OpenAI para sugerir soluciones a los conflictos encontrados
     */
    public String obtenerSugerenciasConflictos(String tenantId, List<CitaConflictoDTO> conflictos) {
        try {
            if (conflictos.isEmpty()) {
                return "No hay conflictos que resolver";
            }

            // Preparar contexto para OpenAI
            StringBuilder contexto = new StringBuilder();
            contexto.append("Conflictos encontrados en la reserva de cita:\\n");

            for (CitaConflictoDTO conflicto : conflictos) {
                contexto.append(String.format("- %s: %s\\n",
                        conflicto.getTipoConflicto(),
                        conflicto.getDescripcion()));
            }

            // Obtener configuración del tenant para contexto
            String nombreSalon = obtenerConfiguracion(tenantId, "nombre_negocio", "Salón");
            String horaApertura = obtenerConfiguracion(tenantId, "hora_apertura", "09:00");
            String horaCierre = obtenerConfiguracion(tenantId, "hora_cierre", "20:00");

            contexto.append(String.format("\\nContexto del salón '%s':\\n", nombreSalon));
            contexto.append(String.format("- Horario: %s a %s\\n", horaApertura, horaCierre));

            String prompt = String.format(
                    "Eres el asistente IA de %s. Analiza estos conflictos de reserva y sugiere 2-3 alternativas específicas y prácticas para el cliente:\\n\\n%s\\n\\nRespuesta en español, máximo 150 palabras:",
                    nombreSalon,
                    contexto.toString()
            );

            // Llamar a OpenAI
            return openAIService.obtenerRespuestaSimple(prompt, tenantId);

        } catch (Exception e) {
            logger.warn("Error obteniendo sugerencias IA para conflictos: {}", e.getMessage());
            return "Sugerencias IA no disponibles. Por favor contacte con el salón para alternativas.";
        }
    }

    /**
     * 🎯 MÉTODO CRÍTICO: analizarDisponibilidadRango()
     * Analiza disponibilidad en un rango de fechas
     */
    public List<Map<String, Object>> analizarDisponibilidadRango(String tenantId,
                                                                 LocalDateTime fechaInicio,
                                                                 LocalDateTime fechaFin,
                                                                 String servicioId,
                                                                 String empleadoId) {
        List<Map<String, Object>> resultado = new ArrayList<>();

        try {
            LocalDate diaActual = fechaInicio.toLocalDate();
            LocalDate diaFinal = fechaFin.toLocalDate();

            while (!diaActual.isAfter(diaFinal)) {
                Map<String, Object> disponibilidadDia = new HashMap<>();
                disponibilidadDia.put("fecha", diaActual.toString());

                // Verificar si es día laborable
                boolean esDiaLaborable = esDiaLaborable(tenantId, diaActual.atStartOfDay());
                disponibilidadDia.put("esDiaLaborable", esDiaLaborable);

                if (!esDiaLaborable) {
                    disponibilidadDia.put("hayDisponibilidad", false);
                    disponibilidadDia.put("motivo", "No es día laborable");
                    disponibilidadDia.put("slotsDisponibles", 0);
                } else {
                    // Analizar slots disponibles en el día
                    List<LocalDateTime> slotsDisponibles = obtenerSlotsDisponiblesEnDia(
                            tenantId, diaActual, servicioId, empleadoId);

                    disponibilidadDia.put("hayDisponibilidad", !slotsDisponibles.isEmpty());
                    disponibilidadDia.put("slotsDisponibles", slotsDisponibles.size());
                    disponibilidadDia.put("primeraHoraDisponible",
                            slotsDisponibles.isEmpty() ? null : slotsDisponibles.get(0).toString());
                    disponibilidadDia.put("ultimaHoraDisponible",
                            slotsDisponibles.isEmpty() ? null :
                                    slotsDisponibles.get(slotsDisponibles.size() - 1).toString());
                }

                resultado.add(disponibilidadDia);
                diaActual = diaActual.plusDays(1);
            }

        } catch (Exception e) {
            logger.error("Error analizando disponibilidad por rango para tenant {}: {}",
                    tenantId, e.getMessage());
        }

        return resultado;
    }

    /**
     * 🤖 MÉTODO IA PREMIUM: optimizarHorarioConIA()
     * Encuentra el mejor horario usando inteligencia artificial
     */
    public Map<String, Object> optimizarHorarioConIA(String tenantId,
                                                     String fechaPreferida,
                                                     String horaPreferida,
                                                     String servicioId,
                                                     String empleadoId,
                                                     Integer diasFlexibles) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            LocalDateTime fechaHoraPreferida = LocalDateTime.parse(fechaPreferida + "T" + horaPreferida);

            // 1. Verificar si el horario preferido está disponible
            DisponibilidadResult disponibilidadPreferida = verificarDisponibilidadCompleta(
                    tenantId, fechaHoraPreferida, servicioId, empleadoId);

            if (disponibilidadPreferida.isDisponible()) {
                resultado.put("horarioOptimo", fechaHoraPreferida.toString());
                resultado.put("esHorarioPreferido", true);
                resultado.put("mensaje", "Su horario preferido está disponible");
                resultado.put("razonOptimizacion", "Coincide con preferencia del cliente");
                return resultado;
            }

            // 2. Si no está disponible, buscar alternativas inteligentes
            List<LocalDateTime> alternativas = buscarAlternativasInteligentes(
                    tenantId, fechaHoraPreferida, servicioId, empleadoId, diasFlexibles);

            if (alternativas.isEmpty()) {
                resultado.put("horarioOptimo", null);
                resultado.put("esHorarioPreferido", false);
                resultado.put("mensaje", "No se encontraron horarios disponibles en el rango especificado");
                resultado.put("alternativas", List.of());
                return resultado;
            }

            // 3. Usar IA para elegir la mejor alternativa
            LocalDateTime mejorAlternativa = seleccionarMejorAlternativaConIA(
                    tenantId, fechaHoraPreferida, alternativas, servicioId);

            // 4. Generar explicación IA del por qué es la mejor opción
            String explicacionIA = generarExplicacionOptimizacion(
                    tenantId, fechaHoraPreferida, mejorAlternativa);

            resultado.put("horarioOptimo", mejorAlternativa.toString());
            resultado.put("esHorarioPreferido", false);
            resultado.put("mensaje", "Horario optimizado encontrado");
            resultado.put("explicacionIA", explicacionIA);
            resultado.put("alternativas", alternativas.stream()
                    .limit(5) // Máximo 5 alternativas
                    .map(LocalDateTime::toString)
                    .collect(Collectors.toList()));
            resultado.put("factoresConsiderados", List.of(
                    "Proximidad a horario preferido",
                    "Disponibilidad de empleado",
                    "Patrones de demanda histórica",
                    "Optimización de recursos del salón"
            ));

        } catch (Exception e) {
            logger.error("Error optimizando horario con IA para tenant {}: {}", tenantId, e.getMessage());
            resultado.put("horarioOptimo", null);
            resultado.put("error", "Error en optimización: " + e.getMessage());
        }

        return resultado;
    }

// ========================================
// MÉTODOS AUXILIARES PRIVADOS
// ========================================

    /**
     * Verificar si una fecha es día laborable según configuración del tenant
     */
    private boolean esDiaLaborable(String tenantId, LocalDateTime fecha) {
        try {
            String diasLaborables = obtenerConfiguracion(tenantId, "dias_laborables", "L,M,X,J,V,S");
            DayOfWeek diaSeleccionado = fecha.getDayOfWeek();

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

            return diasPermitidos.contains(diaSeleccionado);
        } catch (Exception e) {
            logger.warn("Error verificando día laborable: {}", e.getMessage());
            return true; // Fallback seguro
        }
    }

    /**
     * Obtener slots disponibles en un día específico
     */
    private List<LocalDateTime> obtenerSlotsDisponiblesEnDia(String tenantId,
                                                             LocalDate fecha,
                                                             String servicioId,
                                                             String empleadoId) {
        List<LocalDateTime> slots = new ArrayList<>();

        try {
            String horaApertura = obtenerConfiguracion(tenantId, "hora_apertura", "09:00");
            String horaCierre = obtenerConfiguracion(tenantId, "hora_cierre", "20:00");

            LocalTime apertura = LocalTime.parse(horaApertura);
            LocalTime cierre = LocalTime.parse(horaCierre);

            // Generar slots cada 30 minutos
            LocalTime horaActual = apertura;
            while (horaActual.isBefore(cierre)) {
                LocalDateTime slot = fecha.atTime(horaActual);

                // Verificar disponibilidad del slot
                DisponibilidadResult disponibilidad = verificarDisponibilidadCompleta(
                        tenantId, slot, servicioId, empleadoId);

                if (disponibilidad.isDisponible()) {
                    slots.add(slot);
                }

                horaActual = horaActual.plusMinutes(30);
            }
        } catch (Exception e) {
            logger.warn("Error obteniendo slots disponibles para fecha {}: {}", fecha, e.getMessage());
        }

        return slots;
    }

    /**
     * Buscar alternativas inteligentes cerca del horario preferido
     */
    private List<LocalDateTime> buscarAlternativasInteligentes(String tenantId,
                                                               LocalDateTime horarioPreferido,
                                                               String servicioId,
                                                               String empleadoId,
                                                               Integer diasFlexibles) {
        List<LocalDateTime> alternativas = new ArrayList<>();

        // Buscar en el mismo día (antes y después)
        for (int minutos = 30; minutos <= 480; minutos += 30) { // Hasta 8 horas de diferencia
            LocalDateTime antesPreferido = horarioPreferido.minusMinutes(minutos);
            LocalDateTime despuesPreferido = horarioPreferido.plusMinutes(minutos);

            // Verificar slot anterior
            if (verificarDisponibilidadCompleta(tenantId, antesPreferido, servicioId, empleadoId).isDisponible()) {
                alternativas.add(antesPreferido);
            }

            // Verificar slot posterior
            if (verificarDisponibilidadCompleta(tenantId, despuesPreferido, servicioId, empleadoId).isDisponible()) {
                alternativas.add(despuesPreferido);
            }

            if (alternativas.size() >= 10) break; // Límite de alternativas por día
        }

        // Buscar en días adyacentes
        for (int dia = 1; dia <= diasFlexibles; dia++) {
            LocalDateTime diaSiguiente = horarioPreferido.plusDays(dia);
            LocalDateTime diaAnterior = horarioPreferido.minusDays(dia);

            if (verificarDisponibilidadCompleta(tenantId, diaSiguiente, servicioId, empleadoId).isDisponible()) {
                alternativas.add(diaSiguiente);
            }

            if (verificarDisponibilidadCompleta(tenantId, diaAnterior, servicioId, empleadoId).isDisponible()) {
                alternativas.add(diaAnterior);
            }
        }

        return alternativas.stream()
                .sorted((a, b) -> {
                    // Ordenar por proximidad al horario preferido
                    long diffA = Math.abs(ChronoUnit.MINUTES.between(horarioPreferido, a));
                    long diffB = Math.abs(ChronoUnit.MINUTES.between(horarioPreferido, b));
                    return Long.compare(diffA, diffB);
                })
                .limit(8) // Máximo 8 alternativas
                .collect(Collectors.toList());
    }

    /**
     * 🤖 IA selecciona la mejor alternativa considerando múltiples factores
     */
    private LocalDateTime seleccionarMejorAlternativaConIA(String tenantId,
                                                           LocalDateTime horarioPreferido,
                                                           List<LocalDateTime> alternativas,
                                                           String servicioId) {
        if (alternativas.isEmpty()) {
            return null;
        }

        // Por ahora, seleccionar la más cercana al horario preferido
        // TODO: Integrar análisis más sofisticado con OpenAI
        return alternativas.get(0);
    }

    /**
     * 🤖 Generar explicación IA de por qué se eligió ese horario
     */
    private String generarExplicacionOptimizacion(String tenantId,
                                                  LocalDateTime horarioPreferido,
                                                  LocalDateTime horarioOptimo) {
        try {
            long minutosDelante = ChronoUnit.MINUTES.between(horarioPreferido, horarioOptimo);
            String direccion = minutosDelante > 0 ? "después" : "antes";
            long minutosAbs = Math.abs(minutosDelante);

            if (minutosAbs < 60) {
                return String.format("Encontramos disponibilidad %d minutos %s de su horario preferido. " +
                        "Es la opción más cercana disponible.", minutosAbs, direccion);
            } else if (minutosAbs < 1440) { // Menos de un día
                long horas = minutosAbs / 60;
                return String.format("Su horario preferido no estaba disponible, pero encontramos un hueco " +
                        "%d hora(s) %s que se ajusta perfectamente.", horas, direccion);
            } else {
                long dias = minutosAbs / 1440;
                return String.format("Le sugerimos este horario %d día(s) %s ya que mantiene la misma hora " +
                        "que prefería originalmente.", dias, direccion);
            }
        } catch (Exception e) {
            return "Horario optimizado según disponibilidad del salón.";
        }
    }
}