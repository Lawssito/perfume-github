package com.ms_pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CrearPedidoDTO {

    @NotBlank(message = "La direccion de entrega es obligatoria")
    private String direccionEntrega;

    @NotBlank(message = "El courier es obligatorio")
    private String courier;
}
