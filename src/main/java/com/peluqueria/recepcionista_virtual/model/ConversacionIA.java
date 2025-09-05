package com.peluqueria.recepcionista_virtual.model;

import lombok.*;
import javax.persistence.*;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "conversaciones_ia")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversacionIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "call_sid", length = 100)
    private String callSid;

    @Column(name = "mensaje_usuario", columnDefinition = "TEXT")
    private String mensajeUsuario;

    @Column(name = "mensaje_ia", columnDefinition = "TEXT")
    private String mensajeIA;

    @Column(name = "intencion", length = 100)
    private String intencion;

    @Column(name = "accion_ejecutada", length = 100)
    private String accionEjecutada;

    @Column(name = "confianza")
    private Float confianza;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @ElementCollection
    @CollectionTable(name = "conversacion_parametros",
            joinColumns = @JoinColumn(name = "conversacion_id"))
    @MapKeyColumn(name = "parametro_key")
    @Column(name = "parametro_value")
    private Map<String, String> parametros;

    @Column(name = "tokens_usados")
    private Integer tokensUsados;

    @Column(name = "modelo_ia", length = 50)
    private String modeloIA = "gpt-4";

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}