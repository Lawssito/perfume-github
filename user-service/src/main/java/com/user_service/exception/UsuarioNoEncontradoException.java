package com.user_service.exception;

public class UsuarioNoEncontradoException extends RuntimeException {
    
    public UsuarioNoEncontradoException(String message) {
        super(message);
    }
    
    public UsuarioNoEncontradoException(String message, Throwable cause) {
        super(message, cause);
    }
}