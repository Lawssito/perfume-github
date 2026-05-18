package com.ms_pagos.dto;

import com.ms_pagos.model.EstadoPago;
import com.ms_pagos.model.MetodoPago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {
    private Long idTransaccion;
    private Long idPedido;
    private BigDecimal montoTotal;
    private MetodoPago metodoPago;
    private EstadoPago estado;
    private String referenciaExterna;
    private LocalDateTime creadoEn;
    private LocalDateTime procesadoEn;
}
