package com.ms_pedidos.client.dto;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StockDTO {
    private Long    idInventario;
    private Long    idVariante;
    private Integer cantidadDisponible;
    private Integer cantidadReservada;
}
