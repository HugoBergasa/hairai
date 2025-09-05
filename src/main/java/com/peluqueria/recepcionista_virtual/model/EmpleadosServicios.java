package com.peluqueria.recepcionista_virtual.model;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "empleados_servicios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(EmpleadoServicioId.class)
public class EmpleadosServicios {

    @Id
    @Column(name = "empleado_id")
    private Long empleadoId;

    @Id
    @Column(name = "servicio_id")
    private Long servicioId;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Column(name = "precio_especial", precision = 10, scale = 2)
    private BigDecimal precioEspecial;

    @Column(name = "nivel_experiencia")
    private Integer nivelExperiencia; // 1-5

    @Column(name = "activo")
    private Boolean activo = true;

    @ManyToOne
    @JoinColumn(name = "empleado_id", insertable = false, updatable = false)
    private Empleado empleado;

    @ManyToOne
    @JoinColumn(name = "servicio_id", insertable = false, updatable = false)
    private Servicio servicio;
}