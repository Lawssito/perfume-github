package com.ms_catalogo.controller;

import com.ms_catalogo.dto.PerfumeDTO;
import com.ms_catalogo.dto.VarianteDTO;
import com.ms_catalogo.model.Perfume;
import com.ms_catalogo.model.Variante;
import com.ms_catalogo.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping
    public ResponseEntity<List<Perfume>> obtenerCatalogo() {
        log.info("Petición GET recibida para listar el catálogo");
        return ResponseEntity.ok(catalogoService.listarCatalogoCompleto());
    }

    @PostMapping("/perfumes")
    public ResponseEntity<Perfume> crearPerfume(@Valid @RequestBody PerfumeDTO dto) {
        log.info("Petición POST para crear perfume: {}", dto.getNombre());
        return ResponseEntity.ok(catalogoService.crearPerfume(dto));
    }

    @PostMapping("/perfumes/{id}/variantes")
    public ResponseEntity<Variante> agregarVariante(@PathVariable Long id, @Valid @RequestBody VarianteDTO dto) {
        log.info("Petición POST para agregar variante al perfume {}", id);
        return ResponseEntity.ok(catalogoService.agregarVariante(id, dto));
    }
}