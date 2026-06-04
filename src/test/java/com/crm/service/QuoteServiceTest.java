package com.crm.service;

import com.crm.domain.entity.Quote;
import com.crm.domain.entity.SalesOrder;
import com.crm.domain.enums.QuoteStatus;
import com.crm.dto.request.QuoteRequest;
import com.crm.dto.response.QuoteResponse;
import com.crm.dto.response.SalesOrderResponse;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.OpportunityRepository;
import com.crm.repository.QuoteRepository;
import com.crm.repository.SalesOrderRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock QuoteRepository quoteRepository;
    @Mock SalesOrderRepository salesOrderRepository;
    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock ContactRepository contactRepository;
    @Mock OpportunityRepository opportunityRepository;

    @InjectMocks QuoteService quoteService;

    @Test
    void create_savesQuote_andReturnsResponse() {
        QuoteRequest req = new QuoteRequest("Q-2026-001", QuoteStatus.DRAFT, null, "USD", null, null, null, null, null, List.of());
        Quote saved = new Quote();
        saved.setTitle("Q-2026-001");
        saved.setStatus(QuoteStatus.DRAFT);
        when(quoteRepository.save(any())).thenReturn(saved);

        QuoteResponse response = quoteService.create(req, "admin");

        assertThat(response.title()).isEqualTo("Q-2026-001");
        verify(quoteRepository).save(any());
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(quoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> quoteService.findById(99L));
    }

    @Test
    void delete_callsRepositoryDelete() {
        Quote quote = new Quote();
        quote.setTitle("Old Quote");
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

        quoteService.delete(1L);

        verify(quoteRepository).delete(quote);
    }

    @Test
    void count_delegatesToRepository_withStatus() {
        when(quoteRepository.countByStatus(QuoteStatus.WON)).thenReturn(2L);

        assertThat(quoteService.count(QuoteStatus.WON)).isEqualTo(2L);
    }

    @Test
    void convertToOrder_throwsNotFound_whenQuoteMissing() {
        when(quoteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> quoteService.convertToOrder(99L));
    }

    @Test
    void convertToOrder_throwsBadRequest_whenQuoteNotWon() {
        Quote quote = new Quote();
        quote.setTitle("Draft Quote");
        quote.setStatus(QuoteStatus.DRAFT);
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));

        assertThrows(BadRequestException.class, () -> quoteService.convertToOrder(1L));
    }

    @Test
    void convertToOrder_createsSalesOrder_whenQuoteIsWon() {
        Quote quote = new Quote();
        quote.setTitle("Won Quote");
        quote.setStatus(QuoteStatus.WON);

        SalesOrder order = new SalesOrder();
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
        when(salesOrderRepository.save(any())).thenReturn(order);

        SalesOrderResponse response = quoteService.convertToOrder(1L);

        assertThat(response).isNotNull();
        verify(salesOrderRepository).save(any());
    }

    @Test
    void findByOpportunity_delegatesToRepository() {
        when(quoteRepository.findByOpportunity_Id(5L)).thenReturn(List.of());

        List<QuoteResponse> result = quoteService.findByOpportunity(5L);

        assertThat(result).isEmpty();
        verify(quoteRepository).findByOpportunity_Id(5L);
    }
}
