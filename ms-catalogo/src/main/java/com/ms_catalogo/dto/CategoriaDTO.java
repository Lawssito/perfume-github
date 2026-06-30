package com.ms_catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoriaDTO {

    @Schema(description = "Nombre de la categoría", example = "Perfumes Masculinos")
    @NotBlank(message = "El nombre de la categoria es obligatorio")
    private String nombre;
    private String descripcion;
}
