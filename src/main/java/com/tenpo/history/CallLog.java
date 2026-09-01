package com.tenpo.history;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("call_log")
public record CallLog(
    @Id UUID id,
    String endpoint,
    @Column("http_method") String httpMethod,
    String params,
    @Column("status_code") int statusCode,
    @Column("response_body") String responseBody,
    @Column("error_message") String errorMessage,
    @Column("called_at") Instant calledAt
) implements Persistable<UUID> {

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return true;
    }
}
