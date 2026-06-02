package com.crm.dto.request;

import com.crm.domain.enums.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AccountRequest(
        @NotBlank String name,
        String industry,
        String website,
        String phone,
        @Email String email,
        String address,
        AccountType type,
        String notes
) {}
