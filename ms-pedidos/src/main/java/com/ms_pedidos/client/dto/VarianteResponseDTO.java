package com.ms_pedidos.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VarianteResponseDTO {
    private Long    idVariante;
    private Long    idPerfume;
    private String  sku;
    private Integer ml;
    private Double  precio;
}
