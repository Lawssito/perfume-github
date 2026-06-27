package com.ms_catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MarcaDTO {

    @Schema(description = "Nombre de la marca", example = "Giorgio Armani")
    @NotBlank(message = "El nombre de la marca es obligatorio")
    private String nombre;
}
