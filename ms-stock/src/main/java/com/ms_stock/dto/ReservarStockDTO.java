package com.ms_stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservarStockDTO {

    @Schema(description = "Cantidad a reservar", example = "2")

    @NotNull(message = "La cantidad no puede ser nula")
    @Min(value = 1, message = "La cantidad minima a reservar es 1")
    private Integer cantidad;

    @NotBlank(message = "idempotencyKey es obligatoria para evitar duplicados")
    @Schema(description = "Clave de idempotencia", example = "ord-123-abc")
    private String idempotencyKey;
}
