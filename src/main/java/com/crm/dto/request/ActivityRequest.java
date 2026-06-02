package com.crm.dto.request;

import com.crm.domain.enums.ActivityPriority;
import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ActivityType;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ActivityRequest(
        @NotBlank String title,
        String description,
        ActivityType type,
        ActivityStatus status,
        ActivityPriority priority,
        LocalDate dueDate,
        Long assignedToId,
        Long accountId,
        Long contactId
) {}
