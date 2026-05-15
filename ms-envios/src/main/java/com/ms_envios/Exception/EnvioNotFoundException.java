package com.ms_envios.Exception;

public class EnvioNotFoundException extends RuntimeException{
    public EnvioNotFoundException(Long id) {
        super("No existe envio con ID: " + id);
    }
}
