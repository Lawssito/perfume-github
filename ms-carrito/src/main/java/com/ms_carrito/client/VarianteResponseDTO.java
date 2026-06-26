package com.ms_carrito.client;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Espejo de la respuesta de ms-catalogo para una variante.
 * Solo los campos que ms-carrito necesita — si ms-catalogo
 * agrega campos nuevos, este DTO los ignora silenciosamente.
 */

@Data
@NoArgsConstructor
public class VarianteResponseDTO {
    private Long idVariante;
    private Long idPerfume;
    private String sku;
    private Integer ml;
    private BigDecimal precio;
}