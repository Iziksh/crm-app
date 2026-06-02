package com.crm.dto.response;

import com.crm.domain.entity.AccountGroup;

import java.time.LocalDateTime;

public record AccountGroupResponse(
        Long id,
        String name,
        String description,
        Long parentId,
        String parentName,
        int memberCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AccountGroupResponse from(AccountGroup g) {
        return new AccountGroupResponse(
                g.getId(),
                g.getName(),
                g.getDescription(),
                g.getParent() != null ? g.getParent().getId() : null,
                g.getParent() != null ? g.getParent().getName() : null,
                g.getMembers().size(),
                g.getCreatedAt(),
                g.getUpdatedAt()
        );
    }
}
