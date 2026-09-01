package com.tenpo.ratelimit;

import com.tenpo.history.CallLogEventBus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimitFilterUnitTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void failsOpenWhenRedisIsUnavailable() {
        // I4: a Redis outage on the rate-limit counter must not take down the whole API with a
        // non-uniform error body - it should let the request through (fail-open).
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> valueOps = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(any())).thenReturn(Mono.error(new RuntimeException("redis unreachable")));

        CallLogEventBus eventBus = mock(CallLogEventBus.class);
        RateLimitFilter filter = new RateLimitFilter(redisTemplate, eventBus, objectMapper, 3, 60);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/history").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        verify(chain).filter(exchange);
    }
}
