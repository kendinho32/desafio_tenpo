package com.tenpo.config;

import com.tenpo.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

class OpenApiConfigTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void exposesOpenApiDocs() {
        webTestClient.get().uri("/v3/api-docs")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.info.title").isEqualTo("Tenpo Challenge API");
    }
}
