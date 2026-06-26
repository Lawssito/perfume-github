package com.ms_pedidos.controller;

import com.ms_pedidos.dto.CrearPedidoDTO;
import com.ms_pedidos.dto.PedidoDTO;
import com.ms_pedidos.model.EstadoPedido;
import com.ms_pedidos.service.PedidoService;
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
    public ResponseEntity<PedidoDTO> crearPedido(
            @Valid @RequestBody CrearPedidoDTO dto) {

        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] POST /api/pedidos — creando pedido", idUsuario);
        PedidoDTO respuesta = pedidoService.crearPedido(dto, idUsuario);
        log.info("[AUDIT idUsuario={}] Pedido {} creado. Estado: {}", idUsuario, respuesta.getIdPedido(), respuesta.getEstado());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<PedidoDTO> pagarPedido(@PathVariable Long id) {
        PedidoDTO pedido = pedidoService.consultarPorId(id);
        exigirMismoUsuario(pedido.getIdUsuario());
        log.info("[AUDIT pedidoId={}] POST /api/pedidos/{}/pagar — procesando pago y confirmando pedido", id, id);
        PedidoDTO respuesta = pedidoService.pagarPedido(id);
        log.info("[AUDIT pedidoId={}] Pago procesado y pedido confirmado. Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<PedidoDTO> confirmarPedido(@PathVariable Long id) {
        PedidoDTO pedido = pedidoService.consultarPorId(id);
        exigirMismoUsuario(pedido.getIdUsuario());
        log.info("[AUDIT pedidoId={}] POST /api/pedidos/{}/confirmar — confirmando pedido (legacy)", id, id);
        PedidoDTO respuesta = pedidoService.confirmarPedido(id);
        log.info("[AUDIT pedidoId={}] Pedido confirmado. Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<PedidoDTO>> listarTodos() {
        exigirAdmin();
        log.info("[AUDIT] GET /api/pedidos — listado admin");
        List<PedidoDTO> respuesta = pedidoService.listarTodos();
        log.info("[AUDIT] Total pedidos: {}", respuesta.size());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/mis-pedidos")
    public ResponseEntity<List<PedidoDTO>> listarMisPedidos() {

        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT idUsuario={}] GET /api/pedidos/mis-pedidos", idUsuario);
        List<PedidoDTO> respuesta = pedidoService.listarPorUsuario(idUsuario);
        log.info("[AUDIT idUsuario={}] Tiene {} pedidos", idUsuario, respuesta.size());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> consultarPorId(@PathVariable Long id) {
        PedidoDTO pedido = pedidoService.consultarPorId(id);
        exigirMismoUsuario(pedido.getIdUsuario());
        log.info("[AUDIT pedidoId={}] GET /api/pedidos/{}", id, id);
        PedidoDTO respuesta = pedido;
        log.info("[AUDIT pedidoId={}] Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoDTO> actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoPedido estado) {

        exigirAdmin();
        log.info("[AUDIT pedidoId={}] Cambio estado → {}", id, estado);
        PedidoDTO respuesta = pedidoService.actualizarEstado(id, estado);
        log.info("[AUDIT pedidoId={}] Estado actualizado a {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }
}