package com.ms_carrito.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ItemCarritoDTO {

    private Long       idItem;
    private Long       idVariante;
    private Integer    cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

}
