package com.peluqueria.recepcionista_virtual.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "citas")
@Data
public class Cita {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    @ManyToOne
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    private LocalDateTime fechaHora;
    private Integer duracionMinutos;
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    private EstadoCita estado = EstadoCita.PENDIENTE;

    private String notas;
    private Boolean recordatorioEnviado = false;

    @Enumerated(EnumType.STRING)
    private OrigenCita origen = OrigenCita.TELEFONO;

    private LocalDateTime fechaCreacion = LocalDateTime.now();
}



