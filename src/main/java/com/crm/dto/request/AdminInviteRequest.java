package com.crm.dto.request;

public record AdminInviteRequest(
        String email,
        String role,
        Long workspaceId
) {}
