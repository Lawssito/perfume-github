package com.user_service.exception;

public class EmailYaRegistradoException extends RuntimeException {
    
    public EmailYaRegistradoException(String message) {
        super(message);
    }
    
    public EmailYaRegistradoException(String message, Throwable cause) {
        super(message, cause);
    }
}