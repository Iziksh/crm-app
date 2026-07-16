package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 12) String password
) {}
