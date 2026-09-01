package com.tenpo.calculation;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CalculationRequest(
    @NotNull(message = "num1 es obligatorio") BigDecimal num1,
    @NotNull(message = "num2 es obligatorio") BigDecimal num2
) {}
