package com.crm.service;

import com.crm.domain.entity.Subscription;
import com.crm.domain.enums.AlertImportance;
import com.crm.repository.SubscriptionRepository;
import com.crm.repository.TopicRepository;
import com.crm.service.event.CrmEntityChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Mirrors OpenCRX SubscriptionHandlerServlet.
 * Listens to CrmEntityChangedEvents and fires alerts for all matching subscriptions.
 */
@Service
public class SubscriptionHandlerService {

    private final SubscriptionRepository subscriptionRepository;
    private final TopicRepository topicRepository;
    private final AlertService alertService;
    private final SubscriptionMatcherService matcher;

    public SubscriptionHandlerService(SubscriptionRepository subscriptionRepository,
                                       TopicRepository topicRepository,
                                       AlertService alertService,
                                       SubscriptionMatcherService matcher) {
        this.subscriptionRepository = subscriptionRepository;
        this.topicRepository = topicRepository;
        this.alertService = alertService;
        this.matcher = matcher;
    }

    @EventListener
    @Transactional
    public void handleEntityChanged(CrmEntityChangedEvent event) {
        try {
            topicRepository.findByEntityType(event.getEntityType()).ifPresent(topic -> {
                if (!topic.isSendAlertEnabled()) return;
                List<Subscription> subs = subscriptionRepository.findByTopic_IdAndActiveTrue(topic.getId());
                for (Subscription sub : subs) {
                    if (sub.getUser() == null || !sub.getUser().isEnabled()) continue;
                    if (matcher.matches(sub, event.getEntityType(), event.getEventType(), event.getAttributes())) {
                        String alertName = topic.getName();
                        String description = event.getEntityType() + " #" + event.getEntityId()
                                + " — " + event.getEventType().name().replace("OBJECT_", "");
                        alertService.sendAlert(
                                sub.getUser().getUsername(),
                                alertName,
                                description,
                                AlertImportance.NORMAL,
                                60,
                                event.getEntityType(),
                                event.getEntityId()
                        );
                    }
                }
            });
        } catch (Exception e) {
            // Never let subscription processing roll back the triggering transaction
        }
    }
}
