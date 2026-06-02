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
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(), u.getUsername(), u.getEmail(),
                u.getRoles(), u.isEnabled(), u.getCreatedAt()
        );
    }
}
