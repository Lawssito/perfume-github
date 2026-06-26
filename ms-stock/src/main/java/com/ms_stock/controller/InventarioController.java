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

import com.ms_stock.dto.ConfirmarReservaDTO;
import com.ms_stock.dto.InventarioDTO;
import com.ms_stock.dto.LiberarReservaDTO;
import com.ms_stock.dto.ReducirStockDTO;
import com.ms_stock.dto.ReponerStockDTO;
import com.ms_stock.dto.ReservarStockDTO;
import com.ms_stock.service.InventarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ms_stock.exception.ForbiddenException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class InventarioController {
    
    private final InventarioService inventarioService;

    private void exigirAdmin() {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String roles = req.getHeader("X-User-Roles");
        if (roles == null) return; // Internal Feign call → trust
        if (!roles.contains("ROLE_ADMIN")) {
            throw new ForbiddenException("No tienes permisos de administrador para realizar esta accion");
        }
    }

    @GetMapping
    public ResponseEntity<List<InventarioDTO>> listarTodo() {
        log.info("[AUDIT] GET /api/stock - Listando todo el inventario");

        List<InventarioDTO> lista = inventarioService.listarTodo();

        log.info("[AUDIT] Retornando {} registros de inventario", lista.size());
        
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{idVariante}")
    public ResponseEntity<InventarioDTO> consultarPorVariante(@PathVariable Long idVariante) {

        log.info("[AUDIT] GET /api/stock/{} - Consultando stock", idVariante);

        InventarioDTO respuesta = inventarioService.consultarPorVariante(idVariante);

        log.info("[AUDIT] Stock retornado para variante {}: disponible={}",
                idVariante, respuesta.getCantidadDisponible());

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{idVariante}")
    public ResponseEntity<InventarioDTO> crearInventario(
            @PathVariable Long idVariante) {

        exigirAdmin();
        log.info("[AUDIT] POST /api/stock/{} - Creando registro de inventario", idVariante);

        InventarioDTO respuesta = inventarioService.crearInventario(idVariante);

        log.info("[AUDIT] Inventario creado con ID: {}", respuesta.getIdInventario());

        // 201 Created es el código correcto cuando se crea un recurso nuevo
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PutMapping("/{idVariante}/reponer")
    public ResponseEntity<InventarioDTO> reponerStock(@PathVariable Long idVariante,
            @Valid @RequestBody ReponerStockDTO dto) {

        exigirAdmin();

        log.info("[AUDIT] PUT /api/stock/{}/reponer - Cantidad: {}",
                idVariante, dto.getCantidad());

        InventarioDTO respuesta = inventarioService.reponerStock(idVariante, dto);

        log.info("[AUDIT] Stock repuesto. Nuevo disponible: {}",
                respuesta.getCantidadDisponible());

        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/{idVariante}/reducir")
    public ResponseEntity<InventarioDTO> reducirStock(@PathVariable Long idVariante,
            @Valid @RequestBody ReducirStockDTO dto) {

        exigirAdmin();
        log.info("[AUDIT] PUT /api/stock/{}/reducir - Cantidad: {}",
                idVariante, dto.getCantidad());

        InventarioDTO respuesta = inventarioService.reducirStock(idVariante, dto);

        log.info("[AUDIT] Stock reducido. Nuevo disponible: {}",
                respuesta.getCantidadDisponible());

        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/{idVariante}/reservar")
    public ResponseEntity<InventarioDTO> reservarStock(@PathVariable Long idVariante,
            @Valid @RequestBody ReservarStockDTO dto) {

        exigirAdmin();
        log.info("[AUDIT] PUT /api/stock/{}/reservar - Cantidad: {}",
                idVariante, dto.getCantidad());

        InventarioDTO respuesta = inventarioService.reservarStock(idVariante, dto);

        log.info("[AUDIT] Stock reservado. Disponible: {}, Reservado: {}",
                respuesta.getCantidadDisponible(), respuesta.getCantidadReservada());

        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/{idVariante}/confirmar-reserva")
    public ResponseEntity<InventarioDTO> confirmarReserva(@PathVariable Long idVariante,
            @Valid @RequestBody ConfirmarReservaDTO dto) {

        exigirAdmin();
        log.info("[AUDIT] PUT /api/stock/{}/confirmar-reserva - Cantidad: {}",
                idVariante, dto.getCantidad());

        InventarioDTO respuesta = inventarioService.confirmarReserva(idVariante, dto);

        log.info("[AUDIT] Reserva confirmada. Reservado restante: {}",
                respuesta.getCantidadReservada());

        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/{idVariante}/liberar-reserva")
    public ResponseEntity<InventarioDTO> liberarReserva(@PathVariable Long idVariante,
            @Valid @RequestBody LiberarReservaDTO dto) {

        exigirAdmin();
        log.info("[AUDIT] PUT /api/stock/{}/liberar-reserva - Cantidad: {}",
                idVariante, dto.getCantidad());

        InventarioDTO respuesta = inventarioService.liberarReserva(idVariante, dto);

        log.info("[AUDIT] Reserva liberada. Disponible: {}, Reservado: {}",
                respuesta.getCantidadDisponible(), respuesta.getCantidadReservada());

        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping("/{idVariante}")
    public ResponseEntity<Void> eliminarInventario(@PathVariable Long idVariante) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/stock/{} - Eliminando inventario", idVariante);
        inventarioService.eliminarInventario(idVariante);
        return ResponseEntity.noContent().build();
    }
}
