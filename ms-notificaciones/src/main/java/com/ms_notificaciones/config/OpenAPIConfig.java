package com.ms_notificaciones.config;

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
    public OpenAPI notificacionesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notificaciones Service API")
                        .description("Microservicio de notificaciones. " +
                                "Envía y consulta notificaciones a usuarios.")
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
