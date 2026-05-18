package com.ms_pedidos.client.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CrearEnvioClientDTO {
    private Long   idPedido;
    private String direccionDestino;
    private String courier;
}
