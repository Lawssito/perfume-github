package com.auth_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.auth_service.dto.RolesUsuarioResponseDTO;

@FeignClient(name = "security-service", fallbackFactory = SecurityServiceClientFallbackFactory.class)
public interface SecurityServiceClient {
    @GetMapping("/api/usuario-roles/{idUsuario}")
    RolesUsuarioResponseDTO obtenerRoles(@PathVariable("idUsuario") Long idUsuario);
}