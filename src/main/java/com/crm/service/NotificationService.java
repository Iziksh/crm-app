package com.crm.service;

import com.crm.domain.enums.AlertImportance;
import com.crm.domain.enums.AlertState;
import com.crm.domain.enums.SubscriptionEventType;
import com.crm.dto.response.AlertResponse;
import com.crm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Thin facade kept for backwards compatibility with existing service callers.
 * Delegates to AlertService (OpenCRX Alert model) and CrmEventPublisher.
 */
@Service
@Transactional
public class NotificationService {

    private final AlertService alertService;
    private final CrmEventPublisher eventPublisher;
    private final UserRepository userRepository;

    public NotificationService(AlertService alertService,
                                CrmEventPublisher eventPublisher,
                                UserRepository userRepository) {
        this.alertService = alertService;
        this.eventPublisher = eventPublisher;
        this.userRepository = userRepository;
    }

    public void notify(Long userId, String message, String entityType, Long entityId) {
        userRepository.findById(userId).ifPresent(user -> {
            // Create a direct alert (no dedup — resendDelaySeconds = 0)
            alertService.sendAlert(user.getUsername(), message, null,
                    AlertImportance.NORMAL, 0, entityType, entityId);
            // Publish event so subscriptions can also fire
            if (entityType != null && entityId != null) {
                eventPublisher.publish(entityType, entityId, SubscriptionEventType.OBJECT_CREATION, java.util.Map.of());
            }
        });
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getUnread(String username) {
        return alertService.getForUser(username, List.of(AlertState.NEW));
    }

    @Transactional(readOnly = true)
    public long countUnread(String username) {
        return alertService.countUnread(username);
    }

    public void markRead(Long alertId) {
        alertService.markAsRead(alertId);
    }

    public void markAllRead(String username) {
        alertService.markAllRead(username);
    }
}
