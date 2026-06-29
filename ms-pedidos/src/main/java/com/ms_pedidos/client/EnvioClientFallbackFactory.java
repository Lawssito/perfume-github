package com.ms_pedidos.client;

import com.ms_pedidos.client.dto.CrearEnvioClientDTO;
import com.ms_pedidos.client.dto.EnvioDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EnvioClientFallbackFactory implements FallbackFactory<EnvioClient> {

    @Override
    public EnvioClient create(Throwable cause) {
        log.error("Fallback activated for EnvioClient: {}", cause.getMessage());
        return new EnvioClient() {
            @Override
            public EnvioDTO crearEnvio(CrearEnvioClientDTO dto) {
                log.warn("Degradado: crearEnvio() simulado por caida de ms-envios");
                return new EnvioDTO();
            }
        };
    }
}
