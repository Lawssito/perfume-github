package com.ms_pedidos.client.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CrearPagoClientDTO {
    private Long       idPedido;
    private BigDecimal montoTotal;
    private String     metodoPago;
}
