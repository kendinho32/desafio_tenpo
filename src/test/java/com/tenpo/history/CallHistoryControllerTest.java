package com.tenpo.history;

import com.tenpo.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.UUID;

class CallHistoryControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CallLogRepository repository;

    @BeforeEach
    void seedData() {
        repository.deleteAll().block();
        for (int i = 0; i < 5; i++) {
            repository.save(new CallLog(UUID.randomUUID(), "/api/v1/calculations", "POST",
                "{}", 200, "{}", null, Instant.now().plusSeconds(i))).block();
        }
    }

    @Test
    void returnsPaginatedHistory() {
        webTestClient.get().uri("/api/v1/history?page=0&size=2")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content.length()").isEqualTo(2)
            .jsonPath("$.totalElements").isEqualTo(5)
            .jsonPath("$.totalPages").isEqualTo(3);
    }
}
