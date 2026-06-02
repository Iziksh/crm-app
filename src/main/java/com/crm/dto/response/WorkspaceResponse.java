package com.crm.dto.response;

import com.crm.domain.entity.Workspace;

import java.time.LocalDateTime;
import java.util.List;

public record WorkspaceResponse(
        Long id,
        String name,
        String description,
        int memberCount,
        List<String> memberNames,
        LocalDateTime createdAt
) {
    public static WorkspaceResponse from(Workspace w) {
        return new WorkspaceResponse(
                w.getId(),
                w.getName(),
                w.getDescription(),
                w.getMembers().size(),
                w.getMembers().stream().map(u -> u.getUsername()).toList(),
                w.getCreatedAt()
        );
    }
}
