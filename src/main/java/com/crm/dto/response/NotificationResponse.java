package com.crm.dto.response;

import com.crm.domain.entity.AppNotification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String message,
        String entityType,
        Long entityId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(AppNotification n) {
        return new NotificationResponse(
                n.getId(), n.getMessage(), n.getEntityType(),
                n.getEntityId(), n.isRead(), n.getCreatedAt()
        );
    }
}
