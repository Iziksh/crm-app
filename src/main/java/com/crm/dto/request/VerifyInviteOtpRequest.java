package com.crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyInviteOtpRequest(
        @NotBlank @Email String email,
        @NotBlank String otp,
        @NotBlank String password
) {}
