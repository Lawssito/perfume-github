package com.ms_pagos.dto;

import com.ms_pagos.model.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrearPagoDTO {

    @Schema(description = "ID del pedido", example = "1")
    @NotNull(message = "El id del pedido es obligatorio")
    private Long idPedido;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @Schema(example = "45000")
    private BigDecimal montoTotal;

    @NotNull(message = "El metodo de pago es obligatorio. Solo se acepta: TARJETA")
    @Schema(description = "Método de pago", example = "TARJETA")
    private MetodoPago metodoPago;
}
