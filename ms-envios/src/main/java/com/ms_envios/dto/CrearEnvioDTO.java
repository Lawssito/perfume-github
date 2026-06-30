package com.ms_envios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearEnvioDTO {

    @Schema(description = "ID del pedido", example = "1")

    @NotNull(message = "El id del pedido es obligatorio")
    private Long idPedido;

    @NotBlank(message = "La direccion de destino es obligatoria")
    @Schema(example = "Av. Providencia 123, Santiago")
    private String direccionDestino;

    @NotBlank(message = "El courier es obligatorio")
    @Schema(example = "Starken")
    private String courier;
}
