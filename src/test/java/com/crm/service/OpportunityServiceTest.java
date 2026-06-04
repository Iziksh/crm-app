package com.crm.service;

import com.crm.domain.entity.Opportunity;
import com.crm.domain.enums.OpportunityStage;
import com.crm.dto.request.OpportunityRequest;
import com.crm.dto.response.OpportunityResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.OpportunityRepository;
import com.crm.repository.QuoteRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    @Mock OpportunityRepository opportunityRepository;
    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock ContactRepository contactRepository;
    @Mock LeadRepository leadRepository;
    @Mock QuoteRepository quoteRepository;
    @Mock NotificationService notificationService;
    @Mock CrmEventPublisher eventPublisher;

    @InjectMocks OpportunityService opportunityService;

    @Test
    void create_savesOpportunity_andReturnsResponse() {
        OpportunityRequest req = new OpportunityRequest(
                "Big Deal", OpportunityStage.PROSPECTING, new BigDecimal("50000"),
                "USD", 40, null, null, null, null, null, null);
        Opportunity saved = new Opportunity();
        saved.setName("Big Deal");
        saved.setStage(OpportunityStage.PROSPECTING);
        when(opportunityRepository.save(any())).thenReturn(saved);

        OpportunityResponse response = opportunityService.create(req, "admin");

        assertThat(response.name()).isEqualTo("Big Deal");
        verify(opportunityRepository).save(any());
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(opportunityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> opportunityService.findById(99L));
    }

    @Test
    void delete_callsRepositoryDelete() {
        Opportunity opp = new Opportunity();
        opp.setName("Deal");
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opp));

        opportunityService.delete(1L);

        verify(opportunityRepository).delete(opp);
    }

    @Test
    void countByStage_delegatesToRepository() {
        when(opportunityRepository.countByStage(OpportunityStage.WON)).thenReturn(3L);

        assertThat(opportunityService.countByStage(OpportunityStage.WON)).isEqualTo(3L);
    }

    @Test
    void sumPipelineAmount_returnsValueFromRepository() {
        when(opportunityRepository.sumPipelineAmount()).thenReturn(new BigDecimal("250000"));

        assertThat(opportunityService.sumPipelineAmount()).isEqualByComparingTo("250000");
    }

    @Test
    void sumPipelineAmount_returnsZero_whenRepositoryReturnsNull() {
        when(opportunityRepository.sumPipelineAmount()).thenReturn(null);

        assertThat(opportunityService.sumPipelineAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void findQuotes_returnsQuotesForOpportunity() {
        when(quoteRepository.findByOpportunity_Id(1L)).thenReturn(List.of());

        List<?> quotes = opportunityService.findQuotes(1L);

        assertThat(quotes).isEmpty();
        verify(quoteRepository).findByOpportunity_Id(1L);
    }
}
