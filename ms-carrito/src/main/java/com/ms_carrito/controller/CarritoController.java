package com.ms_carrito.controller;

import com.ms_carrito.dto.*;
import com.ms_carrito.service.CarritoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService carritoService;

    // GET /api/carrito/{idUsuario}
    // Obtiene el carrito del usuario. Si no existe, lo crea vacío.
    @GetMapping("/{idUsuario}")
    public ResponseEntity<CarritoDTO> obtenerCarrito(
            @PathVariable Long idUsuario) {

        log.info("[CONTROLLER] GET /api/carrito/{}", idUsuario);
        CarritoDTO respuesta = carritoService.obtenerOCrearCarrito(idUsuario);
        log.info("[CONTROLLER] Carrito retornado con {} items para usuario {}",
                respuesta.getItems().size(), idUsuario);
        return ResponseEntity.ok(respuesta);
    }

    // POST /api/carrito/{idUsuario}/items
    // Agrega un perfume al carrito o suma cantidad si ya existe
    @PostMapping("/{idUsuario}/items")
    public ResponseEntity<CarritoDTO> agregarItem(
            @PathVariable Long idUsuario,
            @Valid @RequestBody AgregarItemDTO dto) {

        log.info("[CONTROLLER] POST /api/carrito/{}/items - variante: {}, cantidad: {}",
                idUsuario, dto.getIdVariante(), dto.getCantidad());
        CarritoDTO respuesta = carritoService.agregarItem(idUsuario, dto);
        log.info("[CONTROLLER] Item agregado. Total del carrito: {}", respuesta.getTotal());
        return ResponseEntity.ok(respuesta);
    }

    // PUT /api/carrito/{idUsuario}/items/{idItem}
    // Cambia la cantidad de un item específico
    @PutMapping("/{idUsuario}/items/{idItem}")
    public ResponseEntity<CarritoDTO> actualizarCantidad(
            @PathVariable Long idUsuario,
            @PathVariable Long idItem,
            @Valid @RequestBody ActualizarCantidadDTO dto) {

        log.info("[CONTROLLER] PUT /api/carrito/{}/items/{} - nueva cantidad: {}",
                idUsuario, idItem, dto.getCantidad());
        CarritoDTO respuesta = carritoService.actualizarCantidad(idUsuario, idItem, dto);
        log.info("[CONTROLLER] Cantidad actualizada. Total del carrito: {}", respuesta.getTotal());
        return ResponseEntity.ok(respuesta);
    }

    // DELETE /api/carrito/{idUsuario}/items/{idItem}
    // Elimina un item específico del carrito
    @DeleteMapping("/{idUsuario}/items/{idItem}")
    public ResponseEntity<CarritoDTO> eliminarItem(@PathVariable Long idUsuario,
            @PathVariable Long idItem) {

        log.info("[CONTROLLER] DELETE /api/carrito/{}/items/{}", idUsuario, idItem);
        CarritoDTO respuesta = carritoService.eliminarItem(idUsuario, idItem);
        log.info("[CONTROLLER] Item eliminado. Items restantes: {}", respuesta.getItems().size());
        return ResponseEntity.ok(respuesta);
    }

    // DELETE /api/carrito/{idUsuario}
    // Vacía el carrito completo (ms-pedidos lo llama al confirmar la compra)
    @DeleteMapping("/{idUsuario}")
    public ResponseEntity<Void> vaciarCarrito(@PathVariable Long idUsuario) {

        log.info("[CONTROLLER] DELETE /api/carrito/{} - vaciando carrito completo", idUsuario);
        carritoService.vaciarCarrito(idUsuario);
        log.info("[CONTROLLER] Carrito del usuario {} vaciado", idUsuario);

        // 204 No Content → operación exitosa sin cuerpo de respuesta
        return ResponseEntity.noContent().build();
    }
}