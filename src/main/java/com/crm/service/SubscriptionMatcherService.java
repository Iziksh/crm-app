package com.crm.service;

import com.crm.domain.entity.Subscription;
import com.crm.domain.enums.SubscriptionEventType;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Mirrors OpenCRX UserHomes.subscriptionMatches().
 * Filter logic: multiple values within a slot → OR; multiple slots → AND; "!" prefix → negation.
 */
@Service
public class SubscriptionMatcherService {

    public boolean matches(Subscription sub, String entityType,
                           SubscriptionEventType eventType, Map<String, String> attributes) {
        return topicMatches(sub, entityType)
                && eventTypeMatches(sub, eventType)
                && filterMatches(sub, attributes);
    }

    private boolean topicMatches(Subscription sub, String entityType) {
        return sub.getTopic() != null && entityType.equals(sub.getTopic().getEntityType());
    }

    private boolean eventTypeMatches(Subscription sub, SubscriptionEventType eventType) {
        return sub.getEventTypes().isEmpty() || sub.getEventTypes().contains(eventType);
    }

    private boolean filterMatches(Subscription sub, Map<String, String> attributes) {
        return slotMatches(sub.getFilterName0(), sub.getFilterValue0(), attributes)
                && slotMatches(sub.getFilterName1(), sub.getFilterValue1(), attributes)
                && slotMatches(sub.getFilterName2(), sub.getFilterValue2(), attributes)
                && slotMatches(sub.getFilterName3(), sub.getFilterValue3(), attributes)
                && slotMatches(sub.getFilterName4(), sub.getFilterValue4(), attributes);
    }

    // Returns true if the slot is unconfigured OR the attribute value matches (OR across comma-separated values).
    private boolean slotMatches(String filterName, String filterValue, Map<String, String> attributes) {
        if (filterName == null || filterName.isBlank()) return true;
        if (filterValue == null || filterValue.isBlank()) return true;
        String actual = attributes.getOrDefault(filterName, "");
        for (String token : filterValue.split(",")) {
            token = token.trim();
            if (token.startsWith("!")) {
                String negated = token.substring(1);
                if (!actual.equalsIgnoreCase(negated)) return true;
            } else {
                if (actual.equalsIgnoreCase(token)) return true;
            }
        }
        return false;
    }
}
