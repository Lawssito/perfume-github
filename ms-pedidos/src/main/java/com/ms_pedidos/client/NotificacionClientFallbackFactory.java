package com.ms_pedidos.client;

import com.ms_pedidos.client.dto.CrearNotificacionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificacionClientFallbackFactory implements FallbackFactory<NotificacionClient> {

    @Override
    public NotificacionClient create(Throwable cause) {
        log.error("Fallback activated for NotificacionClient: {}", cause.getMessage());
        return new NotificacionClient() {
            @Override
            public void enviarNotificacion(CrearNotificacionDTO dto) {
                log.warn("Degradado: enviarNotificacion({}) omitido por caida de ms-notificaciones", dto.getIdUsuario());
            }
        };
    }
}
