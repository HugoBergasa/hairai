package com.peluqueria.recepcionista_virtual.model;



import lombok.*;
import org.hibernate.annotations.Type;
import javax.persistence.*;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "logs_llamadas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogLlamada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "call_sid", unique = true, nullable = false, length = 100)
    private String callSid;

    @Column(name = "numero_origen", length = 20)
    private String numeroOrigen;

    @Column(name = "numero_destino", length = 20)
    private String numeroDestino;

    @Column(name = "numero_desviado", length = 20)
    private String numeroDesviado;

    @Column(name = "timestamp_inicio", nullable = false)
    private Instant timestampInicio;

    @Column(name = "timestamp_fin")
    private Instant timestampFin;

    @Column(name = "duracion")
    private Integer duracion; // en segundos

    @Column(name = "estado", length = 50)
    private String estado; // ringing, in-progress, completed, failed

    @Type(type = "jsonb")
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "grabacion_url", length = 500)
    private String grabacionUrl;

    @Column(name = "coste_llamada")
    private Double costeLlamada;

    @Column(name = "moneda", length = 3)
    private String moneda = "EUR";

    // Campos de compliance
    @Column(name = "consentimiento_rgpd")
    private Boolean consentimientoRgpd;

    @Type(type = "jsonb")
    @Column(name = "datos_compliance", columnDefinition = "jsonb")
    private Map<String, Object> datosCompliance;

    @PrePersist
    protected void onCreate() {
        if (timestampInicio == null) {
            timestampInicio = Instant.now();
        }
        if (estado == null) {
            estado = "initiated";
        }
    }
}