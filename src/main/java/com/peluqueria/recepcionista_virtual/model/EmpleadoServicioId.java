package com.peluqueria.recepcionista_virtual.model;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EmpleadoServicioId implements Serializable {
    private Long empleadoId;
    private Long servicioId;
}