package com.crm.dto.response;

import com.crm.domain.entity.Activity;
import com.crm.domain.enums.ActivityPriority;
import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ActivityType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ActivityResponse(
        Long id,
        String title,
        String description,
        ActivityType type,
        ActivityStatus status,
        ActivityPriority priority,
        LocalDate dueDate,
        LocalDateTime resolvedAt,
        Long assignedToId,
        String assignedToName,
        Long accountId,
        String accountName,
        Long contactId,
        String contactName,
        List<ActivityNoteResponse> notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ActivityResponse from(Activity a) {
        return new ActivityResponse(
                a.getId(),
                a.getTitle(),
                a.getDescription(),
                a.getType(),
                a.getStatus(),
                a.getPriority(),
                a.getDueDate(),
                a.getResolvedAt(),
                a.getAssignedTo() != null ? a.getAssignedTo().getId() : null,
                a.getAssignedTo() != null ? a.getAssignedTo().getUsername() : null,
                a.getAccount() != null ? a.getAccount().getId() : null,
                a.getAccount() != null ? a.getAccount().getName() : null,
                a.getContact() != null ? a.getContact().getId() : null,
                a.getContact() != null ? (a.getContact().getFirstName() + " " + a.getContact().getLastName()) : null,
                a.getNotes().stream().map(ActivityNoteResponse::from).toList(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
