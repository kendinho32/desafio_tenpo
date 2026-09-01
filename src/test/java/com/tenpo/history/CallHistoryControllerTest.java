package com.tenpo.history;

import com.tenpo.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void returnsHistoryOrderedByCalledAtDescending() {
        PageResponse<CallLog> body = webTestClient.get().uri("/api/v1/history?page=0&size=5")
            .exchange()
            .expectStatus().isOk()
            .expectBody(new ParameterizedTypeReference<PageResponse<CallLog>>() {})
            .returnResult()
            .getResponseBody();

        assertThat(body).isNotNull();
        List<CallLog> content = body.content();
        assertThat(content).hasSize(5);
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).calledAt())
                .as("row %d should be more recent than row %d", i, i + 1)
                .isAfter(content.get(i + 1).calledAt());
        }
    }

    @Test
    void rejectsSizeZeroWithBadRequest() {
        webTestClient.get().uri("/api/v1/history?page=0&size=0")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void rejectsNegativePageWithBadRequest() {
        webTestClient.get().uri("/api/v1/history?page=-1&size=2")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.status").isEqualTo(400);
    }
}
