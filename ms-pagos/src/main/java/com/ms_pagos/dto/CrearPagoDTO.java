package com.ms_pagos.dto;

import com.ms_pagos.model.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrearPagoDTO {
    @NotNull(message = "El id del pedido es obligatorio")
    private Long idPedido;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal montoTotal;

    @NotNull(message = "El metodo de pago es obligatorio. Solo se acepta: TARJETA")
    private MetodoPago metodoPago;
}
