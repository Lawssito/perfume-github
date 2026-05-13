package com.ms_stock.exception;

public class VarianteNotFoundException extends RuntimeException{
    
    public VarianteNotFoundException(Long idVariante) {
        super("No existe registro de inventario para la variante con ID: " + idVariante);
    }
}
