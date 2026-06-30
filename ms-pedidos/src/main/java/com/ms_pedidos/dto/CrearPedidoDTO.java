package com.ms_pedidos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CrearPedidoDTO {

    @Schema(description = "Dirección de entrega", example = "Av. Providencia 123, Santiago")

    @NotBlank(message = "La direccion de entrega es obligatoria")
    private String direccionEntrega;

    @NotBlank(message = "El courier es obligatorio")
    @Schema(example = "Starken")
    private String courier;
}
