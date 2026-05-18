package com.ms_pedidos.exception;

public class PedidoNotFoundException extends RuntimeException {
    public PedidoNotFoundException(Long id) {
        super("No existe pedido con ID: " + id);
    }
}