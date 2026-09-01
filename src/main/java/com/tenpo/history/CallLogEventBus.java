package com.tenpo.history;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class CallLogEventBus {

    private static final Logger log = LoggerFactory.getLogger(CallLogEventBus.class);

    // autoCancel=false: with the default autoCancel=true, the sink terminates permanently the
    // moment its lone subscriber (CallLogPersister) disappears - including a momentary gap while
    // CallLogPersister.retry() is re-subscribing after an error. Once terminated, every future
    // publish() would fail with FAIL_TERMINATED forever, defeating the retry() safety net
    // entirely (verified empirically: with autoCancel=true, a resubscription after an error does
    // attach a new subscriber, but that subscriber never receives subsequently published events).
    private final Sinks.Many<CallLogEvent> sink = Sinks.many().multicast().onBackpressureBuffer(256, false);

    public void publish(CallLogEvent event) {
        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isFailure()) {
            log.warn("Evento de historial descartado ({}) para {} {}", result, event.httpMethod(), event.endpoint());
        }
    }

    public Flux<CallLogEvent> events() {
        return sink.asFlux();
    }
}
