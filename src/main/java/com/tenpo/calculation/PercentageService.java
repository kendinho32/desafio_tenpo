package com.tenpo.calculation;

import com.tenpo.calculation.exception.PercentageServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Random;

@Service
public class PercentageService {

    private final BigDecimal fixedPercentage;
    private final double failureProbability;
    private final Random random = new Random();

    public PercentageService(
            @Value("${percentage.value:10.0}") double percentageValue,
            @Value("${percentage.failure-probability:0.0}") double failureProbability) {
        this.fixedPercentage = BigDecimal.valueOf(percentageValue);
        this.failureProbability = failureProbability;
    }

    public Mono<BigDecimal> fetchPercentage() {
        return Mono.defer(() -> {
            if (random.nextDouble() < failureProbability) {
                return Mono.error(new PercentageServiceException("Fallo simulado del servicio de porcentaje."));
            }
            return Mono.just(fixedPercentage);
        });
    }
}
