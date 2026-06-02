package com.crm.dto.response;

import com.crm.domain.entity.Quote;
import com.crm.domain.enums.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record QuoteResponse(
        Long id,
        String quoteNumber,
        String title,
        QuoteStatus status,
        LocalDate validUntil,
        BigDecimal totalAmount,
        String currency,
        Long accountId,
        String accountName,
        Long contactId,
        String contactName,
        Long opportunityId,
        String opportunityName,
        List<QuoteLineItemResponse> lineItems,
        LocalDateTime createdAt
) {
    public static QuoteResponse from(Quote q) {
        return new QuoteResponse(
                q.getId(),
                q.getQuoteNumber(),
                q.getTitle(),
                q.getStatus(),
                q.getValidUntil(),
                q.getTotalAmount(),
                q.getCurrency(),
                q.getAccount() != null ? q.getAccount().getId() : null,
                q.getAccount() != null ? q.getAccount().getName() : null,
                q.getContact() != null ? q.getContact().getId() : null,
                q.getContact() != null ? (q.getContact().getFirstName() + " " + q.getContact().getLastName()) : null,
                q.getOpportunity() != null ? q.getOpportunity().getId() : null,
                q.getOpportunity() != null ? q.getOpportunity().getName() : null,
                q.getLineItems().stream().map(QuoteLineItemResponse::from).toList(),
                q.getCreatedAt()
        );
    }
}
