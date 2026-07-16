package com.crm.billing.dto;

import com.crm.billing.enums.DocumentType;
import jakarta.validation.constraints.NotNull;

/** paymentRequestId comes from the URL path (POST /payment-requests/{id}/convert), not this body. */
public record ConvertRequest(
        @NotNull DocumentType targetDocumentType
) {}
