package com.crm.dto.response;

import com.crm.domain.entity.Subscription;
import com.crm.domain.enums.SubscriptionEventType;

import java.time.LocalDateTime;
import java.util.List;

public record SubscriptionResponse(
        Long id,
        String name,
        String description,
        boolean active,
        Long topicId,
        String topicName,
        String entityType,
        List<SubscriptionEventType> eventTypes,
        String filterName0, String filterValue0,
        String filterName1, String filterValue1,
        String filterName2, String filterValue2,
        String filterName3, String filterValue3,
        String filterName4, String filterValue4,
        LocalDateTime createdAt
) {
    public static SubscriptionResponse from(Subscription s) {
        return new SubscriptionResponse(
                s.getId(), s.getName(), s.getDescription(), s.isActive(),
                s.getTopic().getId(), s.getTopic().getName(), s.getTopic().getEntityType(),
                List.copyOf(s.getEventTypes()),
                s.getFilterName0(), s.getFilterValue0(),
                s.getFilterName1(), s.getFilterValue1(),
                s.getFilterName2(), s.getFilterValue2(),
                s.getFilterName3(), s.getFilterValue3(),
                s.getFilterName4(), s.getFilterValue4(),
                s.getCreatedAt()
        );
    }
}
