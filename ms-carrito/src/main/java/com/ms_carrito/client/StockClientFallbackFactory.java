package com.ms_carrito.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockClientFallbackFactory implements FallbackFactory<StockClient> {

    @Override
    public StockClient create(Throwable cause) {
        log.error("Fallback activated for StockClient: {}", cause.getMessage());
        return new StockClient() {
            @Override
            public StockResponseDTO consultarStock(Long idVariante) {
                log.warn("Degradado: consultarStock({}) simulado por caida de ms-stock", idVariante);
                return new StockResponseDTO();
            }
        };
    }
}
