package com.ms_carrito.exception;

public class CarritoNotFoundException extends RuntimeException{
    public CarritoNotFoundException(Long idUsuario) {
        super("No existe carrito para el usuario con ID: " + idUsuario);
    }
}
