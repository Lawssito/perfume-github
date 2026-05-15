package com.ms_pedidos.client;

import com.ms_pedidos.client.dto.CarritoDTO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "ms-carrito", url = "http://localhost:8083/api/carrito")
public interface CarritoClient {

    @GetMapping("/api/carrito/{idUsuario}")
    CarritoDTO obtenerCarrito(@PathVariable("idUsuario") Long idUsuario);

    @DeleteMapping("/api/carrito/{idUsuario}")
    void vaciarCarrito(@PathVariable("idUsuario") Long idUsuario);
}
