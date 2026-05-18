package com.ms_carrito.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgregarItemDTO {
    @NotNull(message = "El id de variante es obligatorio")
    private Long idVariante;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad minima es 1")
    private Integer cantidad;

}
