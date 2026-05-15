package com.ms_pagos.exception;

import com.ms_pagos.model.EstadoPago;

public class TransicionEstadoInvalidaException extends RuntimeException{
    public TransicionEstadoInvalidaException(EstadoPago actual, EstadoPago solicitado) {
        super("No se puede cambiar el estado de " + actual + " a " + solicitado
              + ". El pago ya se encuentra en un estado terminal.");
    }
}
