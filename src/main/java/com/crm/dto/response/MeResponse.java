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
        Long accountId,
        String accountName,
        String status,
        LocalDateTime createdAt
) {
    /**
     * The account name is resolved by the caller rather than read off {@code u.getAccount()}:
     * the authenticated principal is detached (UserDetailsServiceImpl runs outside a transaction),
     * so touching the lazy association here would throw LazyInitializationException.
     */
    public static MeResponse from(User u, String accountName) {
        Long accountId = u.getAccount() != null ? u.getAccount().getId() : null;
        return new MeResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getRoles(), u.getWorkspaceId(),
                accountId, accountName,
                u.getStatus() != null ? u.getStatus().name() : (u.isEnabled() ? "ACTIVE" : "DISABLED"),
                u.getCreatedAt());
    }
}