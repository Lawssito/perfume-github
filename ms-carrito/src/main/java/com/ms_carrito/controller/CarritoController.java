package com.ms_carrito.controller;

import com.ms_carrito.dto.*;
import com.ms_carrito.service.CarritoService;

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
    public ResponseEntity<CarritoDTO> obtenerMiCarrito() {
        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] GET /api/carrito/mi-carrito", idUsuario);
        CarritoDTO respuesta = carritoService.obtenerOCrearCarrito(idUsuario);
        log.info("[AUDIT idUsuario={}] Carrito con {} items", idUsuario, respuesta.getItems().size());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/items")
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
    public ResponseEntity<CarritoDTO> actualizarCantidad(
            @PathVariable Long idItem,
            @Valid @RequestBody ActualizarCantidadDTO dto) {

        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] PUT cantidad item {} → {}", idUsuario, idItem, dto.getCantidad());
        CarritoDTO respuesta = carritoService.actualizarCantidad(idUsuario, idItem, dto);
        log.info("[AUDIT idUsuario={}] Cantidad actualizada. Total: {}", idUsuario, respuesta.getTotal());
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping("/items/{idItem}")
    public ResponseEntity<CarritoDTO> eliminarItem(@PathVariable Long idItem) {

        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] DELETE item {}", idUsuario, idItem);
        CarritoDTO respuesta = carritoService.eliminarItem(idUsuario, idItem);
        log.info("[AUDIT idUsuario={}] Item eliminado. {} restantes", idUsuario, respuesta.getItems().size());
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping
    public ResponseEntity<Void> vaciarCarrito() {

        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] DELETE — vaciando carrito completo", idUsuario);
        carritoService.vaciarCarrito(idUsuario);
        log.info("[AUDIT idUsuario={}] Carrito vaciado", idUsuario);
        return ResponseEntity.noContent().build();
    }
}