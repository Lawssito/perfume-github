package com.ms_catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MarcaDTO {
    @NotBlank(message = "El nombre de la marca es obligatorio")
    private String nombre;
}
