package com.ms_stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LiberarReservaDTO {

    @NotNull(message = "La cantidad no puede ser nula")
    @Min(value = 1, message = "La cantidad minima a liberar es 1")
    private Integer cantidad;

    @NotBlank(message = "idempotencyKey es obligatoria para evitar duplicados")
    private String idempotencyKey;
}
