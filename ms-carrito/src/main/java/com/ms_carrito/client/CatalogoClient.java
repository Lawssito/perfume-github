package com.ms_carrito.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign hacia ms-catalogo.
 * ms-carrito lo usa para obtener el precio real de una variante
 * al momento de agregarla al carrito.
 *
 * El precio se guarda como snapshot en items_carrito — si ms-catalogo
 * cambia el precio después, el carrito conserva el precio al momento
 * de agregar el producto.
 */

@FeignClient(name = "ms-catalogo")
public interface CatalogoClient {

    @GetMapping("/api/variantes/{idVariante}")
    VarianteResponseDTO consultarVariante(
            @PathVariable("idVariante") Long idVariante
    );
}
