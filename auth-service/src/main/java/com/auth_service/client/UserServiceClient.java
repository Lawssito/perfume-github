package com.auth_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// El nombre debe coincidir con el spring.application.name del user-service
@FeignClient(name = "user-service", url = "http://localhost:8086") 
public interface UserServiceClient {

    // Asumimos que en user-service crearás un endpoint para obtener el perfil por ID
    @GetMapping("/api/usuarios/{id}")
    Object obtenerPerfilUsuario(@PathVariable("id") Long id);
}