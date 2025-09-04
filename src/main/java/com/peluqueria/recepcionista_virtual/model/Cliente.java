package com.peluqueria.recepcionista_virtual.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "clientes")
@Data
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    private String nombre;
    private String telefono;
    private String email;
    private String notas;

    private LocalDateTime fechaRegistro = LocalDateTime.now();
    private LocalDateTime ultimaVisita;

    @OneToMany(mappedBy = "cliente")
    private List<Cita> historialCitas;
}