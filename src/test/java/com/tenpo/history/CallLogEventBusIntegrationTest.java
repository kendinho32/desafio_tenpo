package com.tenpo.history;

import com.tenpo.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CallLogEventBusIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CallLogEventBus eventBus;

    @Autowired
    private CallLogRepository repository;

    @Test
    void publishedEventIsPersistedAsynchronously() {
        String uniqueEndpoint = "/test/" + UUID.randomUUID();
        eventBus.publish(new CallLogEvent(uniqueEndpoint, "GET", null, 200, "ok", null, Instant.now()));

        StepVerifier.create(
                Flux.interval(Duration.ofMillis(50))
                    .onBackpressureDrop()
                    .flatMap(tick -> repository.findAll(), 1)
                    .filter(log -> log.endpoint().equals(uniqueEndpoint))
                    .next()
                    .timeout(Duration.ofSeconds(3)))
            .assertNext(log -> {
                assertThat(log.httpMethod()).isEqualTo("GET");
                assertThat(log.statusCode()).isEqualTo(200);
            })
            .verifyComplete();
    }
}
