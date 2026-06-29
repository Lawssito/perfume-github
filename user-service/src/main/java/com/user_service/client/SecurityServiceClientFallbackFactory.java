package com.user_service.client;

import com.user_service.dto.AsignarRolClientDTO;
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
            public void asignarRol(AsignarRolClientDTO request) {
                log.warn("Degradado: asignarRol({}) omitido por caida de security-service", request.getIdUsuario());
            }
        };
    }
}
