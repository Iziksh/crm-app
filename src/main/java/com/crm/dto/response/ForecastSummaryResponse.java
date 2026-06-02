package com.crm.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ForecastSummaryResponse(
        String period,
        BigDecimal totalAmount,
        BigDecimal weightedAmount,
        long count,
        List<ForecastByStageResponse> byStage
) {}
