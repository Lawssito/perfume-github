package com.ms_pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearPedidoDTO {
    @NotNull(message = "El id de usuario es obligatorio")
    private Long idUsuario;

    @NotBlank(message = "La direccion de entrega es obligatoria")
    private String direccionEntrega;

    @NotBlank(message = "El courier es obligatorio")
    private String courier;
}
