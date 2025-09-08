package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.TipoCierre;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class DisponibilidadResult {

    private boolean disponible;

    // ZERO HARDCODING: Este mensaje viene de BD o es generado por OpenAI
    private String mensaje;

    private TipoCierre tipoRestriccion;
    private LocalTime horarioDisponibleInicio;
    private LocalTime horarioDisponibleFin;
    private List<String> empleadosDisponibles;
    private List<String> serviciosDisponibles;
    private List<LocalDate> fechasAlternativas;

    // Constructores
    public DisponibilidadResult() {}

    public DisponibilidadResult(boolean disponible, String mensaje) {
        this.disponible = disponible;
        this.mensaje = mensaje;
    }

    // Factory methods técnicos (sin hardcoding)
    public static DisponibilidadResult disponible() {
        return new DisponibilidadResult(true, null); // Sin mensaje hardcodeado
    }

    public static DisponibilidadResult noDisponible(String mensajeDinamico) {
        return new DisponibilidadResult(false, mensajeDinamico); // Mensaje viene de BD o OpenAI
    }

    public static DisponibilidadResult horarioLimitado(LocalTime inicio, LocalTime fin, String mensajeDinamico) {
        DisponibilidadResult result = new DisponibilidadResult(true, mensajeDinamico);
        result.setHorarioDisponibleInicio(inicio);
        result.setHorarioDisponibleFin(fin);
        return result;
    }

    // Getters y Setters
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public TipoCierre getTipoRestriccion() { return tipoRestriccion; }
    public void setTipoRestriccion(TipoCierre tipoRestriccion) { this.tipoRestriccion = tipoRestriccion; }

    public LocalTime getHorarioDisponibleInicio() { return horarioDisponibleInicio; }
    public void setHorarioDisponibleInicio(LocalTime horarioDisponibleInicio) { this.horarioDisponibleInicio = horarioDisponibleInicio; }

    public LocalTime getHorarioDisponibleFin() { return horarioDisponibleFin; }
    public void setHorarioDisponibleFin(LocalTime horarioDisponibleFin) { this.horarioDisponibleFin = horarioDisponibleFin; }

    public List<String> getEmpleadosDisponibles() { return empleadosDisponibles; }
    public void setEmpleadosDisponibles(List<String> empleadosDisponibles) { this.empleadosDisponibles = empleadosDisponibles; }

    public List<String> getServiciosDisponibles() { return serviciosDisponibles; }
    public void setServiciosDisponibles(List<String> serviciosDisponibles) { this.serviciosDisponibles = serviciosDisponibles; }

    public List<LocalDate> getFechasAlternativas() { return fechasAlternativas; }
    public void setFechasAlternativas(List<LocalDate> fechasAlternativas) { this.fechasAlternativas = fechasAlternativas; }
}
