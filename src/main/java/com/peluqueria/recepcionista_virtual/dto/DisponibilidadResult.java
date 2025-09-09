package com.peluqueria.recepcionista_virtual.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO mejorado para resultados de verificación de disponibilidad
 *
 * MULTITENANT: Compatible con configuraciones específicas por tenant
 * ZERO HARDCODING: Mensajes configurables, sin textos fijos
 * OpenAI CEREBRO: Preparado para análisis inteligente de conflictos
 */
public class DisponibilidadResult {

    private boolean disponible;
    private String mensaje;

    // ========================================
    // NUEVOS CAMPOS PARA ANÁLISIS AVANZADO
    // ========================================

    private List<LocalDate> fechasAlternativas = new ArrayList<>();
    private LocalTime horarioDisponibleInicio;
    private LocalTime horarioDisponibleFin;
    private String tipoRestriccion;

    // NUEVOS: Para análisis de conflictos específicos
    private List<String> conflictosDetectados = new ArrayList<>();
    private String empleadoAlternativo;
    private String servicioAlternativo;
    private Integer minutosHastaProximoSlot;

    // NUEVOS: Para OpenAI cerebro
    private String sugerenciaIA;
    private Double confianzaSugerencia;
    private List<String> razonesConflicto = new ArrayList<>();

    // NUEVOS: Para UX mejorado
    private String colorIndicador; // Para frontend
    private String iconoTipo; // Para frontend
    private Boolean requiereConfirmacionUsuario = false;

    // ========================================
    // CONSTRUCTORES
    // ========================================

    public DisponibilidadResult() {}

    public DisponibilidadResult(boolean disponible, String mensaje) {
        this.disponible = disponible;
        this.mensaje = mensaje;
    }

    // ========================================
    // FACTORY METHODS MEJORADOS
    // ========================================

    /**
     * Crear resultado disponible básico
     */
    public static DisponibilidadResult disponible() {
        DisponibilidadResult result = new DisponibilidadResult();
        result.setDisponible(true);
        result.setColorIndicador("#28a745"); // Verde
        result.setIconoTipo("check-circle");
        return result;
    }

    /**
     * Crear resultado disponible con restricciones (horario reducido)
     */
    public static DisponibilidadResult disponibleConRestricciones(LocalTime inicio, LocalTime fin, String motivo) {
        DisponibilidadResult result = new DisponibilidadResult();
        result.setDisponible(true);
        result.setHorarioDisponibleInicio(inicio);
        result.setHorarioDisponibleFin(fin);
        result.setTipoRestriccion("HORARIO_REDUCIDO");
        result.setMensaje(motivo);
        result.setColorIndicador("#ffc107"); // Amarillo
        result.setIconoTipo("clock");
        return result;
    }

    /**
     * Crear resultado no disponible básico
     */
    public static DisponibilidadResult noDisponible(String mensaje) {
        DisponibilidadResult result = new DisponibilidadResult();
        result.setDisponible(false);
        result.setMensaje(mensaje);
        result.setColorIndicador("#dc3545"); // Rojo
        result.setIconoTipo("x-circle");
        return result;
    }

    /**
     * Crear resultado no disponible con conflicto específico
     */
    public static DisponibilidadResult noDisponibleConConflicto(String mensaje, String tipoConflicto) {
        DisponibilidadResult result = noDisponible(mensaje);
        result.setTipoRestriccion(tipoConflicto);
        result.getConflictosDetectados().add(tipoConflicto);
        return result;
    }

    /**
     * Crear resultado con análisis inteligente para OpenAI
     */
    public static DisponibilidadResult conAnalisisIA(boolean disponible, String sugerenciaIA,
                                                     Double confianza, List<String> razones) {
        DisponibilidadResult result = new DisponibilidadResult();
        result.setDisponible(disponible);
        result.setSugerenciaIA(sugerenciaIA);
        result.setConfianzaSugerencia(confianza);
        result.setRazonesConflicto(razones != null ? razones : new ArrayList<>());
        result.setColorIndicador(disponible ? "#28a745" : "#dc3545");
        result.setIconoTipo(disponible ? "check-circle" : "brain");
        return result;
    }

    // ========================================
    // MÉTODOS DE UTILIDAD
    // ========================================

    /**
     * Agregar fecha alternativa de forma segura
     */
    public DisponibilidadResult agregarFechaAlternativa(LocalDate fecha) {
        if (fecha != null && !this.fechasAlternativas.contains(fecha)) {
            this.fechasAlternativas.add(fecha);
        }
        return this;
    }

    /**
     * Agregar conflicto detectado
     */
    public DisponibilidadResult agregarConflicto(String conflicto) {
        if (conflicto != null && !this.conflictosDetectados.contains(conflicto)) {
            this.conflictosDetectados.add(conflicto);
        }
        return this;
    }

    /**
     * Agregar razón de conflicto para análisis IA
     */
    public DisponibilidadResult agregarRazonConflicto(String razon) {
        if (razon != null && !this.razonesConflicto.contains(razon)) {
            this.razonesConflicto.add(razon);
        }
        return this;
    }

    /**
     * Verificar si tiene alternativas disponibles
     */
    public boolean tieneAlternativas() {
        return !fechasAlternativas.isEmpty() ||
                empleadoAlternativo != null ||
                servicioAlternativo != null ||
                minutosHastaProximoSlot != null;
    }

    /**
     * Verificar si es una restricción menor (horario reducido)
     */
    public boolean esRestriccionMenor() {
        return disponible && "HORARIO_REDUCIDO".equals(tipoRestriccion);
    }

    /**
     * Verificar si requiere análisis IA adicional
     */
    public boolean requiereAnalisisIA() {
        return !disponible &&
                (sugerenciaIA == null || conflictosDetectados.size() > 1);
    }

    // ========================================
    // GETTERS Y SETTERS
    // ========================================

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public List<LocalDate> getFechasAlternativas() {
        return fechasAlternativas;
    }

    public void setFechasAlternativas(List<LocalDate> fechasAlternativas) {
        this.fechasAlternativas = fechasAlternativas != null ? fechasAlternativas : new ArrayList<>();
    }

    public LocalTime getHorarioDisponibleInicio() {
        return horarioDisponibleInicio;
    }

    public void setHorarioDisponibleInicio(LocalTime horarioDisponibleInicio) {
        this.horarioDisponibleInicio = horarioDisponibleInicio;
    }

    public LocalTime getHorarioDisponibleFin() {
        return horarioDisponibleFin;
    }

    public void setHorarioDisponibleFin(LocalTime horarioDisponibleFin) {
        this.horarioDisponibleFin = horarioDisponibleFin;
    }

    public String getTipoRestriccion() {
        return tipoRestriccion;
    }

    public void setTipoRestriccion(String tipoRestriccion) {
        this.tipoRestriccion = tipoRestriccion;
    }

    public List<String> getConflictosDetectados() {
        return conflictosDetectados;
    }

    public void setConflictosDetectados(List<String> conflictosDetectados) {
        this.conflictosDetectados = conflictosDetectados != null ? conflictosDetectados : new ArrayList<>();
    }

    public String getEmpleadoAlternativo() {
        return empleadoAlternativo;
    }

    public void setEmpleadoAlternativo(String empleadoAlternativo) {
        this.empleadoAlternativo = empleadoAlternativo;
    }

    public String getServicioAlternativo() {
        return servicioAlternativo;
    }

    public void setServicioAlternativo(String servicioAlternativo) {
        this.servicioAlternativo = servicioAlternativo;
    }

    public Integer getMinutosHastaProximoSlot() {
        return minutosHastaProximoSlot;
    }

    public void setMinutosHastaProximoSlot(Integer minutosHastaProximoSlot) {
        this.minutosHastaProximoSlot = minutosHastaProximoSlot;
    }

    public String getSugerenciaIA() {
        return sugerenciaIA;
    }

    public void setSugerenciaIA(String sugerenciaIA) {
        this.sugerenciaIA = sugerenciaIA;
    }

    public Double getConfianzaSugerencia() {
        return confianzaSugerencia;
    }

    public void setConfianzaSugerencia(Double confianzaSugerencia) {
        this.confianzaSugerencia = confianzaSugerencia;
    }

    public List<String> getRazonesConflicto() {
        return razonesConflicto;
    }

    public void setRazonesConflicto(List<String> razonesConflicto) {
        this.razonesConflicto = razonesConflicto != null ? razonesConflicto : new ArrayList<>();
    }

    public String getColorIndicador() {
        return colorIndicador;
    }

    public void setColorIndicador(String colorIndicador) {
        this.colorIndicador = colorIndicador;
    }

    public String getIconoTipo() {
        return iconoTipo;
    }

    public void setIconoTipo(String iconoTipo) {
        this.iconoTipo = iconoTipo;
    }

    public Boolean getRequiereConfirmacionUsuario() {
        return requiereConfirmacionUsuario;
    }

    public void setRequiereConfirmacionUsuario(Boolean requiereConfirmacionUsuario) {
        this.requiereConfirmacionUsuario = requiereConfirmacionUsuario;
    }

    @Override
    public String toString() {
        return "DisponibilidadResult{" +
                "disponible=" + disponible +
                ", mensaje='" + mensaje + '\'' +
                ", tipoRestriccion='" + tipoRestriccion + '\'' +
                ", conflictosDetectados=" + conflictosDetectados.size() +
                ", tieneAlternativas=" + tieneAlternativas() +
                ", sugerenciaIA='" + sugerenciaIA + '\'' +
                '}';
    }
}