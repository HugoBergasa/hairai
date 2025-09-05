package com.peluqueria.recepcionista_virtual.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "servicios")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // ← CAMBIAR A UUID
    private String id;  // ← CAMBIAR A String

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(name = "duracion", nullable = false)
    private Integer duracionMinutos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id")
    private Tenant tenant;

    public Servicio() {
        this.activo = true;
    }

    public Servicio(String nombre, Integer duracionMinutos, BigDecimal precio) {
        this();
        this.nombre = nombre;
        this.duracionMinutos = duracionMinutos;
        this.precio = precio;
    }

    // CAMBIAR GETTER/SETTER DE ID
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Resto de getters y setters igual...
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public Integer getDuracion() {
        return duracionMinutos;
    }

    public void setDuracion(Integer duracion) {
        this.duracionMinutos = duracion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }

    @Override
    public String toString() {
        return "Servicio{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", duracionMinutos=" + duracionMinutos +
                ", precio=" + precio +
                ", activo=" + activo +
                '}';
    }
}