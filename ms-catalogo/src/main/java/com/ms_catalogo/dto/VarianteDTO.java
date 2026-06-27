package com.ms_catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class VarianteDTO {

    @Schema(description = "SKU de la variante", example = "ADG-EDP-100")
    @NotBlank(message = "El SKU es obligatorio")
    private String sku;
    
    @NotNull(message = "Los mililitros son obligatorios")
    @Min(value = 1, message = "Debe ser mayor a 0 ml")
    @Schema(example = "100")
    private Integer ml;
    
    @NotNull(message = "El precio es obligatorio")
    @Min(value = 0, message = "El precio no puede ser negativo")
    @Schema(example = "45000")
    private BigDecimal precio;
}