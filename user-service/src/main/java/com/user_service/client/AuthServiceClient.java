package com.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.user_service.dto.CrearCredencialClientDTO;
import com.user_service.dto.TokenClaimsClientDTO;
import com.user_service.dto.ValidateTokenClientDTO;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {
    @PostMapping("/api/auth/credenciales")
    void crearCredencial(@RequestBody CrearCredencialClientDTO request);

    @PostMapping("/api/auth/validate")
    TokenClaimsClientDTO validarToken(@RequestBody ValidateTokenClientDTO request);
}
