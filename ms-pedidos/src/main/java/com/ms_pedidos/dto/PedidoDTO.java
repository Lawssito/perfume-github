package com.ms_pedidos.dto;

import com.ms_pedidos.model.EstadoPedido;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pedido con detalles")
public class PedidoDTO {

    @Schema(description = "ID del pedido", example = "1")
    private Long                         idPedido;
    private Long                         idUsuario;
    private EstadoPedido                 estado;
    private BigDecimal                   total;
    private LocalDateTime                fechaCreacion;
    private String                       direccionEntrega;
    private String                       courier;
    @ArraySchema(schema = @Schema(description = "Detalles del pedido (productos)"))
    private List<DetallePedidoDTO> detalles;
}
