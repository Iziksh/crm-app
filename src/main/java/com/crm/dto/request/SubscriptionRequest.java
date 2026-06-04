package com.crm.dto.request;

import com.crm.domain.enums.SubscriptionEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubscriptionRequest(
        @NotBlank String name,
        String description,
        boolean active,
        @NotNull Long topicId,
        List<SubscriptionEventType> eventTypes,
        String filterName0, String filterValue0,
        String filterName1, String filterValue1,
        String filterName2, String filterValue2,
        String filterName3, String filterValue3,
        String filterName4, String filterValue4
) {}
