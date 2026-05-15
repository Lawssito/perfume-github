package com.ms_envios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class CrearEnvioDTO {

    @NotNull(message = "El id del pedido es obligatorio")
    private Long idPedido;

    @NotBlank(message = "La direccion de destino es obligatoria")
    private String direccionDestino;

    @NotBlank(message = "El courier es obligatorio")
    private String courier;
}
