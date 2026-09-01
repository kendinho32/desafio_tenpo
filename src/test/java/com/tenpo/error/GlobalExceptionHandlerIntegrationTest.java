package com.tenpo.error;

import com.tenpo.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

@TestPropertySource(properties = "percentage.failure-probability=1.0")
class GlobalExceptionHandlerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void clearCache() {
        redisTemplate.delete("percentage:cache").block();
    }

    @Test
    void returns503WhenExternalServiceFailsAndNoCacheExists() {
        webTestClient.post().uri("/api/v1/calculations")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("num1", 5, "num2", 5))
            .exchange()
            .expectStatus().isEqualTo(503)
            .expectBody()
            .jsonPath("$.status").isEqualTo(503);
    }
}
