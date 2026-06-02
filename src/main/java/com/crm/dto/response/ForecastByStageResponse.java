package com.crm.dto.response;

import java.math.BigDecimal;

public record ForecastByStageResponse(
        String stage,
        long count,
        BigDecimal amount,
        BigDecimal weighted
) {}
