package com.ms_pedidos.client.dto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EnvioDTO {
    private Long   idEnvio;
    private Long   idPedido;
    private String estado;
    private String numeroTracking;
}
