package com.tenpo.history;

import com.tenpo.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CallLogRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private CallLogRepository repository;

    @Test
    void savesAndRetrievesCallLog() {
        CallLog entity = new CallLog(UUID.randomUUID(), "/api/v1/calculations", "POST",
            "{\"num1\":5,\"num2\":5}", 200, "{\"result\":11}", null, Instant.now());

        StepVerifier.create(repository.save(entity).then(repository.findById(entity.id())))
            .assertNext(saved -> {
                assertThat(saved.endpoint()).isEqualTo("/api/v1/calculations");
                assertThat(saved.statusCode()).isEqualTo(200);
            })
            .verifyComplete();
    }
}
