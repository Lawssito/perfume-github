package com.ms_pedidos.client;

import com.ms_pedidos.client.dto.CarritoDTO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "ms-carrito")
public interface CarritoClient {

    @GetMapping("/api/carrito/mi-carrito")
    CarritoDTO obtenerCarrito();

    @DeleteMapping("/api/carrito")
    void vaciarCarrito();
}