package com.tenpo.history;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface CallLogRepository extends ReactiveCrudRepository<CallLog, UUID> {}
