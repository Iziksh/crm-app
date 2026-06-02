package com.crm.dto.request;

import com.crm.domain.enums.ContactStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ContactRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        String phone,
        String jobTitle,
        String department,
        ContactStatus status,
        String notes,
        Long accountId
) {}
