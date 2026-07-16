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
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return from(u, null);
    }

    public static UserResponse from(User u, String managerName) {
        return new UserResponse(
                u.getId(), u.getUsername(), u.getEmail(),
                u.getRoles(), u.isEnabled(), u.getManagerId(), managerName, u.getCreatedAt()
        );
    }
}
