package com.auth_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("Microservicio de autenticación y gestión de credenciales. " +
                                "Proporciona endpoints para login, registro, validación de tokens JWT y refresh.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo Perfume Platform")
                                .email("soporte@perfumeplatform.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")));
    }
}
