package com.peluqueria.recepcionista_virtual.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "empleados")
@Data
public class Empleado {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    private String nombre;
    private String telefono;
    private String email;
    private String especialidad;

    // Horario personalizado (opcional)
    private String horaEntrada;
    private String horaSalida;
    private String diasTrabajo;

    private Boolean activo = true;

    @OneToMany(mappedBy = "empleado")
    private List<Cita> citas;
}
