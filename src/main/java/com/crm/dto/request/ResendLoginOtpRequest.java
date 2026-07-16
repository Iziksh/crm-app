package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResendLoginOtpRequest(
        @NotBlank String username
) {}
