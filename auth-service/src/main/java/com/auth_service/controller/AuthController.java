package com.auth_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth_service.client.UserServiceClient;
import com.auth_service.dto.AuthResponseDTO;
import com.auth_service.dto.LoginRequestDTO;
import com.auth_service.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserServiceClient userServiceClient; // Para consultar el perfil (Feign)

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("Recibida petición POST en /login para: {}", request.getEmail());
        
        AuthResponseDTO respuesta = authService.autenticarUsuario(request);
        
        // EJEMPLO DE FEIGN: Consultando el user-service después de un login exitoso (Opcional, pero genial para la defensa)
        log.info("Consultando user-service para verificar estado del perfil del usuario ID: {}", respuesta.getIdUsuario());
        try {
            userServiceClient.obtenerPerfilUsuario(respuesta.getIdUsuario());
            log.info("Perfil recuperado exitosamente desde user-service");
        } catch (Exception e) {
            log.warn("No se pudo contactar al user-service, pero el login procedió. Error: {}", e.getMessage());
        }

        return ResponseEntity.ok(respuesta);
    }
}