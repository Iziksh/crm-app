package com.crm.service.event;

import com.crm.domain.enums.SubscriptionEventType;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class CrmEntityChangedEvent extends ApplicationEvent {

    private final String entityType;
    private final Long entityId;
    private final SubscriptionEventType eventType;
    private final Map<String, String> attributes;

    public CrmEntityChangedEvent(Object source, String entityType, Long entityId,
                                  SubscriptionEventType eventType, Map<String, String> attributes) {
        super(source);
        this.entityType = entityType;
        this.entityId = entityId;
        this.eventType = eventType;
        this.attributes = attributes != null ? attributes : Map.of();
    }

    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public SubscriptionEventType getEventType() { return eventType; }
    public Map<String, String> getAttributes() { return attributes; }
}
