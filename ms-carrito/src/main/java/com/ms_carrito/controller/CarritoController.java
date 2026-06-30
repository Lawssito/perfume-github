package com.ms_carrito.controller;

import com.ms_carrito.dto.*;
import com.ms_carrito.service.CarritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.ms_carrito.exception.ForbiddenException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
@Tag(name = "Carrito de Compras", description = "Gestión del carrito de compras por usuario")
public class CarritoController {

    private final CarritoService carritoService;

    private Long getIdUsuarioAutenticado() {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String id = req.getHeader("X-User-Id");
        if (id == null) {
            throw new ForbiddenException("Usuario no autenticado");
        }
        return Long.parseLong(id);
    }

    @GetMapping("/mi-carrito")
    @Operation(summary = "Obtener mi carrito", description = "Obtiene o crea el carrito del usuario autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carrito obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
    })
    public ResponseEntity<CarritoDTO> obtenerMiCarrito() {
        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] GET /api/carrito/mi-carrito", idUsuario);
        CarritoDTO respuesta = carritoService.obtenerOCrearCarrito(idUsuario);
        log.info("[AUDIT idUsuario={}] Carrito con {} items", idUsuario, respuesta.getItems().size());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/items")
    @Operation(summary = "Agregar item al carrito", description = "Agrega un producto (variante) al carrito del usuario autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item agregado al carrito"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o stock insuficiente")
    })
    public ResponseEntity<CarritoDTO> agregarItem(
            @Valid @RequestBody AgregarItemDTO dto) {

        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] POST item variante={} cantidad={}",
                idUsuario, dto.getIdVariante(), dto.getCantidad());
        CarritoDTO respuesta = carritoService.agregarItem(idUsuario, dto);
        log.info("[AUDIT idUsuario={}] Item agregado. Total: {}", idUsuario, respuesta.getTotal());
        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/items/{idItem}")
    @Operation(summary = "Actualizar cantidad de item", description = "Actualiza la cantidad de un item específico en el carrito")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cantidad actualizada"),
            @ApiResponse(responseCode = "404", description = "Item no encontrado en el carrito")
    })
    public ResponseEntity<CarritoDTO> actualizarCantidad(
            @Parameter(description = "ID del item en el carrito", example = "1") @PathVariable Long idItem,
            @Valid @RequestBody ActualizarCantidadDTO dto) {

        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] PUT cantidad item {} → {}", idUsuario, idItem, dto.getCantidad());
        CarritoDTO respuesta = carritoService.actualizarCantidad(idUsuario, idItem, dto);
        log.info("[AUDIT idUsuario={}] Cantidad actualizada. Total: {}", idUsuario, respuesta.getTotal());
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping("/items/{idItem}")
    @Operation(summary = "Eliminar item del carrito", description = "Elimina un item específico del carrito del usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item eliminado del carrito"),
            @ApiResponse(responseCode = "404", description = "Item no encontrado")
    })
    public ResponseEntity<CarritoDTO> eliminarItem(@Parameter(description = "ID del item en el carrito", example = "1") @PathVariable Long idItem) {

        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] DELETE item {}", idUsuario, idItem);
        CarritoDTO respuesta = carritoService.eliminarItem(idUsuario, idItem);
        log.info("[AUDIT idUsuario={}] Item eliminado. {} restantes", idUsuario, respuesta.getItems().size());
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping
    @Operation(summary = "Vaciar carrito", description = "Elimina todos los items del carrito del usuario autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Carrito vaciado exitosamente")
    })
    public ResponseEntity<Void> vaciarCarrito() {

        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] DELETE — vaciando carrito completo", idUsuario);
        carritoService.vaciarCarrito(idUsuario);
        log.info("[AUDIT idUsuario={}] Carrito vaciado", idUsuario);
        return ResponseEntity.noContent().build();
    }
}