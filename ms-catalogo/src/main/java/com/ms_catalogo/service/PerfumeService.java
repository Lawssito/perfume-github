package com.ms_catalogo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ms_catalogo.dto.PerfumeDto;
import com.ms_catalogo.exception.ResourceNotFoundException;
import com.ms_catalogo.model.Perfume;
import com.ms_catalogo.repository.PerfumeRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PerfumeService {

    @Autowired
    private PerfumeRepository repository;

    public List<PerfumeDto> obtenerTodos() {
        return repository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public PerfumeDto obtenerPorId(Long id) {
        Perfume perfume = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfume no encontrado con ID: " + id));
        return mapToDTO(perfume);
    }

    public PerfumeDto crearPerfume(PerfumeDto dto) {
        Perfume perfume = mapToEntity(dto);
        Perfume guardado = repository.save(perfume);
        return mapToDTO(guardado);
    }

    private PerfumeDto mapToDTO(Perfume p) {
        return new PerfumeDto(p.getId(), p.getNombre(), p.getMarca(), p.getMl(), p.getPrecio(), p.getConcentracion(), p.getGenero(), p.getDescripcion());
    }

    private Perfume mapToEntity(PerfumeDto dto) {
        Perfume p = new Perfume();
        p.setNombre(dto.nombre());
        p.setMarca(dto.marca());
        p.setMl(dto.ml());
        p.setPrecio(dto.precio());
        p.setConcentracion(dto.concentracion());
        p.setGenero(dto.genero());
        p.setDescripcion(dto.descripcion());
        return p;
    }
}
