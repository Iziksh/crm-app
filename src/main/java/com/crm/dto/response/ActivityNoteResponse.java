package com.crm.dto.response;

import com.crm.domain.entity.ActivityNote;

import java.time.LocalDateTime;

public record ActivityNoteResponse(
        Long id,
        String text,
        String authorName,
        LocalDateTime createdAt
) {
    public static ActivityNoteResponse from(ActivityNote n) {
        return new ActivityNoteResponse(
                n.getId(),
                n.getText(),
                n.getAuthor() != null ? n.getAuthor().getUsername() : "system",
                n.getCreatedAt()
        );
    }
}
