package com.ms_pedidos.client;

import com.ms_pedidos.client.dto.ConfirmarReservaClientDTO;
import com.ms_pedidos.client.dto.LiberarReservaClientDTO;
import com.ms_pedidos.client.dto.ReducirStockClientDTO;
import com.ms_pedidos.client.dto.ReponerStockClientDTO;
import com.ms_pedidos.client.dto.ReservarStockClientDTO;
import com.ms_pedidos.client.dto.StockDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

// Eliminado fallbackFactory: las operaciones de escritura deben fallar fuerte
// en lugar de devolver datos falsos (Fix #3)
@FeignClient(name = "ms-stock", fallbackFactory = StockClientFallbackFactory.class)
public interface StockClient {

    @GetMapping("/api/stock/{idVariante}")
    StockDTO consultarStock(@PathVariable("idVariante") Long idVariante);

    @PutMapping("/api/stock/{idVariante}/reducir")
    StockDTO reducirStock(@PathVariable("idVariante") Long idVariante,
                                  @RequestBody ReducirStockClientDTO dto);

    @PutMapping("/api/stock/{idVariante}/reponer")
    StockDTO reponerStock(@PathVariable("idVariante") Long idVariante,
                                  @RequestBody ReponerStockClientDTO dto);

    @PutMapping("/api/stock/{idVariante}/reservar")
    StockDTO reservarStock(@PathVariable("idVariante") Long idVariante,
                                   @RequestBody ReservarStockClientDTO dto);

    @PutMapping("/api/stock/{idVariante}/confirmar-reserva")
    StockDTO confirmarReserva(@PathVariable("idVariante") Long idVariante,
                                      @RequestBody ConfirmarReservaClientDTO dto);

    @PutMapping("/api/stock/{idVariante}/liberar-reserva")
    StockDTO liberarReserva(@PathVariable("idVariante") Long idVariante,
                                    @RequestBody LiberarReservaClientDTO dto);

}