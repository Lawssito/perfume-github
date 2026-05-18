package com.ms_pedidos.dto;

import com.ms_pedidos.model.EstadoPedido;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private Long                         idPedido;
    private Long                         idUsuario;
    private EstadoPedido                 estado;
    private BigDecimal                   total;
    private LocalDateTime                fechaCreacion;
    private List<DetallePedidoDTO> detalles;
}
