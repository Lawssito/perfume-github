package com.ms_pedidos.client.dto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class ItemCarritoDTO {
    private Long       idItem;
    private Long       idVariante;
    private Integer    cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
