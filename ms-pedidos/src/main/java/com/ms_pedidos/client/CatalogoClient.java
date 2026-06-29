package com.ms_pedidos.client;

import com.ms_pedidos.client.dto.VarianteResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-catalogo", fallbackFactory = CatalogoClientFallbackFactory.class)
public interface CatalogoClient {

    @GetMapping("/api/catalogo/variantes/{idVariante}")
    VarianteResponseDTO consultarVariante(@PathVariable("idVariante") Long idVariante);
}
