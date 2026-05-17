package com.user_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest; 
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ResponseEntity<ErrorResponse> handleEmailYaRegistradoException(
            EmailYaRegistradoException ex,
            WebRequest request) {

        ErrorResponse error = new ErrorResponse();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.CONFLICT.value());
        error.setError("Conflict");  // CORREGIDO: quitado "error:"
        error.setMessage(ex.getMessage());
        error.setPath(getPath(request));

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)  // CORREGIDO: "UsuarioNoEncontradoException" no "UseraNoEncontradoException"
    public ResponseEntity<ErrorResponse> handleUsuarioNoEncontradoException(
            UsuarioNoEncontradoException ex,
            WebRequest request) {

        ErrorResponse error = new ErrorResponse();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setError("Not Found");  // CORREGIDO: quitado "error:"
        error.setMessage(ex.getMessage());
        error.setPath(getPath(request));

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();  // CORREGIDO: usar getDefaultMessage() en lugar de getMessage()
            errors.put(fieldName, errorMessage);
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);  // AGREGADO: faltaba el return
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        ErrorResponse error = new ErrorResponse();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.setError("Internal Server Error");
        error.setMessage("Ha ocurrido un error interno en el servidor: " + ex.getMessage());
        error.setPath(getPath(request));

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Método auxiliar para extraer la ruta de la petición
    private String getPath(WebRequest request) {
        String path = request.getDescription(false);
        if (path != null && path.startsWith("uri=")) {
            path = path.substring(4);
        }
        return path != null ? path : "desconocido";
    }
}