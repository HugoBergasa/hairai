package com.peluqueria.recepcionista_virtual.dto;

import com.peluqueria.recepcionista_virtual.model.TipoCierre;

public class EstadisticasCierreDTO {

    // Contadores técnicos (sin texto hardcodeado)
    private long totalCierres;
    private long cierresCompletos;
    private long horariosReducidos;
    private long empleadosAusentes;
    private long serviciosNoDisponibles;
    private long soloEmergencias;

    // Métricas de tiempo
    private int diasCerradosEsteMes;
    private int diasCerradosProximoMes;

    // Análisis dinámico
    private String motivoMasFrecuente; // Viene de BD, no hardcodeado
    private TipoCierre tipoMasFrecuente; // Dato técnico

    // Análisis financiero (si aplica)
    private Double impactoEstimadoIngresos;
    private Integer citasCanceladasPorCierres;

    // Constructores
    public EstadisticasCierreDTO() {}

    // Getters y Setters
    public long getTotalCierres() { return totalCierres; }
    public void setTotalCierres(long totalCierres) { this.totalCierres = totalCierres; }

    public long getCierresCompletos() { return cierresCompletos; }
    public void setCierresCompletos(long cierresCompletos) { this.cierresCompletos = cierresCompletos; }

    public long getHorariosReducidos() { return horariosReducidos; }
    public void setHorariosReducidos(long horariosReducidos) { this.horariosReducidos = horariosReducidos; }

    public long getEmpleadosAusentes() { return empleadosAusentes; }
    public void setEmpleadosAusentes(long empleadosAusentes) { this.empleadosAusentes = empleadosAusentes; }

    public long getServiciosNoDisponibles() { return serviciosNoDisponibles; }
    public void setServiciosNoDisponibles(long serviciosNoDisponibles) { this.serviciosNoDisponibles = serviciosNoDisponibles; }

    public long getSoloEmergencias() { return soloEmergencias; }
    public void setSoloEmergencias(long soloEmergencias) { this.soloEmergencias = soloEmergencias; }

    public int getDiasCerradosEsteMes() { return diasCerradosEsteMes; }
    public void setDiasCerradosEsteMes(int diasCerradosEsteMes) { this.diasCerradosEsteMes = diasCerradosEsteMes; }

    public int getDiasCerradosProximoMes() { return diasCerradosProximoMes; }
    public void setDiasCerradosProximoMes(int diasCerradosProximoMes) { this.diasCerradosProximoMes = diasCerradosProximoMes; }

    public String getMotivoMasFrecuente() { return motivoMasFrecuente; }
    public void setMotivoMasFrecuente(String motivoMasFrecuente) { this.motivoMasFrecuente = motivoMasFrecuente; }

    public TipoCierre getTipoMasFrecuente() { return tipoMasFrecuente; }
    public void setTipoMasFrecuente(TipoCierre tipoMasFrecuente) { this.tipoMasFrecuente = tipoMasFrecuente; }

    public Double getImpactoEstimadoIngresos() { return impactoEstimadoIngresos; }
    public void setImpactoEstimadoIngresos(Double impactoEstimadoIngresos) { this.impactoEstimadoIngresos = impactoEstimadoIngresos; }

    public Integer getCitasCanceladasPorCierres() { return citasCanceladasPorCierres; }
    public void setCitasCanceladasPorCierres(Integer citasCanceladasPorCierres) { this.citasCanceladasPorCierres = citasCanceladasPorCierres; }
}