package com.tenpo.history;

import java.time.Instant;

public record CallLogEvent(
    String endpoint,
    String httpMethod,
    String params,
    int statusCode,
    String responseBody,
    String errorMessage,
    Instant calledAt
) {}
