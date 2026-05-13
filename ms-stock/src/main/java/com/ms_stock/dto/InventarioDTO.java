package com.ms_stock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventarioDTO {

    private Long idInventario;
    private Long idVariante;
    private Integer cantidadDisponible;
    private Integer cantidadReservada;
}