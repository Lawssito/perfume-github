package com.ms_envios.dto;

import com.ms_envios.model.EstadoEnvio;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvanzarEstadoDTO {
    @NotNull(message = "El estado es obligatorio")
    private EstadoEnvio estado;
}
