package com.ms_carrito.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-stock", url = "http://localhost:8083")
public interface StockClient {

    @GetMapping("/api/stock/{idVariante}")
    StockResponseDTO consultarStock(@PathVariable("idVariante") Long idVariante);

}
