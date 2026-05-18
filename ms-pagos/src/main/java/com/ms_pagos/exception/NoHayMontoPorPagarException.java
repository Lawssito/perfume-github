package com.ms_pagos.exception;
import java.math.BigDecimal;

public class NoHayMontoPorPagarException extends RuntimeException {
    public NoHayMontoPorPagarException(BigDecimal monto) {
        super("No se puede crear o procesar un pago porque no hay monto por pagar. Monto recibido: " + monto);
    }
}
