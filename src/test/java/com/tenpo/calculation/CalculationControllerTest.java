package com.tenpo.calculation;

import com.tenpo.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

class CalculationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void computesSumPlusPercentage() {
        webTestClient.post().uri("/api/v1/calculations")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("num1", 5, "num2", 5))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.result").isEqualTo(11.0)
            .jsonPath("$.percentageApplied").isEqualTo(10.0);
    }

    @Test
    void rejectsMissingNum1WithBadRequest() {
        webTestClient.post().uri("/api/v1/calculations")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("num2", 5))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.status").isEqualTo(400);
    }
}
