package com.tenpo.error;

import com.tenpo.calculation.exception.PercentageUnavailableException;
import com.tenpo.history.CallLogEvent;
import com.tenpo.history.CallLogEventBus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String HISTORY_PATH_PREFIX = "/api/v1/history";

    private final CallLogEventBus eventBus;

    public GlobalExceptionHandler(CallLogEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @ExceptionHandler(PercentageUnavailableException.class)
    public ResponseEntity<ApiError> handlePercentageUnavailable(PercentageUnavailableException ex, ServerHttpRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleValidation(WebExchangeBindException ex, ServerHttpRequest request) {
        String message = ex.getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        String finalMessage = message.isBlank() ? "Solicitud inválida." : message;
        logIfNotHistory(request, 400, finalMessage);
        return build(HttpStatus.BAD_REQUEST, finalMessage, request);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ApiError> handleInput(ServerWebInputException ex, ServerHttpRequest request) {
        String message = "Solicitud inválida: " + ex.getReason();
        logIfNotHistory(request, 400, message);
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, ServerHttpRequest request) {
        logIfNotHistory(request, 500, "Error interno inesperado.");
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno inesperado.", request);
    }

    private void logIfNotHistory(ServerHttpRequest request, int status, String message) {
        String path = request.getPath().value();
        if (!path.startsWith(HISTORY_PATH_PREFIX)) {
            eventBus.publish(new CallLogEvent(path, request.getMethod().name(), null, status, null, message, Instant.now()));
        }
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, ServerHttpRequest request) {
        ApiError error = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getPath().value());
        return ResponseEntity.status(status).body(error);
    }
}
