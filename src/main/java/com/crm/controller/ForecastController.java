package com.crm.controller;

import com.crm.dto.response.ForecastSummaryResponse;
import com.crm.service.ForecastService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/forecast")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping("/leads")
    public ResponseEntity<ForecastSummaryResponse> leads() {
        return ResponseEntity.ok(forecastService.leadForecast());
    }

    @GetMapping("/opportunities")
    public ResponseEntity<ForecastSummaryResponse> opportunities() {
        return ResponseEntity.ok(forecastService.opportunityForecast());
    }

    @GetMapping("/quotes")
    public ResponseEntity<ForecastSummaryResponse> quotes() {
        return ResponseEntity.ok(forecastService.quoteForecast());
    }
}
