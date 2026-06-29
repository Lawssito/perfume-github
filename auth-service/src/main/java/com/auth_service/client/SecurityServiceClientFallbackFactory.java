package com.auth_service.client;

import com.auth_service.dto.RolesUsuarioResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityServiceClientFallbackFactory implements FallbackFactory<SecurityServiceClient> {

    @Override
    public SecurityServiceClient create(Throwable cause) {
        log.error("Fallback activated for SecurityServiceClient: {}", cause.getMessage());
        return new SecurityServiceClient() {
            @Override
            public RolesUsuarioResponseDTO obtenerRoles(Long idUsuario) {
                log.warn("Degradado: obtenerRoles({}) simulado por caida de security-service", idUsuario);
                return new RolesUsuarioResponseDTO();
            }
        };
    }
}
