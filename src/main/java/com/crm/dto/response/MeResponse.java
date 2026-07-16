package com.crm.dto.response;

import com.crm.domain.entity.User;

import java.time.LocalDateTime;
import java.util.Set;

public record MeResponse(
        Long id,
        String username,
        String email,
        Set<String> roles,
        Long workspaceId,
        String status,
        LocalDateTime createdAt
) {
    public static MeResponse from(User u) {
        return new MeResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getRoles(), u.getWorkspaceId(),
                u.getStatus() != null ? u.getStatus().name() : (u.isEnabled() ? "ACTIVE" : "DISABLED"),
                u.getCreatedAt());
    }
}
