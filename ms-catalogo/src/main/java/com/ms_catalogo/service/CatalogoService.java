package com.ms_catalogo.service;

import java.util.List;

import com.ms_catalogo.dto.CategoriaDTO;
import com.ms_catalogo.dto.CategoriaResponseDTO;
import com.ms_catalogo.dto.MarcaDTO;
import com.ms_catalogo.dto.MarcaResponseDTO;
import com.ms_catalogo.dto.PerfumeDTO;
import com.ms_catalogo.dto.PerfumeResponseDTO;
import com.ms_catalogo.dto.VarianteDTO;
import com.ms_catalogo.dto.VarianteResponseDTO;

public interface CatalogoService {

    List<PerfumeResponseDTO> listarCatalogoCompleto();

    PerfumeResponseDTO obtenerPerfume(Long idPerfume);

    PerfumeResponseDTO crearPerfume(PerfumeDTO dto);

    PerfumeResponseDTO actualizarPerfume(Long idPerfume, PerfumeDTO dto);

    void eliminarPerfume(Long idPerfume);

    VarianteResponseDTO obtenerVariante(Long idVariante);

    VarianteResponseDTO agregarVariante(Long idPerfume, VarianteDTO dto);

    VarianteResponseDTO actualizarVariante(Long idVariante, VarianteDTO dto);

    void eliminarVariante(Long idVariante);

    List<MarcaResponseDTO> listarMarcas();

    MarcaResponseDTO crearMarca(MarcaDTO dto);

    List<CategoriaResponseDTO> listarCategorias();

    CategoriaResponseDTO crearCategoria(CategoriaDTO dto);

    // Búsqueda
    List<PerfumeResponseDTO> buscarPerfumes(String termino);

    List<PerfumeResponseDTO> listarPorMarca(Long idMarca);

    List<PerfumeResponseDTO> listarPorCategoria(Long idCategoria);

    List<PerfumeResponseDTO> listarPorNombre(String nombre);
}
