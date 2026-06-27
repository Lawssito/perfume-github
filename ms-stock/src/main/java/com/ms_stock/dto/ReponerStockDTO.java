package com.ms_stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReponerStockDTO {

    @Schema(description = "Cantidad a reponer", example = "50")
    
    @NotNull(message = "La cantidad no puede ser nula")
    @Min(value = 1, message = "La cantidad minima a reponer es 1")
    private Integer cantidad;
}
