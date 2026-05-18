package com.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.user_service.dto.AsignarRolClientDTO;

@FeignClient(name = "security-service")
public interface SecurityServiceClient {
    @PostMapping("/api/usuario-roles")
    void asignarRol(@RequestBody AsignarRolClientDTO request);
}
