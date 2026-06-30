package com.ms_catalogo.controller;

import com.ms_catalogo.dto.*;
import com.ms_catalogo.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Catálogo", description = "Gestión de perfumes, variantes, marcas y categorías")
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
    @Operation(summary = "Obtener catálogo completo", description = "Lista todos los perfumes disponibles con sus variantes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Catálogo obtenido exitosamente")
    })
    public ResponseEntity<List<PerfumeResponseDTO>> obtenerCatalogo() {
        log.info("[AUDIT] GET /api/catalogo/productos");
        return ResponseEntity.ok(catalogoService.listarCatalogoCompleto());
    }

    @GetMapping("/perfumes/{id}")
    @Operation(summary = "Obtener perfume por ID", description = "Obtiene los detalles de un perfume específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfume encontrado"),
            @ApiResponse(responseCode = "404", description = "Perfume no encontrado")
    })
    public ResponseEntity<PerfumeResponseDTO> obtenerPerfume(@Parameter(description = "ID del perfume", example = "1") @PathVariable Long id) {
        log.info("[AUDIT] GET /api/catalogo/perfumes/{}", id);
        return ResponseEntity.ok(catalogoService.obtenerPerfume(id));
    }

    @PostMapping("/perfumes")
    @Operation(summary = "Crear perfume", description = "Crea un nuevo perfume en el catálogo (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Perfume creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos de administrador")
    })
    public ResponseEntity<PerfumeResponseDTO> crearPerfume(@Valid @RequestBody PerfumeDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/catalogo/perfumes - {}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearPerfume(dto));
    }

    @PutMapping("/perfumes/{id}")
    @Operation(summary = "Actualizar perfume", description = "Actualiza los datos de un perfume existente (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfume actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Perfume no encontrado")
    })
    public ResponseEntity<PerfumeResponseDTO> actualizarPerfume(
            @Parameter(description = "ID del perfume", example = "1") @PathVariable Long id,
            @Valid @RequestBody PerfumeDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] PUT /api/catalogo/perfumes/{}", id);
        return ResponseEntity.ok(catalogoService.actualizarPerfume(id, dto));
    }

    @DeleteMapping("/perfumes/{id}")
    @Operation(summary = "Eliminar perfume", description = "Elimina un perfume del catálogo (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Perfume eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Perfume no encontrado")
    })
    public ResponseEntity<Void> eliminarPerfume(@Parameter(description = "ID del perfume", example = "1") @PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/catalogo/perfumes/{}", id);
        catalogoService.eliminarPerfume(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/variantes/{id}")
    @Operation(summary = "Obtener variante por ID", description = "Obtiene los detalles de una variante específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variante encontrada"),
            @ApiResponse(responseCode = "404", description = "Variante no encontrada")
    })
    public ResponseEntity<VarianteResponseDTO> obtenerVariante(@Parameter(description = "ID de la variante", example = "1") @PathVariable Long id) {
        log.info("[AUDIT] GET /api/catalogo/variantes/{}", id);
        return ResponseEntity.ok(catalogoService.obtenerVariante(id));
    }

    @PostMapping("/perfumes/{id}/variantes")
    @Operation(summary = "Agregar variante a perfume", description = "Agrega una nueva variante (ml, precio) a un perfume (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Variante creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<VarianteResponseDTO> agregarVariante(
            @Parameter(description = "ID del perfume", example = "1") @PathVariable Long id,
            @Valid @RequestBody VarianteDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/catalogo/perfumes/{}/variantes", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.agregarVariante(id, dto));
    }

    @PutMapping("/variantes/{id}")
    @Operation(summary = "Actualizar variante", description = "Actualiza los datos de una variante existente (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variante actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Variante no encontrada")
    })
    public ResponseEntity<VarianteResponseDTO> actualizarVariante(
            @Parameter(description = "ID de la variante", example = "1") @PathVariable Long id,
            @Valid @RequestBody VarianteDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] PUT /api/catalogo/variantes/{}", id);
        return ResponseEntity.ok(catalogoService.actualizarVariante(id, dto));
    }

    @DeleteMapping("/variantes/{id}")
    @Operation(summary = "Eliminar variante", description = "Elimina una variante del sistema (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Variante eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Variante no encontrada")
    })
    public ResponseEntity<Void> eliminarVariante(@Parameter(description = "ID de la variante", example = "1") @PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/catalogo/variantes/{}", id);
        catalogoService.eliminarVariante(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────
    // BÚSQUEDA
    // ─────────────────────────────────────────────────────

    @GetMapping("/buscar/{nombre}")
    @Operation(summary = "Buscar perfumes por nombre", description = "Busca perfumes cuyo nombre contenga el texto indicado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda obtenidos")
    })
    public ResponseEntity<List<PerfumeResponseDTO>> buscarPerfumes(
            @Parameter(description = "Nombre o parte del nombre a buscar", example = "Acqua") @PathVariable String nombre) {
        log.info("[AUDIT] GET /api/catalogo/buscar/{}", nombre);
        return ResponseEntity.ok(catalogoService.buscarPerfumes(nombre));
    }

    @GetMapping("/productos/marca/{idMarca}")
    @Operation(summary = "Listar perfumes por marca", description = "Obtiene todos los perfumes de una marca específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfumes de la marca obtenidos")
    })
    public ResponseEntity<List<PerfumeResponseDTO>> listarPorMarca(
            @Parameter(description = "ID de la marca", example = "1") @PathVariable Long idMarca) {
        log.info("[AUDIT] GET /api/catalogo/productos/marca/{}", idMarca);
        return ResponseEntity.ok(catalogoService.listarPorMarca(idMarca));
    }

    @GetMapping("/productos/categoria/{idCategoria}")
    @Operation(summary = "Listar perfumes por categoría", description = "Obtiene todos los perfumes de una categoría específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfumes de la categoría obtenidos")
    })
    public ResponseEntity<List<PerfumeResponseDTO>> listarPorCategoria(
            @Parameter(description = "ID de la categoría", example = "1") @PathVariable Long idCategoria) {
        log.info("[AUDIT] GET /api/catalogo/productos/categoria/{}", idCategoria);
        return ResponseEntity.ok(catalogoService.listarPorCategoria(idCategoria));
    }

    @GetMapping("/marcas")
    @Operation(summary = "Listar marcas", description = "Obtiene todas las marcas registradas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de marcas obtenida")
    })
    public ResponseEntity<List<MarcaResponseDTO>> listarMarcas() {
        log.info("[AUDIT] GET /api/catalogo/marcas");
        return ResponseEntity.ok(catalogoService.listarMarcas());
    }

    @PostMapping("/marcas")
    @Operation(summary = "Crear marca", description = "Crea una nueva marca (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Marca creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<MarcaResponseDTO> crearMarca(@Valid @RequestBody MarcaDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/catalogo/marcas - {}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearMarca(dto));
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorías", description = "Obtiene todas las categorías registradas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de categorías obtenida")
    })
    public ResponseEntity<List<CategoriaResponseDTO>> listarCategorias() {
        log.info("[AUDIT] GET /api/catalogo/categorias");
        return ResponseEntity.ok(catalogoService.listarCategorias());
    }

    @PostMapping("/categorias")
    @Operation(summary = "Crear categoría", description = "Crea una nueva categoría (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Categoría creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<CategoriaResponseDTO> crearCategoria(@Valid @RequestBody CategoriaDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/catalogo/categorias - {}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearCategoria(dto));
    }
}
