package com.ms_pedidos.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.ms_pedidos.client.dto.CrearNotificacionDTO;

@FeignClient(name = "ms-notificaciones", url = "")
public interface NotificacionClient {

    @PostMapping("/api/notificaciones")
    void enviarNotificacion(@RequestBody CrearNotificacionDTO dto);
}