package com.ms_pedidos.client;

import com.ms_pedidos.client.dto.*;
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
            public StockDTO consultarStock(Long idVariante) {
                log.warn("Degradado: consultarStock({}) simulado por caida de ms-stock", idVariante);
                StockDTO fallback = new StockDTO();
                fallback.setIdVariante(idVariante);
                return fallback;
            }

            @Override
            public StockDTO reducirStock(Long idVariante, ReducirStockClientDTO dto) {
                log.warn("Degradado: reducirStock({}) simulado por caida de ms-stock", idVariante);
                StockDTO fallback = new StockDTO();
                fallback.setIdVariante(idVariante);
                return fallback;
            }

            @Override
            public StockDTO reponerStock(Long idVariante, ReponerStockClientDTO dto) {
                log.warn("Degradado: reponerStock({}) simulado por caida de ms-stock", idVariante);
                StockDTO fallback = new StockDTO();
                fallback.setIdVariante(idVariante);
                return fallback;
            }

            @Override
            public StockDTO reservarStock(Long idVariante, ReservarStockClientDTO dto) {
                log.warn("Degradado: reservarStock({}) simulado por caida de ms-stock", idVariante);
                StockDTO fallback = new StockDTO();
                fallback.setIdVariante(idVariante);
                return fallback;
            }

            @Override
            public StockDTO confirmarReserva(Long idVariante, ConfirmarReservaClientDTO dto) {
                log.warn("Degradado: confirmarReserva({}) simulado por caida de ms-stock", idVariante);
                StockDTO fallback = new StockDTO();
                fallback.setIdVariante(idVariante);
                return fallback;
            }

            @Override
            public StockDTO liberarReserva(Long idVariante, LiberarReservaClientDTO dto) {
                log.warn("Degradado: liberarReserva({}) simulado por caida de ms-stock", idVariante);
                StockDTO fallback = new StockDTO();
                fallback.setIdVariante(idVariante);
                return fallback;
            }
        };
    }
}
