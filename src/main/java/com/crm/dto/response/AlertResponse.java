package com.crm.dto.response;

import com.crm.domain.entity.Alert;
import com.crm.domain.enums.AlertImportance;
import com.crm.domain.enums.AlertState;

import java.time.LocalDateTime;

public record AlertResponse(
        Long id,
        String name,
        String description,
        AlertState alertState,
        AlertImportance importance,
        String entityType,
        Long entityId,
        LocalDateTime createdAt
) {
    public static AlertResponse from(Alert a) {
        return new AlertResponse(
                a.getId(), a.getName(), a.getDescription(),
                a.getAlertState(), a.getImportance(),
                a.getEntityType(), a.getEntityId(), a.getCreatedAt()
        );
    }
}
