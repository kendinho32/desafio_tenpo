package com.tenpo.history;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CallLogPersister {

    private static final Logger log = LoggerFactory.getLogger(CallLogPersister.class);

    private final CallLogEventBus eventBus;
    private final CallLogRepository repository;

    public CallLogPersister(CallLogEventBus eventBus, CallLogRepository repository) {
        this.eventBus = eventBus;
        this.repository = repository;
    }

    @PostConstruct
    void subscribeToEvents() {
        eventBus.events()
            .flatMap(this::persist)
            .subscribe();
    }

    private Mono<Void> persist(CallLogEvent event) {
        CallLog entity = new CallLog(
            UUID.randomUUID(),
            event.endpoint(),
            event.httpMethod(),
            event.params(),
            event.statusCode(),
            event.responseBody(),
            event.errorMessage(),
            event.calledAt());

        return repository.save(entity)
            .onErrorResume(ex -> {
                log.warn("No se pudo persistir el registro de historial para {} {}: {}",
                    event.httpMethod(), event.endpoint(), ex.getMessage());
                return Mono.empty();
            })
            .then();
    }
}
