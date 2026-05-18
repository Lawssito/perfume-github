package com.ms_catalogo.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private List<VarianteResponseDTO> variantes;
}

