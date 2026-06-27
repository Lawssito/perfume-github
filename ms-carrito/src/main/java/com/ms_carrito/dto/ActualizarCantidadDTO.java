package com.ms_carrito.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActualizarCantidadDTO {

    @Schema(description = "Nueva cantidad", example = "3")
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad minima es 1")
    private Integer cantidad;
}
