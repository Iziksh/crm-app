package com.crm.dto.response;

import com.crm.domain.entity.User;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        Set<String> roles,
        boolean enabled,
        Long managerId,
        String managerName,
        Long accountId,
        String accountName,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return from(u, null, null);
    }

    public static UserResponse from(User u, String managerName) {
        return from(u, managerName, null);
    }

    /**
     * The account id comes off the lazy proxy's identifier, which costs no query, but the name
     * must be supplied by the caller — list paths batch-resolve it to avoid an N+1.
     */
    public static UserResponse from(User u, String managerName, String accountName) {
        Long accountId = u.getAccount() != null ? u.getAccount().getId() : null;
        return new UserResponse(
                u.getId(), u.getUsername(), u.getEmail(),
                u.getRoles(), u.isEnabled(), u.getManagerId(), managerName,
                accountId, accountName, u.getCreatedAt()
        );
    }
}
