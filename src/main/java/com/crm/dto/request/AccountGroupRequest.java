package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AccountGroupRequest(
        @NotBlank String name,
        String description,
        Long parentId
) {}
