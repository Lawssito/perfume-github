package com.ms_pagos.dto;

import com.ms_pagos.model.EstadoPago;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActualizarEstadoDTO {
    
    @NotNull(message = "El estado es obligatorio")
    private EstadoPago estado;

}
