package com.ms_catalogo.dto;

import com.ms_catalogo.model.Concentracion;
import com.ms_catalogo.model.Genero;

public record PerfumeDto(
    Long id,
    String nombre,
    String marca,
    Integer ml,
    Double precio,
    Concentracion concentracion,
    Genero genero,
    String descripcion) {

}
