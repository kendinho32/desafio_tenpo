package com.tenpo.error;

import com.tenpo.calculation.exception.PercentageUnavailableException;
import com.tenpo.history.CallLogEvent;
import com.tenpo.history.CallLogEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, ServerHttpRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        log.warn("Respuesta de error {} en {} {}: {}", status.value(), request.getMethod(), request.getPath().value(), message);
        logIfNotHistory(request, status.value(), message);
        return build(status, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, ServerHttpRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getPath().value(), ex);
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
