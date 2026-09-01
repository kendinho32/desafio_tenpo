package com.tenpo.calculation;

import com.tenpo.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PercentageCacheServiceTest extends AbstractIntegrationTest {

    @Autowired
    private PercentageCacheService cacheService;

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void clearCache() {
        redisTemplate.delete("percentage:cache").block();
    }

    @Test
    void storesAndRetrievesFreshValue() {
        StepVerifier.create(cacheService.store(BigDecimal.TEN).then(cacheService.getCached()))
            .assertNext(cachedOpt -> {
                assertThat(cachedOpt).isPresent();
                assertThat(cachedOpt.get().value()).isEqualByComparingTo(BigDecimal.TEN);
                assertThat(cacheService.isFresh(cachedOpt.get().cachedAt())).isTrue();
            })
            .verifyComplete();
    }

    @Test
    void returnsEmptyWhenNothingCached() {
        StepVerifier.create(cacheService.getCached())
            .assertNext(cachedOpt -> assertThat(cachedOpt).isEmpty())
            .verifyComplete();
    }
}
