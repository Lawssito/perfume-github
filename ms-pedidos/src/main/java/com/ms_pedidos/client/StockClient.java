package com.ms_pedidos.client;

import com.ms_pedidos.client.dto.ReducirStockClientDTO;
import com.ms_pedidos.client.dto.StockDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-stock")
public interface StockClient {

    @GetMapping("/api/stock/{idVariante}")
    StockDTO consultarStock(@PathVariable("idVariante") Long idVariante);

    @PutMapping("/api/stock/{idVariante}/reducir")
    StockDTO reducirStock(@PathVariable("idVariante") Long idVariante,
                                  @RequestBody ReducirStockClientDTO dto);
}