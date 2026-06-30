package com.ms_envios.dto;

import com.ms_envios.model.EstadoEnvio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvanzarEstadoDTO {

    @Schema(description = "Nuevo estado del envío", example = "EN_TRANSITO")
    @NotNull(message = "El estado es obligatorio")
    private EstadoEnvio estado;
}
