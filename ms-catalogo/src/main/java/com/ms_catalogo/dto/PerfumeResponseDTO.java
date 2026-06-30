package com.ms_catalogo.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Perfume con sus variantes")
public class PerfumeResponseDTO {
    private Long idPerfume;
    private String nombre;
    private String concentracion;
    private String descripcion;
    private String estado;
    private Long idMarca;
    private String nombreMarca;
    private Long idCategoria;
    private String nombreCategoria;
    @ArraySchema(schema = @Schema(description = "Variantes del perfume (ml, precio)"))
    private List<VarianteResponseDTO> variantes;
}

