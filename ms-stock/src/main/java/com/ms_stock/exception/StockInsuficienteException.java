package com.ms_stock.exception;


public class StockInsuficienteException extends RuntimeException{

    public StockInsuficienteException(Long idVariante, Integer solicitado, Integer disponible) {
        super("Stock insuficiente para variante " + idVariante
            + ", Solicitado: " + solicitado
            + ", disponible: " + disponible);
    }

}
