package com.ms_catalogo.controller;

import com.ms_catalogo.dto.*;
import com.ms_catalogo.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.ms_catalogo.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    private void exigirAdmin() {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String roles = req.getHeader("X-User-Roles");
        if (roles == null) return; // Internal Feign call → trust
        if (!roles.contains("ROLE_ADMIN")) {
            throw new ForbiddenException("No tienes permisos de administrador para realizar esta accion");
        }
    }

    @GetMapping("/productos")
    public ResponseEntity<List<PerfumeResponseDTO>> obtenerCatalogo() {
        log.info("[AUDIT] GET /api/catalogo/productos");
        return ResponseEntity.ok(catalogoService.listarCatalogoCompleto());
    }

    @GetMapping("/perfumes/{id}")
    public ResponseEntity<PerfumeResponseDTO> obtenerPerfume(@PathVariable Long id) {
        log.info("[AUDIT] GET /api/catalogo/perfumes/{}", id);
        return ResponseEntity.ok(catalogoService.obtenerPerfume(id));
    }

    @PostMapping("/perfumes")
    public ResponseEntity<PerfumeResponseDTO> crearPerfume(@Valid @RequestBody PerfumeDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/catalogo/perfumes - {}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearPerfume(dto));
    }

    @PutMapping("/perfumes/{id}")
    public ResponseEntity<PerfumeResponseDTO> actualizarPerfume(
            @PathVariable Long id,
            @Valid @RequestBody PerfumeDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] PUT /api/catalogo/perfumes/{}", id);
        return ResponseEntity.ok(catalogoService.actualizarPerfume(id, dto));
    }

    @DeleteMapping("/perfumes/{id}")
    public ResponseEntity<Void> eliminarPerfume(@PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/catalogo/perfumes/{}", id);
        catalogoService.eliminarPerfume(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/variantes/{id}")
    public ResponseEntity<VarianteResponseDTO> obtenerVariante(@PathVariable Long id) {
        log.info("[AUDIT] GET /api/catalogo/variantes/{}", id);
        return ResponseEntity.ok(catalogoService.obtenerVariante(id));
    }

    @PostMapping("/perfumes/{id}/variantes")
    public ResponseEntity<VarianteResponseDTO> agregarVariante(
            @PathVariable Long id,
            @Valid @RequestBody VarianteDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/catalogo/perfumes/{}/variantes", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.agregarVariante(id, dto));
    }

    @PutMapping("/variantes/{id}")
    public ResponseEntity<VarianteResponseDTO> actualizarVariante(
            @PathVariable Long id,
            @Valid @RequestBody VarianteDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] PUT /api/catalogo/variantes/{}", id);
        return ResponseEntity.ok(catalogoService.actualizarVariante(id, dto));
    }

    @DeleteMapping("/variantes/{id}")
    public ResponseEntity<Void> eliminarVariante(@PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/catalogo/variantes/{}", id);
        catalogoService.eliminarVariante(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────
    // BÚSQUEDA
    // ─────────────────────────────────────────────────────

    @GetMapping("/buscar/{nombre}")
    public ResponseEntity<List<PerfumeResponseDTO>> buscarPerfumes(
            @PathVariable String nombre) {
        log.info("[AUDIT] GET /api/catalogo/buscar/{}", nombre);
        return ResponseEntity.ok(catalogoService.buscarPerfumes(nombre));
    }

    @GetMapping("/productos/marca/{idMarca}")
    public ResponseEntity<List<PerfumeResponseDTO>> listarPorMarca(
            @PathVariable Long idMarca) {
        log.info("[AUDIT] GET /api/catalogo/productos/marca/{}", idMarca);
        return ResponseEntity.ok(catalogoService.listarPorMarca(idMarca));
    }

    @GetMapping("/productos/categoria/{idCategoria}")
    public ResponseEntity<List<PerfumeResponseDTO>> listarPorCategoria(
            @PathVariable Long idCategoria) {
        log.info("[AUDIT] GET /api/catalogo/productos/categoria/{}", idCategoria);
        return ResponseEntity.ok(catalogoService.listarPorCategoria(idCategoria));
    }

    @GetMapping("/marcas")
    public ResponseEntity<List<MarcaResponseDTO>> listarMarcas() {
        log.info("[AUDIT] GET /api/catalogo/marcas");
        return ResponseEntity.ok(catalogoService.listarMarcas());
    }

    @PostMapping("/marcas")
    public ResponseEntity<MarcaResponseDTO> crearMarca(@Valid @RequestBody MarcaDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/catalogo/marcas - {}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearMarca(dto));
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaResponseDTO>> listarCategorias() {
        log.info("[AUDIT] GET /api/catalogo/categorias");
        return ResponseEntity.ok(catalogoService.listarCategorias());
    }

    @PostMapping("/categorias")
    public ResponseEntity<CategoriaResponseDTO> crearCategoria(@Valid @RequestBody CategoriaDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/catalogo/categorias - {}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearCategoria(dto));
    }
}
