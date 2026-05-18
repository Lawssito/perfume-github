package com.ms_stock.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ms_stock.dto.InventarioDTO;
import com.ms_stock.dto.ReducirStockDTO;
import com.ms_stock.dto.ReponerStockDTO;
import com.ms_stock.service.InventarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/stock")
@RequiredArgsConstructor
public class InventarioController {
    
    private final InventarioService inventarioService;

    @GetMapping
    public ResponseEntity<List<InventarioDTO>> listarTodo() {
        log.info("[CONTROLLER] GET /api/stock - Listando todo el inventario");

        List<InventarioDTO> lista = inventarioService.listarTodo();

        log.info("[CONTROLLER] Retornando {} registros de inventario", lista.size());
        
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{idVariante}")
    public ResponseEntity<InventarioDTO> consultarPorVariante(@PathVariable Long idVariante) {

        log.info("[CONTROLLER] GET /api/stock/{} - Consultando stock", idVariante);

        InventarioDTO respuesta = inventarioService.consultarPorVariante(idVariante);

        log.info("[CONTROLLER] Stock retornado para variante {}: disponible={}",
                idVariante, respuesta.getCantidadDisponible());

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{idVariante}")
    public ResponseEntity<InventarioDTO> crearInventario(
            @PathVariable Long idVariante) {

        log.info("[CONTROLLER] POST /api/stock/{} - Creando registro de inventario", idVariante);

        InventarioDTO respuesta = inventarioService.crearInventario(idVariante);

        log.info("[CONTROLLER] Inventario creado con ID: {}", respuesta.getIdInventario());

        // 201 Created es el código correcto cuando se crea un recurso nuevo
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PutMapping("/{idVariante}/reponer")
    public ResponseEntity<InventarioDTO> reponerStock(@PathVariable Long idVariante,
            @Valid @RequestBody ReponerStockDTO dto) {

        // @Valid → activa Bean Validation sobre el DTO antes de pasar al servicio
        // Si falla, Spring lanza MethodArgumentNotValidException
        // que GlobalExceptionHandler captura y devuelve 400

        log.info("[CONTROLLER] PUT /api/stock/{}/reponer - Cantidad: {}",
                idVariante, dto.getCantidad());

        InventarioDTO respuesta = inventarioService.reponerStock(idVariante, dto);

        log.info("[CONTROLLER] Stock repuesto. Nuevo disponible: {}",
                respuesta.getCantidadDisponible());

        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/{idVariante}/reducir")
    public ResponseEntity<InventarioDTO> reducirStock(@PathVariable Long idVariante,
            @Valid @RequestBody ReducirStockDTO dto) {

        log.info("[CONTROLLER] PUT /api/stock/{}/reducir - Cantidad: {}",
                idVariante, dto.getCantidad());

        InventarioDTO respuesta = inventarioService.reducirStock(idVariante, dto);

        log.info("[CONTROLLER] Stock reducido. Nuevo disponible: {}",
                respuesta.getCantidadDisponible());

        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping("/{idVariante}")
    public ResponseEntity<Void> eliminarInventario(@PathVariable Long idVariante) {
        log.info("[CONTROLLER] DELETE /api/stock/{} - Eliminando inventario", idVariante);
        inventarioService.eliminarInventario(idVariante);
        return ResponseEntity.noContent().build();
    }

}
