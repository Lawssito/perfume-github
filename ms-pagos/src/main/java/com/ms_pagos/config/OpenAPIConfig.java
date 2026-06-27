package com.ms_pagos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI pagosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pagos Service API")
                        .description("Microservicio de procesamiento de pagos. " +
                                "Gestiona transacciones, anulaciones y reintentos.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo Perfume Platform")
                                .email("soporte@perfumeplatform.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")));
    }
}
