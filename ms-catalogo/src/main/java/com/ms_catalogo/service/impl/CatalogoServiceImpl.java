package com.ms_catalogo.service.impl;

import com.ms_catalogo.dto.*;
import com.ms_catalogo.model.*;
import com.ms_catalogo.repository.*;
import com.ms_catalogo.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private final PerfumeRepository perfumeRepository;
    private final MarcaRepository marcaRepository;
    private final CategoriaRepository categoriaRepository;
    private final VarianteRepository varianteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PerfumeResponseDTO> listarCatalogoCompleto() {
        log.info("[SERVICE] Listando catalogo completo");
        return perfumeRepository.findAll().stream().map(this::mapPerfume).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PerfumeResponseDTO obtenerPerfume(Long idPerfume) {
        log.info("[SERVICE] Consultando perfume id={}", idPerfume);
        return mapPerfume(obtenerPerfumeEntidad(idPerfume));
    }

    @Override
    @Transactional
    public PerfumeResponseDTO crearPerfume(PerfumeDTO dto) {
        log.info("[SERVICE] Creando perfume {}", dto.getNombre());
        Marca marca = obtenerMarca(dto.getIdMarca());
        Categoria categoria = obtenerCategoria(dto.getIdCategoria());

        Perfume perfume = Perfume.builder()
                .marca(marca)
                .categoria(categoria)
                .nombre(dto.getNombre())
                .concentracion(dto.getConcentracion())
                .descripcion(dto.getDescripcion())
                .estado("ACTIVO")
                .build();

        return mapPerfume(perfumeRepository.save(perfume));
    }

    @Override
    @Transactional
    public PerfumeResponseDTO actualizarPerfume(Long idPerfume, PerfumeDTO dto) {
        log.info("[SERVICE] Actualizando perfume id={}", idPerfume);
        Perfume perfume = obtenerPerfumeEntidad(idPerfume);
        perfume.setMarca(obtenerMarca(dto.getIdMarca()));
        perfume.setCategoria(obtenerCategoria(dto.getIdCategoria()));
        perfume.setNombre(dto.getNombre());
        perfume.setConcentracion(dto.getConcentracion());
        perfume.setDescripcion(dto.getDescripcion());
        return mapPerfume(perfumeRepository.save(perfume));
    }

    @Override
    @Transactional
    public void eliminarPerfume(Long idPerfume) {
        log.info("[SERVICE] Eliminando perfume id={}", idPerfume);
        Perfume perfume = obtenerPerfumeEntidad(idPerfume);
        perfume.setEstado("DESCONTINUADO");
        perfumeRepository.save(perfume);
    }

    @Override
    @Transactional(readOnly = true)
    public VarianteResponseDTO obtenerVariante(Long idVariante) {
        log.info("[SERVICE] Consultando variante id={}", idVariante);
        return mapVariante(varianteRepository.findById(idVariante)
                .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada con ID: " + idVariante)));
    }

    @Override
    @Transactional
    public VarianteResponseDTO agregarVariante(Long idPerfume, VarianteDTO dto) {
        log.info("[SERVICE] Agregando variante {}ml al perfume {}", dto.getMl(), idPerfume);
        Perfume perfume = obtenerPerfumeEntidad(idPerfume);
        Variante variante = Variante.builder()
                .perfume(perfume)
                .sku(dto.getSku())
                .ml(dto.getMl())
                .precio(dto.getPrecio())
                .build();
        return mapVariante(varianteRepository.save(variante));
    }

    @Override
    @Transactional
    public VarianteResponseDTO actualizarVariante(Long idVariante, VarianteDTO dto) {
        log.info("[SERVICE] Actualizando variante id={}", idVariante);
        Variante variante = varianteRepository.findById(idVariante)
                .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada"));
        variante.setSku(dto.getSku());
        variante.setMl(dto.getMl());
        variante.setPrecio(dto.getPrecio());
        return mapVariante(varianteRepository.save(variante));
    }

    @Override
    @Transactional
    public void eliminarVariante(Long idVariante) {
        log.info("[SERVICE] Eliminando variante id={}", idVariante);
        varianteRepository.delete(varianteRepository.findById(idVariante)
                .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarcaResponseDTO> listarMarcas() {
        return marcaRepository.findAll().stream()
                .map(m -> new MarcaResponseDTO(m.getIdMarca(), m.getNombre()))
                .toList();
    }

    @Override
    @Transactional
    public MarcaResponseDTO crearMarca(MarcaDTO dto) {
        log.info("[SERVICE] Creando marca {}", dto.getNombre());
        Marca marca = marcaRepository.save(Marca.builder().nombre(dto.getNombre()).build());
        return new MarcaResponseDTO(marca.getIdMarca(), marca.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listarCategorias() {
        return categoriaRepository.findAll().stream()
                .map(c -> new CategoriaResponseDTO(c.getIdCategoria(), c.getNombre(), c.getDescripcion()))
                .toList();
    }

    @Override
    @Transactional
    public CategoriaResponseDTO crearCategoria(CategoriaDTO dto) {
        log.info("[SERVICE] Creando categoria {}", dto.getNombre());
        Categoria categoria = categoriaRepository.save(Categoria.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .build());
        return new CategoriaResponseDTO(categoria.getIdCategoria(), categoria.getNombre(), categoria.getDescripcion());
    }

    private Marca obtenerMarca(Long id) {
        return marcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada con ID: " + id));
    }

    private Categoria obtenerCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada con ID: " + id));
    }

    private Perfume obtenerPerfumeEntidad(Long id) {
        return perfumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfume no encontrado con ID: " + id));
    }

    private PerfumeResponseDTO mapPerfume(Perfume perfume) {
        List<VarianteResponseDTO> variantes = perfume.getVariantes() == null
                ? List.of()
                : perfume.getVariantes().stream().map(this::mapVariante).toList();
        return new PerfumeResponseDTO(
                perfume.getIdPerfume(),
                perfume.getNombre(),
                perfume.getConcentracion(),
                perfume.getDescripcion(),
                perfume.getEstado(),
                perfume.getMarca().getIdMarca(),
                perfume.getMarca().getNombre(),
                perfume.getCategoria().getIdCategoria(),
                perfume.getCategoria().getNombre(),
                variantes);
    }

    private VarianteResponseDTO mapVariante(Variante variante) {
        return new VarianteResponseDTO(
                variante.getIdVariante(),
                variante.getPerfume().getIdPerfume(),
                variante.getSku(),
                variante.getMl(),
                variante.getPrecio());
    }
}
