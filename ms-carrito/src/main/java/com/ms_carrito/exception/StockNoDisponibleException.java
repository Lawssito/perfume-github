package com.ms_carrito.exception;

public class StockNoDisponibleException extends RuntimeException {
    public StockNoDisponibleException(Long idVariante, Integer cantidad) {
        super("Stock insuficiente para variante " + idVariante
              + ". No se pueden agregar " + cantidad + " unidades.");
    }
}
