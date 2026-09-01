package com.tenpo.history;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CallLogPersisterTest {

    @Test
    void continuesProcessingAfterPersistenceFailure() {
        CallLogEventBus eventBus = new CallLogEventBus();
        CallLogRepository repository = mock(CallLogRepository.class);
        when(repository.save(any()))
            .thenReturn(Mono.error(new RuntimeException("boom")))
            .thenReturn(Mono.just(mock(CallLog.class)));

        CallLogPersister persister = new CallLogPersister(eventBus, repository);
        persister.subscribeToEvents();

        eventBus.publish(new CallLogEvent("/a", "GET", null, 200, null, null, Instant.now()));
        eventBus.publish(new CallLogEvent("/b", "GET", null, 200, null, null, Instant.now()));

        verify(repository, timeout(2000).times(2)).save(any());
    }
}
