package com.tenpo.history;

import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class CallHistoryService {

    private final R2dbcEntityTemplate template;
    private final CallLogRepository repository;

    public CallHistoryService(R2dbcEntityTemplate template, CallLogRepository repository) {
        this.template = template;
        this.repository = repository;
    }

    public Mono<PageResponse<CallLog>> findHistory(int page, int size) {
        Query query = Query.empty()
            .sort(Sort.by(Sort.Direction.DESC, "calledAt"))
            .limit(size)
            .offset((long) page * size);

        Mono<List<CallLog>> content = template.select(CallLog.class).matching(query).all().collectList();
        Mono<Long> total = repository.count();

        return Mono.zip(content, total)
            .map(tuple -> {
                long totalElements = tuple.getT2();
                int totalPages = (int) Math.ceil((double) totalElements / size);
                return new PageResponse<>(tuple.getT1(), page, size, totalElements, totalPages);
            });
    }
}
