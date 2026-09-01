package com.tenpo.ratelimit;

import com.tenpo.error.ApiError;
import com.tenpo.history.CallLogEvent;
import com.tenpo.history.CallLogEventBus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Component
public class RateLimitFilter implements WebFilter {

    private static final String HISTORY_PATH_PREFIX = "/api/v1/history";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final CallLogEventBus eventBus;
    private final ObjectMapper objectMapper;
    private final int maxRequests;
    private final long windowSeconds;

    public RateLimitFilter(
            ReactiveStringRedisTemplate redisTemplate,
            CallLogEventBus eventBus,
            ObjectMapper objectMapper,
            @Value("${ratelimit.max-requests:3}") int maxRequests,
            @Value("${ratelimit.window-seconds:60}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long window = Instant.now().getEpochSecond() / windowSeconds;
        String key = "ratelimit:" + window;

        return redisTemplate.opsForValue().increment(key)
            .flatMap(count -> {
                Mono<Void> ensureExpiry = (count == 1)
                    ? redisTemplate.expire(key, Duration.ofSeconds(windowSeconds)).then()
                    : Mono.empty();

                if (count > maxRequests) {
                    return ensureExpiry.then(tooManyRequests(exchange));
                }
                return ensureExpiry.then(chain.filter(exchange));
            });
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();

        ApiError error = new ApiError(
            Instant.now(), 429, "Too Many Requests",
            "Se excedió el límite de %d solicitudes por %d segundos. Intenta nuevamente más tarde."
                .formatted(maxRequests, windowSeconds),
            path);

        if (!path.startsWith(HISTORY_PATH_PREFIX)) {
            eventBus.publish(new CallLogEvent(
                path, exchange.getRequest().getMethod().name(), null, 429, null, error.message(), Instant.now()));
        }

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(error);
        } catch (JacksonException e) {
            bytes = "{\"error\":\"Too Many Requests\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
