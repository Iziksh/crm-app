package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceRequest(
        @NotBlank String name,
        String description
) {}
