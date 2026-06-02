package com.crm.service;

import com.crm.domain.entity.Lead;
import com.crm.domain.entity.Opportunity;
import com.crm.domain.entity.Quote;
import com.crm.domain.enums.LeadStatus;
import com.crm.domain.enums.OpportunityStage;
import com.crm.domain.enums.QuoteStatus;
import com.crm.dto.response.ForecastByStageResponse;
import com.crm.dto.response.ForecastSummaryResponse;
import com.crm.repository.LeadRepository;
import com.crm.repository.OpportunityRepository;
import com.crm.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ForecastService {

    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final QuoteRepository quoteRepository;

    public ForecastService(LeadRepository leadRepository,
                           OpportunityRepository opportunityRepository,
                           QuoteRepository quoteRepository) {
        this.leadRepository = leadRepository;
        this.opportunityRepository = opportunityRepository;
        this.quoteRepository = quoteRepository;
    }

    public ForecastSummaryResponse leadForecast() {
        List<Lead> leads = leadRepository.findByStatusNot(LeadStatus.LOST);

        Map<String, List<Lead>> byStatus = leads.stream()
                .collect(Collectors.groupingBy(l -> l.getStatus().name()));

        List<ForecastByStageResponse> byStage = byStatus.entrySet().stream()
                .map(e -> {
                    BigDecimal sum = e.getValue().stream()
                            .map(Lead::getEstimatedValue)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ForecastByStageResponse(e.getKey(), e.getValue().size(), sum, null);
                })
                .sorted((a, b) -> a.stage().compareTo(b.stage()))
                .toList();

        BigDecimal total = byStage.stream()
                .map(ForecastByStageResponse::amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ForecastSummaryResponse(currentPeriod(), total, null, leads.size(), byStage);
    }

    public ForecastSummaryResponse opportunityForecast() {
        List<Opportunity> opps = opportunityRepository.findByStageNot(OpportunityStage.LOST);

        Map<String, List<Opportunity>> byStage = opps.stream()
                .collect(Collectors.groupingBy(o -> o.getStage().name()));

        List<ForecastByStageResponse> byStageList = byStage.entrySet().stream()
                .map(e -> {
                    BigDecimal sum = e.getValue().stream()
                            .map(Opportunity::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal weighted = e.getValue().stream()
                            .filter(o -> o.getAmount() != null && o.getProbability() != null)
                            .map(o -> o.getAmount().multiply(
                                    BigDecimal.valueOf(o.getProbability()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ForecastByStageResponse(e.getKey(), e.getValue().size(), sum, weighted);
                })
                .sorted((a, b) -> a.stage().compareTo(b.stage()))
                .toList();

        BigDecimal total = byStageList.stream()
                .map(ForecastByStageResponse::amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWeighted = byStageList.stream()
                .map(ForecastByStageResponse::weighted)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ForecastSummaryResponse(currentPeriod(), total, totalWeighted, opps.size(), byStageList);
    }

    public ForecastSummaryResponse quoteForecast() {
        List<Quote> quotes = quoteRepository.findAll().stream()
                .filter(q -> q.getStatus() != QuoteStatus.LOST && q.getStatus() != QuoteStatus.EXPIRED)
                .toList();

        Map<String, List<Quote>> byStatus = quotes.stream()
                .collect(Collectors.groupingBy(q -> q.getStatus().name()));

        List<ForecastByStageResponse> byStage = byStatus.entrySet().stream()
                .map(e -> {
                    BigDecimal sum = e.getValue().stream()
                            .map(Quote::getTotalAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ForecastByStageResponse(e.getKey(), e.getValue().size(), sum, null);
                })
                .sorted((a, b) -> a.stage().compareTo(b.stage()))
                .toList();

        BigDecimal total = byStage.stream()
                .map(ForecastByStageResponse::amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ForecastSummaryResponse(currentPeriod(), total, null, quotes.size(), byStage);
    }

    private String currentPeriod() {
        LocalDate now = LocalDate.now();
        int quarter = (now.getMonthValue() - 1) / 3 + 1;
        return now.getYear() + "-Q" + quarter;
    }
}
