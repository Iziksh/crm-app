package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AddonRequest(
        @NotBlank String name,
        String description,
        LocalDate expiryDate
) {}
