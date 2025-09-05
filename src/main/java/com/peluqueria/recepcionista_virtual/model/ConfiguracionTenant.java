package com.peluqueria.recepcionista_virtual.model;

import lombok.*;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "configuracion_tenant",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "clave"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "clave", nullable = false, length = 100)
    private String clave;

    @Column(name = "valor", columnDefinition = "TEXT")
    private String valor;

    @Column(name = "tipo", length = 20)
    private String tipo; // string, number, boolean, json

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "categoria", length = 50)
    private String categoria; // general, twilio, openai, compliance, etc.

    @Column(name = "es_sensible")
    private Boolean esSensible = false; // Para datos como tokens, passwords

    @Column(name = "es_editable")
    private Boolean esEditable = true;

    @Column(name = "actualizado_en")
    @Temporal(TemporalType.TIMESTAMP)
    private Date actualizadoEn;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = new Date();
    }
}