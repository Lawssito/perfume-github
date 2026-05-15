package com.ms_pedidos.client;

import com.ms_pedidos.client.dto.CrearEnvioClientDTO;
import com.ms_pedidos.client.dto.EnvioDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-envios", url = "http://localhost:8087/api/envios")
public interface EnvioClient {

    @PostMapping("/api/envios")
    EnvioDTO crearEnvio(@RequestBody CrearEnvioClientDTO dto);
}
