package com.crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminInviteRequest(
        @NotBlank @Email String email,
        @NotBlank String role,
        @NotNull Long workspaceId
) {}
