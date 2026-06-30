package com.ms_pedidos.client;

import com.ms_pedidos.client.dto.CarritoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CarritoClientFallbackFactory implements FallbackFactory<CarritoClient> {

    @Override
    public CarritoClient create(Throwable cause) {
        log.error("Fallback activated for CarritoClient: {}", cause.getMessage());
        return new CarritoClient() {
            @Override
            public CarritoDTO obtenerCarrito() {
                log.warn("Degradado: obtenerCarrito() simulado por caida de ms-carrito");
                return new CarritoDTO();
            }

            @Override
            public void vaciarCarrito() {
                log.warn("Degradado: vaciarCarrito() omitido por caida de ms-carrito");
            }
        };
    }
}
