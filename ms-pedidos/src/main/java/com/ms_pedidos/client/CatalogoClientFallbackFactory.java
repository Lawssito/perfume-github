package com.ms_pedidos.client;

import com.ms_pedidos.client.dto.VarianteResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CatalogoClientFallbackFactory implements FallbackFactory<CatalogoClient> {

    @Override
    public CatalogoClient create(Throwable cause) {
        log.error("Fallback activated for CatalogoClient: {}", cause.getMessage());
        return new CatalogoClient() {
            @Override
            public VarianteResponseDTO consultarVariante(Long idVariante) {
                log.warn("Degradado: consultarVariante({}) simulado por caida de ms-catalogo", idVariante);
                return new VarianteResponseDTO();
            }
        };
    }
}
