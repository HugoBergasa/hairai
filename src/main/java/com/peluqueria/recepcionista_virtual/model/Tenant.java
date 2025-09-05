package com.peluqueria.recepcionista_virtual.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tenants")
@Data
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String nombrePeluqueria;

    private String telefono;
    private String email;
    private String direccion;

    // Configuración de horarios
    private String horaApertura = "09:00";
    private String horaCierre = "20:00";
    private Integer duracionCitaMinutos = 30;

    // Días laborables (L,M,X,J,V,S,D)
    private String diasLaborables = "L,M,X,J,V,S";

    @Column(columnDefinition = "TEXT")
    private String mensajeBienvenida = "Hola, gracias por llamar a {nombre}. Soy tu asistente virtual. ¿En qué puedo ayudarte?";

    private Boolean activo = true;
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL)
    private List<Servicio> servicios;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL)
    private List<Empleado> empleados;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL)
    private List<Cita> citas;
}
