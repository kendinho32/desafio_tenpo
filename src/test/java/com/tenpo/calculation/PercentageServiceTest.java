package com.tenpo.calculation;

import com.tenpo.calculation.exception.PercentageServiceException;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

class PercentageServiceTest {

    @Test
    void alwaysSucceedsWhenFailureProbabilityIsZero() {
        PercentageService service = new PercentageService(10.0, 0.0);

        StepVerifier.create(service.fetchPercentage())
            .expectNext(BigDecimal.valueOf(10.0))
            .verifyComplete();
    }

    @Test
    void alwaysFailsWhenFailureProbabilityIsOne() {
        PercentageService service = new PercentageService(10.0, 1.0);

        StepVerifier.create(service.fetchPercentage())
            .expectError(PercentageServiceException.class)
            .verify();
    }
}
