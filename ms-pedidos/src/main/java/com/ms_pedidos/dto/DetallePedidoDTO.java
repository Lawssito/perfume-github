package com.ms_pedidos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedidoDTO {
    private Long       idDetalle;
    private Long       idVariante;
    private String     nombreProducto;
    private Integer    ml;
    private Integer    cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
