package com.crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifySignupRequest(
        @NotBlank @Email String email,
        @NotBlank String otp
) {}
