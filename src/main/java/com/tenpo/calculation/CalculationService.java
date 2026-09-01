package com.tenpo.calculation;

import com.tenpo.calculation.exception.PercentageUnavailableException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Optional;

@Service
public class CalculationService {

    private final PercentageService percentageService;
    private final PercentageCacheService cacheService;

    public CalculationService(PercentageService percentageService, PercentageCacheService cacheService) {
        this.percentageService = percentageService;
        this.cacheService = cacheService;
    }

    public Mono<CalculationResponse> calculate(BigDecimal num1, BigDecimal num2) {
        return resolvePercentage()
            .map(percentage -> buildResponse(num1, num2, percentage));
    }

    private Mono<BigDecimal> resolvePercentage() {
        return cacheService.getCached()
            .flatMap(cachedOpt -> {
                if (cachedOpt.isPresent() && cacheService.isFresh(cachedOpt.get().cachedAt())) {
                    return Mono.just(cachedOpt.get().value());
                }
                return fetchFreshWithFallback(cachedOpt);
            });
    }

    private Mono<BigDecimal> fetchFreshWithFallback(Optional<PercentageCacheService.CachedPercentage> cachedOpt) {
        return Mono.defer(percentageService::fetchPercentage)
            .retryWhen(Retry.backoff(2, Duration.ofMillis(50)))
            .flatMap(fresh -> cacheService.store(fresh).thenReturn(fresh).onErrorReturn(fresh))
            .onErrorResume(fetchError -> cachedOpt
                .map(cached -> Mono.just(cached.value()))
                .orElseGet(() -> Mono.error(new PercentageUnavailableException(
                    "El servicio de porcentaje no está disponible y no hay un valor en caché previo."))));
    }

    private CalculationResponse buildResponse(BigDecimal num1, BigDecimal num2, BigDecimal percentage) {
        BigDecimal sum = num1.add(num2);
        BigDecimal increment = sum.multiply(percentage).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return new CalculationResponse(num1, num2, percentage, sum.add(increment));
    }
}
