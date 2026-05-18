package com.ms_catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PerfumeDTO {
    @NotNull(message = "El ID de la categoría es obligatorio")
    private Long idCategoria;
    
    @NotNull(message = "El ID de la marca es obligatorio")
    private Long idMarca;
    
    @NotBlank(message = "La concentración es obligatoria (ej: EDP, EDT)")
    private String concentracion;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    private String descripcion;
}
