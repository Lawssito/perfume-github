package com.ms_catalogo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class VarianteDTO {
    @NotBlank(message = "El SKU es obligatorio")
    private String sku;
    
    @NotNull(message = "Los mililitros son obligatorios")
    @Min(value = 1, message = "Debe ser mayor a 0 ml")
    private Integer ml;
    
    @NotNull(message = "El precio es obligatorio")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private BigDecimal precio;
}