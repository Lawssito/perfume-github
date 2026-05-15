package com.ms_pedidos.exception;

import com.ms_pedidos.model.EstadoPedido;

public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(EstadoPedido actual, EstadoPedido solicitado) {
        super("Transicion invalida: no se puede pasar de "
              + actual + " a " + solicitado);
    }
}
