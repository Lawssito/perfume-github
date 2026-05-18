package com.ms_pedidos.client.dto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PagoDTO {
    private Long   idTransaccion;
    private Long   idPedido;
    private String estado;
    private String referenciaExterna;
}
