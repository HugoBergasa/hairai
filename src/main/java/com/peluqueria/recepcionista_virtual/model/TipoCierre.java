package com.peluqueria.recepcionista_virtual.model;

/**
 * Enum que define los diferentes tipos de cierres especiales
 * que puede tener un salón/spa
 *
 * ZERO HARDCODING: Solo identificadores técnicos puros
 * MULTITENANT: Las descripciones, iconos y textos vienen de BD o se generan con OpenAI por tenant
 * OpenAI CEREBRO: Los mensajes se generan dinámicamente según el estilo de cada negocio
 */
public enum TipoCierre {

    // ✅ IDENTIFICADORES TÉCNICOS PUROS - SIN TEXTOS HARDCODEADOS
    CERRADO_COMPLETO,
    HORARIO_REDUCIDO,
    SOLO_EMERGENCIAS,
    EMPLEADO_AUSENTE,
    SERVICIO_NO_DISPONIBLE;

    // ========================================
    // MÉTODOS TÉCNICOS (sin hardcoding)
    // ========================================

    /**
     * Obtiene una descripción técnica del comportamiento (para logs y debug)
     * Nota: Estas son descripciones técnicas en inglés para logs, no para usuarios
     */
    public String getDescripcionTecnica() {
        switch (this) {
            case CERRADO_COMPLETO:
                return "blocks_all_appointments";

            case HORARIO_REDUCIDO:
                return "restricts_to_specified_hours";

            case SOLO_EMERGENCIAS:
                return "allows_only_emergency_appointments";

            case EMPLEADO_AUSENTE:
                return "blocks_specific_employees";

            case SERVICIO_NO_DISPONIBLE:
                return "blocks_specific_services";

            default:
                return "unknown_behavior";
        }
    }

    /**
     * Verifica si este tipo de cierre requiere horarios específicos
     */
    public boolean requiereHorarios() {
        return this == HORARIO_REDUCIDO;
    }

    /**
     * Verifica si este tipo de cierre requiere especificar empleados
     */
    public boolean requiereEmpleados() {
        return this == EMPLEADO_AUSENTE;
    }

    /**
     * Verifica si este tipo de cierre requiere especificar servicios
     */
    public boolean requiereServicios() {
        return this == SERVICIO_NO_DISPONIBLE;
    }

    /**
     * Verifica si este tipo bloquea completamente las citas
     */
    public boolean bloqueaCompletamente() {
        return this == CERRADO_COMPLETO;
    }

    /**
     * Verifica si requiere horarios específicos
     */
    public boolean requiereHorariosEspecificos() {
        return this == HORARIO_REDUCIDO;
    }

    /**
     * Verifica si solo permite emergencias
     */
    public boolean soloEmergencias() {
        return this == SOLO_EMERGENCIAS;
    }

    /**
     * Verifica si afecta empleados específicos
     */
    public boolean afectaEmpleadosEspecificos() {
        return this == EMPLEADO_AUSENTE;
    }

    /**
     * Verifica si afecta servicios específicos
     */
    public boolean afectaServiciosEspecificos() {
        return this == SERVICIO_NO_DISPONIBLE;
    }

    /**
     * Convierte un string a TipoCierre (solo acepta nombres técnicos)
     * Ejemplo: "CERRADO_COMPLETO" -> TipoCierre.CERRADO_COMPLETO
     */
    public static TipoCierre fromString(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return null;
        }

        try {
            return TipoCierre.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // No hacer búsquedas por descripción - solo nombres técnicos
        }
    }

    /**
     * Retorna el nombre técnico del enum para logging y APIs
     * Ejemplo: TipoCierre.CERRADO_COMPLETO.toString() -> "CERRADO_COMPLETO"
     */
    @Override
    public String toString() {
        return name(); // Retorna el nombre técnico del enum
    }
}