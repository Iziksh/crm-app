package com.crm.dto.response;

import com.crm.domain.entity.User;
import com.crm.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record UserAdminResponse(
        Long id,
        String username,
        String email,
        Set<String> roles,
        UserStatus status,
        Long workspaceId,
        Long managerId,
        String managerName,
        LocalDateTime createdAt
) {
    public static UserAdminResponse from(User user) {
        return from(user, null);
    }

    public static UserAdminResponse from(User user, String managerName) {
        return new UserAdminResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getStatus(),
                user.getWorkspaceId(),
                user.getManagerId(),
                managerName,
                user.getCreatedAt()
        );
    }
}
