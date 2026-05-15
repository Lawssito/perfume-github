package com.ms_carrito.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CarritoDTO {
    private Long idCarrito;
    private Long idUsuario;
    private LocalDateTime creadoEn;
    private List<ItemCarritoDTO> items;
    private BigDecimal total;
}
