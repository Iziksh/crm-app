package com.crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record UserRequest(
        @NotBlank String username,
        @Email String email,
        String password,
        Set<String> roles,
        Long managerId,
        /** Required on create — every user must belong to an account. */
        Long accountId
) {}
