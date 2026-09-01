package com.tenpo.ratelimit;

import com.tenpo.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@TestPropertySource(properties = {"ratelimit.max-requests=3", "ratelimit.window-seconds=60"})
class RateLimitFilterTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void resetRateLimit() {
        redisTemplate.keys("ratelimit:*").flatMap(redisTemplate::delete).blockLast();
    }

    @Test
    void fourthRequestWithinWindowIsRejected() {
        for (int i = 0; i < 3; i++) {
            webTestClient.get().uri("/api/v1/history")
                .exchange()
                .expectStatus().is2xxSuccessful();
        }

        webTestClient.get().uri("/api/v1/history")
            .exchange()
            .expectStatus().isEqualTo(429)
            .expectBody()
            .jsonPath("$.status").isEqualTo(429);
    }
}
