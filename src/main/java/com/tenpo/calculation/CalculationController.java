package com.tenpo.calculation;

import com.tenpo.calculation.exception.PercentageUnavailableException;
import com.tenpo.history.CallLogEvent;
import com.tenpo.history.CallLogEventBus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Tag(name = "Calculations", description = "Cálculo de suma más porcentaje dinámico")
@RestController
@RequestMapping("/api/v1/calculations")
public class CalculationController {

    private final CalculationService calculationService;
    private final CallLogEventBus eventBus;
    private final ObjectMapper objectMapper;

    public CalculationController(CalculationService calculationService, CallLogEventBus eventBus, ObjectMapper objectMapper) {
        this.calculationService = calculationService;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public Mono<CalculationResponse> calculate(@Valid @RequestBody CalculationRequest request) {
        return calculationService.calculate(request.num1(), request.num2())
            .doOnSuccess(response -> eventBus.publish(successEvent(request, response)))
            .doOnError(PercentageUnavailableException.class, ex -> eventBus.publish(errorEvent(request, ex)));
    }

    private CallLogEvent successEvent(CalculationRequest request, CalculationResponse response) {
        return new CallLogEvent("/api/v1/calculations", "POST", writeJson(request), 200, writeJson(response), null, Instant.now());
    }

    private CallLogEvent errorEvent(CalculationRequest request, PercentageUnavailableException ex) {
        return new CallLogEvent("/api/v1/calculations", "POST", writeJson(request), 503, null, ex.getMessage(), Instant.now());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            return null;
        }
    }
}
