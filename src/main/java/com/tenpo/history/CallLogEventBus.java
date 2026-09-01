package com.tenpo.history;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class CallLogEventBus {

    private final Sinks.Many<CallLogEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void publish(CallLogEvent event) {
        sink.tryEmitNext(event);
    }

    public Flux<CallLogEvent> events() {
        return sink.asFlux();
    }
}
