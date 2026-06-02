package com.crm.dto.request;

import com.crm.domain.enums.SavedSearchScope;
import jakarta.validation.constraints.NotBlank;

public record SavedSearchRequest(
        @NotBlank String name,
        SavedSearchScope scope,
        String filterJson
) {}
