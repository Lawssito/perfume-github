package com.ms_catalogo.dto;

import java.math.BigDecimal;

import com.ms_catalogo.model.Concentracion;
import com.ms_catalogo.model.Genero;

public record PerfumeDto(
    Long id,
    String nombre,
    String marca,
    Integer ml,
    BigDecimal precio,
    Concentracion concentracion,
    Genero genero,
    String descripcion)
    
    {}
