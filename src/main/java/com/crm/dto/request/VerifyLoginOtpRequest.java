package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyLoginOtpRequest(
        @NotBlank String username,
        @NotBlank String otp,
        boolean trustDevice
) {}
