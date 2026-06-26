package com.ms_catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VarianteResponseDTO {
    private Long idVariante;
    private Long idPerfume;
    private String sku;
    private Integer ml;
    private BigDecimal precio;
}