package com.peluqueria.recepcionista_virtual.model;

/**
 * Enum que define los diferentes tipos de cierres especiales
 * que puede tener un salón/spa
 *
 * MULTITENANT: Este enum es compartido por todos los tenants
 * ZERO HARDCODING: Los valores se usan dinámicamente desde BD
 */
public enum TipoCierre {

    CERRADO_COMPLETO("Cerrado todo el día", "🚫",
            "El salón estará completamente cerrado"),

    HORARIO_REDUCIDO("Horario reducido", "⏰",
            "El salón tendrá horario especial limitado"),

    SOLO_EMERGENCIAS("Solo emergencias", "🆘",
            "Solo se atenderán casos de emergencia"),

    EMPLEADO_AUSENTE("Empleado ausente", "👤",
            "Un empleado específico no estará disponible"),

    SERVICIO_NO_DISPONIBLE("Servicio no disponible", "✂️",
            "Un servicio específico no estará disponible");

    private final String descripcion;
    private final String icono;
    private final String explicacion;

    TipoCierre(String descripcion, String icono, String explicacion) {
        this.descripcion = descripcion;
        this.icono = icono;
        this.explicacion = explicacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getIcono() {
        return icono;
    }

    public String getExplicacion() {
        return explicacion;
    }

    public String getDescripcionCompleta() {
        return icono + " " + descripcion;
    }

    /**
     * Obtiene el color CSS apropiado para cada tipo de cierre
     */
    public String getColorCSS() {
        switch (this) {
            case CERRADO_COMPLETO:
                return "red";
            case HORARIO_REDUCIDO:
                return "yellow";
            case SOLO_EMERGENCIAS:
                return "orange";
            case EMPLEADO_AUSENTE:
                return "blue";
            case SERVICIO_NO_DISPONIBLE:
                return "purple";
            default:
                return "gray";
        }
    }

    /**
     * Obtiene la clase CSS de Tailwind apropiada para cada tipo
     */
    public String getTailwindClass() {
        switch (this) {
            case CERRADO_COMPLETO:
                return "bg-red-100 text-red-800 border-red-200";
            case HORARIO_REDUCIDO:
                return "bg-yellow-100 text-yellow-800 border-yellow-200";
            case SOLO_EMERGENCIAS:
                return "bg-orange-100 text-orange-800 border-orange-200";
            case EMPLEADO_AUSENTE:
                return "bg-blue-100 text-blue-800 border-blue-200";
            case SERVICIO_NO_DISPONIBLE:
                return "bg-purple-100 text-purple-800 border-purple-200";
            default:
                return "bg-gray-100 text-gray-800 border-gray-200";
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
     * Verifica si este tipo de cierre bloquea completamente las citas
     */
    public boolean bloqueaCompletamente() {
        return this == CERRADO_COMPLETO;
    }

    /**
     * Obtiene un mensaje por defecto para la IA basado en el tipo de cierre
     */
    public String getMensajeDefaultIA() {
        switch (this) {
            case CERRADO_COMPLETO:
                return "Lo siento, estamos cerrados ese día. ¿Te gustaría agendar para otro día?";

            case HORARIO_REDUCIDO:
                return "Ese día tenemos horario especial. ¿Te confirmo las horas disponibles?";

            case SOLO_EMERGENCIAS:
                return "Ese día solo atendemos emergencias. ¿Es tu caso urgente o prefieres otro día?";

            case EMPLEADO_AUSENTE:
                return "Ese profesional no estará disponible ese día. ¿Te gustaría con otro de nuestros especialistas?";

            case SERVICIO_NO_DISPONIBLE:
                return "Ese servicio no estará disponible ese día. ¿Te interesa algún otro de nuestros tratamientos?";

            default:
                return "No tenemos disponibilidad ese día. ¿Te viene bien otra fecha?";
        }
    }

    /**
     * Convierte un string a TipoCierre (útil para APIs)
     */
    public static TipoCierre fromString(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return null;
        }

        try {
            return TipoCierre.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Si no encuentra match exacto, busca por descripción
            for (TipoCierre tipoCierre : values()) {
                if (tipoCierre.descripcion.toLowerCase().contains(tipo.toLowerCase())) {
                    return tipoCierre;
                }
            }
            return null;
        }
    }

    @Override
    public String toString() {
        return descripcion;
    }
}