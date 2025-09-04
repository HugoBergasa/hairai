package com.peluqueria.recepcionista_virtual.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "servicios")
@Data
public class Servicio {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer duracionMinutos;
    private Boolean activo = true;
}