package com.ms_carrito.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Schema(description = "Carrito de compras del usuario")
public class CarritoDTO {
    private Long idCarrito;
    private Long idUsuario;
    private LocalDateTime creadoEn;
    @ArraySchema(schema = @Schema(description = "Items del carrito"))
    private List<ItemCarritoDTO> items;
    private BigDecimal total;
}
