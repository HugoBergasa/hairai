package com.peluqueria.recepcionista_virtual.model;

import lombok.*;
import org.hibernate.annotations.Type;
import javax.persistence.*;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "historial_cambios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialCambios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "entidad", nullable = false, length = 100)
    private String entidad; // Nombre de la tabla/entidad

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(name = "accion", nullable = false, length = 20)
    private String accion; // CREATE, UPDATE, DELETE

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_email", length = 100)
    private String usuarioEmail;

    @Type(type = "jsonb")
    @Column(name = "valores_anteriores", columnDefinition = "jsonb")
    private Map<String, Object> valoresAnteriores;

    @Type(type = "jsonb")
    @Column(name = "valores_nuevos", columnDefinition = "jsonb")
    private Map<String, Object> valoresNuevos;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}