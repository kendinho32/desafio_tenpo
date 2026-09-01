package com.tenpo.calculation;

import com.tenpo.calculation.exception.PercentageServiceException;
import com.tenpo.calculation.exception.PercentageUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CalculationServiceTest {

    private PercentageService percentageService;
    private PercentageCacheService cacheService;
    private CalculationService calculationService;

    @BeforeEach
    void setUp() {
        percentageService = mock(PercentageService.class);
        cacheService = mock(PercentageCacheService.class);
        calculationService = new CalculationService(percentageService, cacheService);
    }

    @Test
    void usesFreshCacheWithoutCallingExternalService() {
        Instant now = Instant.now();
        when(cacheService.getCached()).thenReturn(Mono.just(Optional.of(
            new PercentageCacheService.CachedPercentage(BigDecimal.TEN, now))));
        when(cacheService.isFresh(now)).thenReturn(true);

        StepVerifier.create(calculationService.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))
            .assertNext(response -> {
                assertThat(response.percentageApplied()).isEqualByComparingTo(BigDecimal.TEN);
                assertThat(response.result()).isEqualByComparingTo(BigDecimal.valueOf(11));
            })
            .verifyComplete();

        verifyNoInteractions(percentageService);
    }

    @Test
    void callsExternalServiceWhenNoCacheExists() {
        when(cacheService.getCached()).thenReturn(Mono.just(Optional.empty()));
        when(percentageService.fetchPercentage()).thenReturn(Mono.just(BigDecimal.TEN));
        when(cacheService.store(BigDecimal.TEN)).thenReturn(Mono.empty());

        StepVerifier.create(calculationService.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))
            .assertNext(response -> assertThat(response.result()).isEqualByComparingTo(BigDecimal.valueOf(11)))
            .verifyComplete();

        verify(cacheService).store(BigDecimal.TEN);
    }

    @Test
    void callsExternalServiceWhenCacheIsStale() {
        Instant old = Instant.now().minus(Duration.ofMinutes(45));
        when(cacheService.getCached()).thenReturn(Mono.just(Optional.of(
            new PercentageCacheService.CachedPercentage(BigDecimal.valueOf(5), old))));
        when(cacheService.isFresh(old)).thenReturn(false);
        when(percentageService.fetchPercentage()).thenReturn(Mono.just(BigDecimal.TEN));
        when(cacheService.store(BigDecimal.TEN)).thenReturn(Mono.empty());

        StepVerifier.create(calculationService.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))
            .assertNext(response -> assertThat(response.percentageApplied()).isEqualByComparingTo(BigDecimal.TEN))
            .verifyComplete();

        verify(percentageService).fetchPercentage();
    }

    @Test
    void fallsBackToStaleCacheWhenExternalServiceFailsAfterRetries() {
        Instant old = Instant.now().minus(Duration.ofMinutes(45));
        when(cacheService.getCached()).thenReturn(Mono.just(Optional.of(
            new PercentageCacheService.CachedPercentage(BigDecimal.valueOf(7), old))));
        when(cacheService.isFresh(old)).thenReturn(false);
        when(percentageService.fetchPercentage())
            .thenReturn(Mono.error(new PercentageServiceException("boom")));

        StepVerifier.create(calculationService.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))
            .assertNext(response -> assertThat(response.percentageApplied()).isEqualByComparingTo(BigDecimal.valueOf(7)))
            .verifyComplete();

        verify(percentageService, times(3)).fetchPercentage();
    }

    @Test
    void returnsErrorWhenExternalServiceFailsAndNoCacheEverExisted() {
        when(cacheService.getCached()).thenReturn(Mono.just(Optional.empty()));
        when(percentageService.fetchPercentage())
            .thenReturn(Mono.error(new PercentageServiceException("boom")));

        StepVerifier.create(calculationService.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))
            .expectError(PercentageUnavailableException.class)
            .verify();

        verify(percentageService, times(3)).fetchPercentage();
    }
}
