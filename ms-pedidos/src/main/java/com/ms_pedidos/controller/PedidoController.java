package com.ms_pedidos.controller;

import com.ms_pedidos.dto.CrearPedidoDTO;
import com.ms_pedidos.dto.PedidoDTO;
import com.ms_pedidos.model.EstadoPedido;
import com.ms_pedidos.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // POST /api/pedidos
    // El usuario confirma su compra — dispara todo el flujo
    @PostMapping
    public ResponseEntity<PedidoDTO> crearPedido(
            @Valid @RequestBody CrearPedidoDTO dto) {

        log.info("[CONTROLLER] POST /api/pedidos - Usuario: {} | Direccion: {}",
                dto.getIdUsuario(), dto.getDireccionEntrega());

        PedidoDTO respuesta = pedidoService.crearPedido(dto);

        log.info("[CONTROLLER] Pedido creado con ID: {} | Estado: {}",
                respuesta.getIdPedido(), respuesta.getEstado());

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // GET /api/pedidos
    // Lista todos los pedidos — panel de administración
    @GetMapping
    public ResponseEntity<List<PedidoDTO>> listarTodos() {
        log.info("[CONTROLLER] GET /api/pedidos - Listando todos");
        List<PedidoDTO> respuesta = pedidoService.listarTodos();
        log.info("[CONTROLLER] Retornando {} pedidos", respuesta.size());
        return ResponseEntity.ok(respuesta);
    }

    // GET /api/pedidos/usuario/{idUsuario}
    // Lista todos los pedidos de un usuario — historial de compras
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<PedidoDTO>> listarPorUsuario(
            @PathVariable Long idUsuario) {

        log.info("[CONTROLLER] GET /api/pedidos/usuario/{}", idUsuario);
        List<PedidoDTO> respuesta = pedidoService.listarPorUsuario(idUsuario);
        log.info("[CONTROLLER] Usuario {} tiene {} pedidos", idUsuario, respuesta.size());
        return ResponseEntity.ok(respuesta);
    }

    // GET /api/pedidos/{id}
    // Consulta un pedido específico
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> consultarPorId(@PathVariable Long id) {
        log.info("[CONTROLLER] GET /api/pedidos/{}", id);
        PedidoDTO respuesta = pedidoService.consultarPorId(id);
        log.info("[CONTROLLER] Pedido {} retornado. Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    // PUT /api/pedidos/{id}/estado
    // Avanza manualmente el estado del pedido (bodega, admin)
    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoDTO> actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoPedido estado) {

        log.info("[CONTROLLER] PUT /api/pedidos/{}/estado - Nuevo estado: {}", id, estado);
        PedidoDTO respuesta = pedidoService.actualizarEstado(id, estado);
        log.info("[CONTROLLER] Pedido {} actualizado a {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }
}