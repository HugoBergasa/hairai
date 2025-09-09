package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.Empleado;
import com.peluqueria.recepcionista_virtual.model.Servicio;
import com.peluqueria.recepcionista_virtual.repository.EmpleadoRepository;
import com.peluqueria.recepcionista_virtual.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;

@Service
public class EmpleadoServicioValidationService {

    private static final Logger logger = LoggerFactory.getLogger(EmpleadoServicioValidationService.class);

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private TenantConfigService tenantConfigService;

    /**
     * CRITICO: Validar que empleado puede realizar servicio especifico
     */
    public void validarEmpleadoAutorizadoParaServicio(String empleadoId, String servicioId, String tenantId) {
        if (empleadoId == null || servicioId == null) return;

        // 1. Verificar que empleado y servicio pertenecen al tenant
        validarEmpleadoYServicioPertenecenTenant(empleadoId, servicioId, tenantId);

        // 2. Obtener política de asignación del tenant
        String politicaAsignacion = tenantConfigService.obtenerValor(tenantId,
                "politica_asignacion_empleados", "CUALQUIER_ACTIVO");

        if ("CUALQUIER_ACTIVO".equals(politicaAsignacion)) {
            // Política permisiva - cualquier empleado activo puede hacer cualquier servicio
            return;
        }

        // 3. Para políticas restrictivas futuras
        if ("CONFIGURACION_ESPECIFICA".equals(politicaAsignacion)) {
            // TODO: Cuando exista empleados_servicios repository
            logger.info("Política restrictiva no implementada aún - usando permisiva");
        }
    }

    /**
     * CRITICO: Validar dias laborables especificos del empleado
     */
    public void validarEmpleadoTrabajaEnFecha(String empleadoId, LocalDate fecha, String tenantId) {
        if (empleadoId == null) return;

        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado"));

        validarEmpleadoPerteneceTenant(empleado, tenantId);

        if (!empleado.getActivo()) {
            throw new RuntimeException(
                    obtenerMensajePersonalizado(tenantId, "empleado_inactivo",
                            "El empleado seleccionado no está disponible")
            );
        }

        // Validar días laborables específicos del empleado
        if (empleado.getDiasTrabajo() != null && !empleado.getDiasTrabajo().trim().isEmpty()) {
            if (!empleadoTrabajaEnDia(empleado, fecha)) {
                throw new RuntimeException(
                        String.format("El empleado %s no trabaja los %s",
                                empleado.getNombre(), formatearDiaSemana(fecha))
                );
            }
        }

        // Validar configuración de validación de días
        String validarDias = tenantConfigService.obtenerValor(tenantId,
                "validar_dias_trabajo_empleado", "true");
        if ("true".equals(validarDias)) {
            validarHorarioTrabajoEmpleado(empleado, fecha, tenantId);
        }
    }

    /**
     * CRITICO: Validar capacidad personalizada por tipo de servicio
     */
    public void validarCapacidadServicioEspecifico(String tenantId, String servicioId,
                                                   LocalDate fecha) {
        if (servicioId == null) return;

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new EntityNotFoundException("Servicio no encontrado"));

        // ZERO HARDCODING: Capacidad específica por servicio
        String nombreServicio = servicio.getNombre().toLowerCase().replace(" ", "_");
        String capacidadConfig = tenantConfigService.obtenerValor(tenantId,
                "capacidad_" + nombreServicio, null);

        if (capacidadConfig != null) {
            try {
                int capacidadMaxima = Integer.parseInt(capacidadConfig);
                logger.info("Validación de capacidad específica para servicio {}: máximo {}",
                        servicio.getNombre(), capacidadMaxima);

                // TODO: Implementar conteo de servicios específicos cuando esté disponible
                // Por ahora solo logear la configuración encontrada

            } catch (NumberFormatException e) {
                logger.warn("Configuración de capacidad inválida para servicio {}: {}",
                        servicio.getNombre(), capacidadConfig);
            }
        }
    }

    /**
     * CRITICO: Validar disponibilidad general del empleado
     */
    public void validarDisponibilidadGeneralEmpleado(String empleadoId, String tenantId) {
        if (empleadoId == null) return;

        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado"));

        if (!empleado.getTenant().getId().equals(tenantId)) {
            throw new SecurityException("Empleado no pertenece al tenant");
        }

        if (!empleado.getActivo()) {
            throw new RuntimeException(
                    obtenerMensajePersonalizado(tenantId, "empleado_inactivo",
                            "Empleado no disponible")
            );
        }
    }

    // ========================================
    // METODOS AUXILIARES PRIVADOS
    // ========================================

    private void validarEmpleadoYServicioPertenecenTenant(String empleadoId, String servicioId, String tenantId) {
        // Validar empleado
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado"));

        if (!empleado.getTenant().getId().equals(tenantId)) {
            throw new SecurityException("Empleado no pertenece al tenant");
        }

        // Validar servicio
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new EntityNotFoundException("Servicio no encontrado"));

        if (!servicio.getTenant().getId().equals(tenantId)) {
            throw new SecurityException("Servicio no pertenece al tenant");
        }
    }

    private void validarEmpleadoPerteneceTenant(Empleado empleado, String tenantId) {
        if (!empleado.getTenant().getId().equals(tenantId)) {
            throw new SecurityException("Empleado no pertenece al tenant especificado");
        }
    }

    private void validarHorarioTrabajoEmpleado(Empleado empleado, LocalDate fecha, String tenantId) {
        String validarHorario = tenantConfigService.obtenerValor(tenantId,
                "validar_horario_empleado", "false");

        if (!"true".equals(validarHorario)) {
            return; // Validación desactivada
        }

        if (empleado.getHoraEntrada() == null || empleado.getHoraSalida() == null) {
            return; // Sin restricción horaria específica
        }

        try {
            LocalTime entrada = LocalTime.parse(empleado.getHoraEntrada());
            LocalTime salida = LocalTime.parse(empleado.getHoraSalida());

            logger.debug("Empleado {} trabaja de {} a {} los {}",
                    empleado.getNombre(), entrada, salida, empleado.getDiasTrabajo());

            // La validación de hora específica se hará en el contexto de la cita

        } catch (DateTimeParseException e) {
            logger.warn("Horario del empleado {} tiene formato inválido: {}-{}",
                    empleado.getId(), empleado.getHoraEntrada(), empleado.getHoraSalida());
        }
    }

    private boolean empleadoTrabajaEnDia(Empleado empleado, LocalDate fecha) {
        String diasTrabajo = empleado.getDiasTrabajo(); // "L,M,X,J,V"
        if (diasTrabajo == null || diasTrabajo.trim().isEmpty()) {
            return true; // Sin restricción específica
        }

        DayOfWeek dia = fecha.getDayOfWeek();
        String diaCorto = obtenerDiaCorto(dia);

        return diasTrabajo.contains(diaCorto);
    }

    private String obtenerDiaCorto(DayOfWeek dia) {
        switch (dia) {
            case MONDAY: return "L";
            case TUESDAY: return "M";
            case WEDNESDAY: return "X";
            case THURSDAY: return "J";
            case FRIDAY: return "V";
            case SATURDAY: return "S";
            case SUNDAY: return "D";
            default: return "";
        }
    }

    private String formatearDiaSemana(LocalDate fecha) {
        return fecha.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, new Locale("es"));
    }

    private String obtenerMensajePersonalizado(String tenantId, String clave, String fallback) {
        try {
            String mensaje = tenantConfigService.obtenerValor(tenantId, "mensaje_" + clave, null);
            if (mensaje != null && !mensaje.trim().isEmpty()) {
                return mensaje;
            }

            return fallback;

        } catch (Exception e) {
            logger.error("Error obteniendo mensaje personalizado para clave {}: {}", clave, e.getMessage());
            return fallback;
        }
    }
}