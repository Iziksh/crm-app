package com.crm.billing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomerQuickCreateRequest(
        @NotBlank String name,
        @Pattern(regexp = "^\\d{9}$", message = "Tax id must be exactly 9 digits") String taxId,
        String phone,
        @Email String email,
        String address,
        String industry,
        String website
) {}
