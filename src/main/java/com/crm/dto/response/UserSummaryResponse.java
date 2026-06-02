package com.crm.dto.response;

import com.crm.domain.entity.User;

public record UserSummaryResponse(Long id, String username, String email) {
    public static UserSummaryResponse from(User u) {
        return new UserSummaryResponse(u.getId(), u.getUsername(), u.getEmail());
    }
}
