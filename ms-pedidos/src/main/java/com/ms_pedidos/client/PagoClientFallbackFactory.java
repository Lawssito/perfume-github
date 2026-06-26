package com.ms_pedidos.client;

import com.ms_pedidos.client.dto.CrearPagoClientDTO;
import com.ms_pedidos.client.dto.PagoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PagoClientFallbackFactory implements FallbackFactory<PagoClient> {

    @Override
    public PagoClient create(Throwable cause) {
        log.error("[AUDIT] ms-pagos NO DISPONIBLE: {}", cause.getMessage());
        return new PagoClient() {
            @Override
            public PagoDTO crearPago(CrearPagoClientDTO dto) {
                log.warn("[AUDIT] Degradado: crearPago({}) simulado por caida de ms-pagos", dto.getIdPedido());
                PagoDTO fallback = new PagoDTO();
                fallback.setIdPedido(dto.getIdPedido());
                fallback.setEstado("COMPLETADO");
                fallback.setReferenciaExterna("FALLBACK-" + System.currentTimeMillis());
                return fallback;
            }

            @Override
            public PagoDTO procesarPago(Long id) {
                log.warn("[AUDIT] Degradado: procesarPago({}) simulado por caida de ms-pagos", id);
                PagoDTO fallback = new PagoDTO();
                fallback.setIdTransaccion(id);
                fallback.setEstado("COMPLETADO");
                fallback.setReferenciaExterna("FALLBACK-" + System.currentTimeMillis());
                return fallback;
            }

            @Override
            public PagoDTO consultarPorPedido(Long idPedido) {
                log.warn("[AUDIT] Degradado: consultarPorPedido({}) simulado", idPedido);
                PagoDTO fallback = new PagoDTO();
                fallback.setIdPedido(idPedido);
                fallback.setEstado("PENDIENTE");
                return fallback;
            }

            @Override
            public PagoDTO reintentarPago(Long id) {
                log.warn("[AUDIT] Degradado: reintentarPago({}) simulado por caida de ms-pagos", id);
                PagoDTO fallback = new PagoDTO();
                fallback.setIdTransaccion(id);
                fallback.setEstado("COMPLETADO");
                fallback.setReferenciaExterna("FALLBACK-" + System.currentTimeMillis());
                return fallback;
            }

            @Override
            public PagoDTO consultarPorId(Long id) {
                log.warn("[AUDIT] Degradado: consultarPorId({}) simulado por caida de ms-pagos", id);
                PagoDTO fallback = new PagoDTO();
                fallback.setIdTransaccion(id);
                fallback.setEstado("PENDIENTE");
                return fallback;
            }

        };
    }
}
