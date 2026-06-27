package com.ms_catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PerfumeDTO {

    @Schema(description = "ID de la categoría", example = "1")
    @NotNull(message = "El ID de la categoría es obligatorio")
    private Long idCategoria;
    
    @NotNull(message = "El ID de la marca es obligatorio")
    private Long idMarca;
    
    @NotBlank(message = "La concentración es obligatoria (ej: EDP, EDT)")
    @Schema(example = "EDP")
    private String concentracion;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Schema(example = "Acqua di Gio")
    private String nombre;
    
    private String descripcion;
}
