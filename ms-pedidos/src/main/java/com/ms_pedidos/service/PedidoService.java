package com.ms_pedidos.service;

import com.ms_pedidos.dto.CrearPedidoDTO;
import com.ms_pedidos.dto.PedidoDTO;
import com.ms_pedidos.model.EstadoPedido;

import java.util.List;

public interface PedidoService {
    PedidoDTO crearPedido(CrearPedidoDTO dto, Long idUsuario);
    PedidoDTO pagarPedido(Long idPedido);
    PedidoDTO confirmarPedido(Long idPedido);
    PedidoDTO actualizarEstado(Long idPedido, EstadoPedido nuevoEstado);
    PedidoDTO consultarPorId(Long idPedido);
    List<PedidoDTO> listarPorUsuario(Long idUsuario);
    List<PedidoDTO> listarTodos();
}