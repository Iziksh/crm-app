package com.crm.billing.dto;

import com.crm.billing.entity.DocumentPayment;
import com.crm.billing.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentPaymentResponse(
        Long id,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        LocalDate receivedAt
) {
    public static DocumentPaymentResponse from(DocumentPayment p) {
        return new DocumentPaymentResponse(p.getId(), p.getPaymentMethod(), p.getAmount(), p.getReceivedAt());
    }
}
