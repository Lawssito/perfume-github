package com.ms_envios.exception;

public class EnvioNotFoundException extends RuntimeException{
    public EnvioNotFoundException(Long id) {
        super("No existe envio con ID: " + id);
    }
}
