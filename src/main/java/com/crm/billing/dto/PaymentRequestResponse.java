package com.crm.billing.dto;

import com.crm.billing.entity.PaymentRequest;
import com.crm.billing.enums.PaymentRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentRequestResponse(
        Long id,
        String number,
        Integer documentYear,
        PaymentRequestStatus status,
        String currency,
        LocalDate documentDate,
        String freeText,
        BigDecimal totalAmount,
        Long accountId,
        String accountName,
        String accountTaxId,
        Long convertedDocumentId,
        List<LineItemResponse> lineItems,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentRequestResponse from(PaymentRequest p, List<LineItemResponse> lineItems) {
        return new PaymentRequestResponse(
                p.getId(), p.getNumber(), p.getDocumentYear(), p.getStatus(), p.getCurrency(),
                p.getDocumentDate(), p.getFreeText(), p.getTotalAmount(),
                p.getAccount() != null ? p.getAccount().getId() : null,
                p.getAccount() != null ? p.getAccount().getName() : null,
                p.getAccount() != null ? p.getAccount().getTaxId() : null,
                p.getConvertedDocumentId(), lineItems, p.getCreatedAt(), p.getUpdatedAt());
    }
}
