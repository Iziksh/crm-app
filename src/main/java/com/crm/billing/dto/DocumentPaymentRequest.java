package com.crm.billing.dto;

import com.crm.billing.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentPaymentRequest(
        @NotNull PaymentMethod paymentMethod,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        LocalDate receivedAt
) {}
