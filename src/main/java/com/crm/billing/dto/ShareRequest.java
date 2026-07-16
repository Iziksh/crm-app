package com.crm.billing.dto;

import com.crm.billing.enums.DocumentOwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShareRequest(
        @NotNull DocumentOwnerType ownerType,
        @NotNull Long documentId,
        @NotNull ShareChannel channel,
        /** Email address for ShareChannel.EMAIL, phone number (E.164-ish) for ShareChannel.WHATSAPP. */
        @NotBlank String target
) {
    public enum ShareChannel { EMAIL, WHATSAPP }
}
