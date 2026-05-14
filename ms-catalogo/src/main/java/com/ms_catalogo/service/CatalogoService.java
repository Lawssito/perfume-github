package com.ms_catalogo.service;

import com.ms_catalogo.dto.*;
import com.ms_catalogo.dto.VarianteDTO;
import com.ms_catalogo.model.*;
import com.ms_catalogo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final PerfumeRepository perfumeRepository;
    private final MarcaRepository marcaRepository;
    private final CategoriaRepository categoriaRepository;
    private final VarianteRepository varianteRepository;

    @Transactional(readOnly = true)
    public List<Perfume> listarCatalogoCompleto() {
        log.info("Consultando el catálogo completo de perfumes");
        return perfumeRepository.findAll();
    }

    @Transactional
    public Perfume crearPerfume(PerfumeDTO dto) {
        log.info("Creando nuevo perfume: {}", dto.getNombre());
        
        Marca marca = marcaRepository.findById(dto.getIdMarca())
                .orElseThrow(() -> new RuntimeException("Marca no encontrada con ID: " + dto.getIdMarca()));
                
        Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + dto.getIdCategoria()));

        Perfume perfume = Perfume.builder()
                .marca(marca)
                .categoria(categoria)
                .nombre(dto.getNombre())
                .concentracion(dto.getConcentracion())
                .descripcion(dto.getDescripcion())
                .estado("ACTIVO")
                .build();

        return perfumeRepository.save(perfume);
    }

    @Transactional
    public Variante agregarVariante(Long idPerfume, VarianteDTO dto) {
        log.info("Agregando variante de {}ml al perfume ID: {}", dto.getMl(), idPerfume);
        
        Perfume perfume = perfumeRepository.findById(idPerfume)
                .orElseThrow(() -> new RuntimeException("Perfume no encontrado con ID: " + idPerfume));

        Variante variante = Variante.builder()
                .perfume(perfume)
                .sku(dto.getSku())
                .ml(dto.getMl())
                .precio(dto.getPrecio())
                .build();

        return varianteRepository.save(variante);
    }
}