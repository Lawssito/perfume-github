package com.ms_pedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ms_pedidos.client.dto.CrearPagoClientDTO;
import com.ms_pedidos.client.dto.PagoDTO;

@FeignClient(name = "ms-pagos", fallbackFactory = PagoClientFallbackFactory.class)
public interface PagoClient {

    @PostMapping("/api/pagos")
    PagoDTO crearPago(@RequestBody CrearPagoClientDTO dto);

    @PostMapping("/api/pagos/{id}/procesar")
    PagoDTO procesarPago(@PathVariable("id") Long id);

    @GetMapping("/api/pagos/pedido/{idPedido}")
    PagoDTO consultarPorPedido(@PathVariable("idPedido") Long idPedido);

    @PostMapping("/api/pagos/{id}/reintentar")
    PagoDTO reintentarPago(@PathVariable("id") Long id);

    @GetMapping("/api/pagos/{id}")
    PagoDTO consultarPorId(@PathVariable("id") Long id);

}