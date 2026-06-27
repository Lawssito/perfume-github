package com.ms_pedidos.controller;

import com.ms_pedidos.dto.CrearPedidoDTO;
import com.ms_pedidos.dto.PedidoDTO;
import com.ms_pedidos.model.EstadoPedido;
import com.ms_pedidos.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ms_pedidos.exception.ForbiddenException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Gestión de pedidos con flujo completo de pago y envío")
public class PedidoController {

    private final PedidoService pedidoService;

    private void exigirAdmin() {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String roles = req.getHeader("X-User-Roles");
        if (roles == null) return;
        if (!roles.contains("ROLE_ADMIN")) {
            throw new ForbiddenException("No tienes permisos de administrador para realizar esta accion");
        }
    }

    private Long getIdUsuarioAutenticado() {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String id = req.getHeader("X-User-Id");
        return id != null ? Long.parseLong(id) : null;
    }

    private void exigirMismoUsuario(Long idUsuarioPath) {
        Long idToken = getIdUsuarioAutenticado();
        if (idToken == null) return;
        if (!idToken.equals(idUsuarioPath)) {
            throw new ForbiddenException("Solo puedes gestionar tus propios datos");
        }
    }

    @PostMapping
    @Operation(summary = "Crear pedido", description = "Crea un nuevo pedido a partir del carrito del usuario autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o carrito vacío")
    })
    public ResponseEntity<PedidoDTO> crearPedido(
            @Valid @RequestBody CrearPedidoDTO dto) {

        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] POST /api/pedidos — creando pedido", idUsuario);
        PedidoDTO respuesta = pedidoService.crearPedido(dto, idUsuario);
        log.info("[AUDIT idUsuario={}] Pedido {} creado. Estado: {}", idUsuario, respuesta.getIdPedido(), respuesta.getEstado());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/{id}/pagar")
    @Operation(summary = "Pagar pedido", description = "Procesa el pago y confirma un pedido pendiente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago procesado y pedido confirmado"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    public ResponseEntity<PedidoDTO> pagarPedido(@Parameter(description = "ID del pedido", example = "1") @PathVariable Long id) {
        PedidoDTO pedido = pedidoService.consultarPorId(id);
        exigirMismoUsuario(pedido.getIdUsuario());
        log.info("[AUDIT pedidoId={}] POST /api/pedidos/{}/pagar — procesando pago y confirmando pedido", id, id);
        PedidoDTO respuesta = pedidoService.pagarPedido(id);
        log.info("[AUDIT pedidoId={}] Pago procesado y pedido confirmado. Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar pedido", description = "Confirma un pedido manualmente (flujo legacy)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido confirmado"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    public ResponseEntity<PedidoDTO> confirmarPedido(@Parameter(description = "ID del pedido", example = "1") @PathVariable Long id) {
        PedidoDTO pedido = pedidoService.consultarPorId(id);
        exigirMismoUsuario(pedido.getIdUsuario());
        log.info("[AUDIT pedidoId={}] POST /api/pedidos/{}/confirmar — confirmando pedido (legacy)", id, id);
        PedidoDTO respuesta = pedidoService.confirmarPedido(id);
        log.info("[AUDIT pedidoId={}] Pedido confirmado. Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping
    @Operation(summary = "Listar todos los pedidos", description = "Obtiene todos los pedidos del sistema (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos obtenida")
    })
    public ResponseEntity<List<PedidoDTO>> listarTodos() {
        exigirAdmin();
        log.info("[AUDIT] GET /api/pedidos — listado admin");
        List<PedidoDTO> respuesta = pedidoService.listarTodos();
        log.info("[AUDIT] Total pedidos: {}", respuesta.size());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/mis-pedidos")
    @Operation(summary = "Listar mis pedidos", description = "Obtiene los pedidos del usuario autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos del usuario obtenida")
    })
    public ResponseEntity<List<PedidoDTO>> listarMisPedidos() {

        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] GET /api/pedidos/mis-pedidos", idUsuario);
        List<PedidoDTO> respuesta = pedidoService.listarPorUsuario(idUsuario);
        log.info("[AUDIT idUsuario={}] Tiene {} pedidos", idUsuario, respuesta.size());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar pedido por ID", description = "Obtiene los detalles de un pedido específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    public ResponseEntity<PedidoDTO> consultarPorId(@Parameter(description = "ID del pedido", example = "1") @PathVariable Long id) {
        PedidoDTO pedido = pedidoService.consultarPorId(id);
        exigirMismoUsuario(pedido.getIdUsuario());
        log.info("[AUDIT pedidoId={}] GET /api/pedidos/{}", id, id);
        PedidoDTO respuesta = pedido;
        log.info("[AUDIT pedidoId={}] Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Actualizar estado de pedido", description = "Actualiza el estado de un pedido (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    public ResponseEntity<PedidoDTO> actualizarEstado(
            @Parameter(description = "ID del pedido", example = "1") @PathVariable Long id,
            @Parameter(description = "Nuevo estado del pedido", example = "CONFIRMADO") @RequestParam EstadoPedido estado) {

        exigirAdmin();
        log.info("[AUDIT pedidoId={}] Cambio estado → {}", id, estado);
        PedidoDTO respuesta = pedidoService.actualizarEstado(id, estado);
        log.info("[AUDIT pedidoId={}] Estado actualizado a {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }
}