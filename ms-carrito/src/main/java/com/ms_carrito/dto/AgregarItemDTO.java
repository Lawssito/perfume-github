package com.ms_carrito.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgregarItemDTO {

    @Schema(description = "ID de la variante", example = "1")
    @NotNull(message = "El id de variante es obligatorio")
    private Long idVariante;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad minima es 1")
    @Schema(example = "2")
    private Integer cantidad;

}
