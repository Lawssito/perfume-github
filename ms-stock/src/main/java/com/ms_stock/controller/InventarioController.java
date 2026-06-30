package com.ms_stock.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Stock / Inventario", description = "Gestión de inventario, reservas y reposiciones")
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
    @Operation(summary = "Listar inventario completo", description = "Obtiene todo el inventario registrado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventario obtenido exitosamente")
    })
    public ResponseEntity<List<InventarioDTO>> listarTodo() {
        log.info("[AUDIT] GET /api/stock - Listando todo el inventario");

        List<InventarioDTO> lista = inventarioService.listarTodo();

        log.info("[AUDIT] Retornando {} registros de inventario", lista.size());
        
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{idVariante}")
    @Operation(summary = "Consultar stock por variante", description = "Obtiene el inventario de una variante específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock encontrado"),
            @ApiResponse(responseCode = "404", description = "Variante no encontrada en inventario")
    })
    public ResponseEntity<InventarioDTO> consultarPorVariante(@Parameter(description = "ID de la variante", example = "1") @PathVariable Long idVariante) {

        log.info("[AUDIT] GET /api/stock/{} - Consultando stock", idVariante);

        InventarioDTO respuesta = inventarioService.consultarPorVariante(idVariante);

        log.info("[AUDIT] Stock retornado para variante {}: disponible={}",
                idVariante, respuesta.getCantidadDisponible());

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{idVariante}")
    @Operation(summary = "Crear registro de inventario", description = "Crea un nuevo registro de inventario para una variante (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inventario creado exitosamente"),
            @ApiResponse(responseCode = "409", description = "La variante ya tiene inventario")
    })
    public ResponseEntity<InventarioDTO> crearInventario(
            @Parameter(description = "ID de la variante", example = "1") @PathVariable Long idVariante) {

        exigirAdmin();
        log.info("[AUDIT] POST /api/stock/{} - Creando registro de inventario", idVariante);

        InventarioDTO respuesta = inventarioService.crearInventario(idVariante);

        log.info("[AUDIT] Inventario creado con ID: {}", respuesta.getIdInventario());

        // 201 Created es el código correcto cuando se crea un recurso nuevo
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PutMapping("/{idVariante}/reponer")
    @Operation(summary = "Reponer stock", description = "Incrementa el stock disponible de una variante (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock repuesto exitosamente"),
            @ApiResponse(responseCode = "400", description = "Cantidad inválida")
    })
    public ResponseEntity<InventarioDTO> reponerStock(@Parameter(description = "ID de la variante", example = "1") @PathVariable Long idVariante,
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
    @Operation(summary = "Reducir stock", description = "Reduce el stock disponible de una variante (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock reducido exitosamente"),
            @ApiResponse(responseCode = "400", description = "Stock insuficiente")
    })
    public ResponseEntity<InventarioDTO> reducirStock(@Parameter(description = "ID de la variante", example = "1") @PathVariable Long idVariante,
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
    @Operation(summary = "Reservar stock", description = "Reserva stock de una variante para un pedido en curso (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock reservado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Stock insuficiente para reservar")
    })
    public ResponseEntity<InventarioDTO> reservarStock(@Parameter(description = "ID de la variante", example = "1") @PathVariable Long idVariante,
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
    @Operation(summary = "Confirmar reserva", description = "Confirma una reserva y descuenta del stock disponible (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva confirmada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Reserva insuficiente")
    })
    public ResponseEntity<InventarioDTO> confirmarReserva(@Parameter(description = "ID de la variante", example = "1") @PathVariable Long idVariante,
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
    @Operation(summary = "Liberar reserva", description = "Libera stock previamente reservado (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva liberada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Reserva insuficiente para liberar")
    })
    public ResponseEntity<InventarioDTO> liberarReserva(@Parameter(description = "ID de la variante", example = "1") @PathVariable Long idVariante,
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
    @Operation(summary = "Eliminar inventario", description = "Elimina el registro de inventario de una variante (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Inventario eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Inventario no encontrado")
    })
    public ResponseEntity<Void> eliminarInventario(@Parameter(description = "ID de la variante", example = "1") @PathVariable Long idVariante) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/stock/{} - Eliminando inventario", idVariante);
        inventarioService.eliminarInventario(idVariante);
        return ResponseEntity.noContent().build();
    }
}
