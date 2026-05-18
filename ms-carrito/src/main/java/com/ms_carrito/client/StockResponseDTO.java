package com.ms_carrito.client;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StockResponseDTO {
    private Long    idInventario;
    private Long    idVariante;
    private Integer cantidadDisponible;
    private Integer cantidadReservada;
}
