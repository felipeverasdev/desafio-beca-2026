package com.beca.ms_user.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MS1 - User Service API")
                        .description("Microsserviço de gestão e cadastro de usuários - Desafio Beca Java Bradesco 2026")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Felipe Veras")
                                .email("felipeveras2016@gmail.com"))
                );
    }
}
