package com.ms_carrito.exception;

public class ItemNotFoundException extends RuntimeException{
    public ItemNotFoundException(Long idItem) {
        super("No existe el item con ID: " + idItem + " en el carrito");
    }
}
