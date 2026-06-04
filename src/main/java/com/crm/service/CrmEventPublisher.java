package com.crm.service;

import com.crm.domain.enums.SubscriptionEventType;
import com.crm.service.event.CrmEntityChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CrmEventPublisher {

    private final ApplicationEventPublisher publisher;

    public CrmEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publishCreated(String entityType, Long entityId) {
        publish(entityType, entityId, SubscriptionEventType.OBJECT_CREATION, Map.of());
    }

    public void publishCreated(String entityType, Long entityId, Map<String, String> attributes) {
        publish(entityType, entityId, SubscriptionEventType.OBJECT_CREATION, attributes);
    }

    public void publishUpdated(String entityType, Long entityId) {
        publish(entityType, entityId, SubscriptionEventType.OBJECT_REPLACEMENT, Map.of());
    }

    public void publishUpdated(String entityType, Long entityId, Map<String, String> attributes) {
        publish(entityType, entityId, SubscriptionEventType.OBJECT_REPLACEMENT, attributes);
    }

    public void publishDeleted(String entityType, Long entityId) {
        publish(entityType, entityId, SubscriptionEventType.OBJECT_REMOVAL, Map.of());
    }

    public void publish(String entityType, Long entityId, SubscriptionEventType eventType,
                        Map<String, String> attributes) {
        publisher.publishEvent(new CrmEntityChangedEvent(this, entityType, entityId, eventType, attributes));
    }
}
