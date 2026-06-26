package com.ms_pedidos.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor userContextInterceptor() {
        return request -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String userId = attrs.getRequest().getHeader("X-User-Id");
                if (userId != null) {
                    request.header("X-User-Id", userId);
                }
                // No propagamos X-User-Roles para que las llamadas Feign internas
                // sean tratadas como confiables por los microservicios destino
                // (ver: exigirAdmin() → if (roles == null) return;)
                String userEmail = attrs.getRequest().getHeader("X-User-Email");
                if (userEmail != null) {
                    request.header("X-User-Email", userEmail);
                }
            }
        };
    }
}
