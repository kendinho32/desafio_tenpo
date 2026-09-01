package com.tenpo.calculation;

import java.math.BigDecimal;

public record CalculationResponse(
    BigDecimal num1,
    BigDecimal num2,
    BigDecimal percentageApplied,
    BigDecimal result
) {}
