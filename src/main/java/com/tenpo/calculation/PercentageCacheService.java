package com.tenpo.calculation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PercentageCacheService {

    private static final String HASH_KEY = "percentage:cache";
    private static final String FIELD_VALUE = "value";
    private static final String FIELD_CACHED_AT = "cachedAt";

    private final ReactiveHashOperations<String, String, String> hashOps;
    private final Duration freshnessWindow;

    public PercentageCacheService(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Value("${percentage.cache-ttl-minutes:30}") long cacheTtlMinutes) {
        this.hashOps = redisTemplate.opsForHash();
        this.freshnessWindow = Duration.ofMinutes(cacheTtlMinutes);
    }

    public Mono<Optional<CachedPercentage>> getCached() {
        return hashOps.multiGet(HASH_KEY, List.of(FIELD_VALUE, FIELD_CACHED_AT))
            .map(values -> {
                String value = values.get(0);
                String cachedAt = values.get(1);
                if (value == null || cachedAt == null) {
                    return Optional.<CachedPercentage>empty();
                }
                return Optional.of(new CachedPercentage(new BigDecimal(value), Instant.parse(cachedAt)));
            });
    }

    public Mono<Void> store(BigDecimal value) {
        Map<String, String> fields = Map.of(
            FIELD_VALUE, value.toPlainString(),
            FIELD_CACHED_AT, Instant.now().toString());
        return hashOps.putAll(HASH_KEY, fields).then();
    }

    public boolean isFresh(Instant cachedAt) {
        return cachedAt.isAfter(Instant.now().minus(freshnessWindow));
    }

    public record CachedPercentage(BigDecimal value, Instant cachedAt) {}
}
