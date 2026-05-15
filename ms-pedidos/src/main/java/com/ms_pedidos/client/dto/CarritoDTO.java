package com.ms_pedidos.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class CarritoDTO {
    private Long                  idCarrito;
    private Long                  idUsuario;
    private List<ItemCarritoDTO>  items;
    private BigDecimal            total;
}
