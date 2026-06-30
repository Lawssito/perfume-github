package com.ms_pedidos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI pedidosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pedidos Service API")
                        .description("Microservicio de gestión de pedidos. " +
                                "Crea, consulta y actualiza pedidos con flujo completo de pago y envío.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo Perfume Platform")
                                .email("soporte@perfumeplatform.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .addServersItem(new Server().url("http://localhost:8080").description("API Gateway"));
    }
}
