package com.user_service.client;

import com.user_service.dto.ActualizarEstadoCredencialClientDTO;
import com.user_service.dto.CrearCredencialClientDTO;
import com.user_service.dto.TokenClaimsClientDTO;
import com.user_service.dto.ValidateTokenClientDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthServiceClientFallbackFactory implements FallbackFactory<AuthServiceClient> {

    @Override
    public AuthServiceClient create(Throwable cause) {
        log.error("Fallback activated for AuthServiceClient: {}", cause.getMessage());
        return new AuthServiceClient() {
            @Override
            public void crearCredencial(CrearCredencialClientDTO request) {
                log.warn("Degradado: crearCredencial() omitido por caida de auth-service");
            }

            @Override
            public TokenClaimsClientDTO validarToken(ValidateTokenClientDTO request) {
                log.warn("Degradado: validarToken() simulado por caida de auth-service");
                return null;
            }

            @Override
            public void actualizarEstadoCuenta(Long idUsuario, ActualizarEstadoCredencialClientDTO dto) {
                log.warn("Degradado: actualizarEstadoCuenta({}) omitido por caida de auth-service", idUsuario);
            }
        };
    }
}
