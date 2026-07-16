package com.crm.billing.dto;

import com.crm.billing.entity.TaxDocument;
import com.crm.billing.enums.AllocationStatus;
import com.crm.billing.enums.DiscountType;
import com.crm.billing.enums.DocumentStatus;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.enums.RoundingMode;
import com.crm.billing.enums.VatType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaxDocumentResponse(
        Long id,
        DocumentType documentType,
        String number,
        Integer documentYear,
        DocumentStatus status,
        String currency,
        LocalDate documentDate,
        String freeText,
        VatType vatType,
        DiscountType discountType,
        BigDecimal discountValue,
        RoundingMode roundingMode,
        BigDecimal netTotal,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal preRoundTotal,
        BigDecimal roundingDelta,
        BigDecimal grossTotal,
        AllocationStatus allocationStatus,
        String allocationNumber,
        Long originalDocumentId,
        Long sourcePaymentRequestId,
        LocalDateTime issuedAt,
        Long accountId,
        String accountName,
        String accountTaxId,
        List<LineItemResponse> lineItems,
        List<DocumentPaymentResponse> payments
) {
    public static TaxDocumentResponse from(TaxDocument d, List<LineItemResponse> lineItems, List<DocumentPaymentResponse> payments) {
        return new TaxDocumentResponse(
                d.getId(), d.getDocumentType(), d.getNumber(), d.getDocumentYear(), d.getStatus(),
                d.getCurrency(), d.getDocumentDate(), d.getFreeText(), d.getVatType(),
                d.getDiscountType(), d.getDiscountValue(), d.getRoundingMode(),
                d.getNetTotal(), d.getVatRate(), d.getVatAmount(), d.getPreRoundTotal(),
                d.getRoundingDelta(), d.getGrossTotal(), d.getAllocationStatus(), d.getAllocationNumber(),
                d.getOriginalDocumentId(), d.getSourcePaymentRequestId(), d.getIssuedAt(),
                d.getAccount() != null ? d.getAccount().getId() : null,
                d.getAccount() != null ? d.getAccount().getName() : null,
                d.getAccount() != null ? d.getAccount().getTaxId() : null,
                lineItems, payments);
    }
}
