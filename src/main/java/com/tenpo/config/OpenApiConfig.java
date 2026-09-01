package com.tenpo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tenpoOpenAPI() {
        return new OpenAPI().info(new Info()
            .title("Tenpo Challenge API")
            .description("API reactiva de cálculo con porcentaje dinámico, caché, reintentos, historial paginado y rate limiting.")
            .version("v1"));
    }
}
