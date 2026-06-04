package com.crm.dto.response;

import com.crm.domain.entity.ScheduledTask;
import com.crm.domain.enums.AlertImportance;
import com.crm.domain.enums.TaskStatus;

import java.time.LocalDateTime;

public record ScheduledTaskResponse(
        Long id,
        String workflowKey,
        String workflowName,
        String targetEntityType,
        Long targetEntityId,
        String recipientUsername,
        TaskStatus status,
        AlertImportance priority,
        LocalDateTime scheduledAt,
        LocalDateTime createdAt,
        int attemptCount,
        int maxAttempts,
        LocalDateTime lastAttemptedAt,
        LocalDateTime completedAt,
        String failureReason,
        String cancelIfField,
        String cancelIfValue
) {
    public static ScheduledTaskResponse from(ScheduledTask t) {
        return new ScheduledTaskResponse(
                t.getId(), t.getWorkflowKey(), t.getWorkflowName(),
                t.getTargetEntityType(), t.getTargetEntityId(),
                t.getRecipient() != null ? t.getRecipient().getUsername() : null,
                t.getStatus(), t.getPriority(),
                t.getScheduledAt(), t.getCreatedAt(),
                t.getAttemptCount(), t.getMaxAttempts(),
                t.getLastAttemptedAt(), t.getCompletedAt(),
                t.getFailureReason(), t.getCancelIfField(), t.getCancelIfValue()
        );
    }
}
