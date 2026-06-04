package com.crm.service;

import com.crm.domain.entity.Alert;
import com.crm.domain.entity.User;
import com.crm.domain.enums.AlertImportance;
import com.crm.domain.enums.AlertState;
import com.crm.domain.enums.NotificationChannel;
import com.crm.dto.response.AlertResponse;
import com.crm.repository.AlertRepository;
import com.crm.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    public AlertService(AlertRepository alertRepository, UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create an alert for each username in the comma/semicolon-delimited toUsernames string.
     * Mirrors OpenCRX Base.sendAlert() including resend-delay deduplication.
     * resendDelaySeconds = 0 means no deduplication (always create).
     */
    /** Simple send — for backwards-compatible callers (NotificationService). */
    public void sendAlert(String toUsernames, String name, String description,
                          AlertImportance importance, int resendDelaySeconds,
                          String entityType, Long entityId) {
        sendAlertFull(toUsernames, null, name, description, importance,
                resendDelaySeconds, entityType, entityId, buildDeepLink(entityType, entityId));
    }

    /** Full send — used by ScheduledTaskService with topicKey and deepLinkUrl. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendAlertFull(String toUsernames, String topicKey,
                               String name, String description,
                               AlertImportance importance, int resendDelaySeconds,
                               String entityType, Long entityId, String deepLinkUrl) {
        if (toUsernames == null || toUsernames.isBlank()) return;
        String resolvedName = (name == null || name.isBlank()) ? "--" : name;
        for (String token : toUsernames.split("[,;\\s]+")) {
            String username = token.trim();
            if (username.isEmpty()) continue;
            userRepository.findByUsername(username).ifPresent(user ->
                    createAlertForUser(user, topicKey, resolvedName, description, importance,
                            resendDelaySeconds, entityType, entityId, deepLinkUrl)
            );
        }
    }

    private void createAlertForUser(User user, String topicKey, String name, String description,
                                     AlertImportance importance, int resendDelaySeconds,
                                     String entityType, Long entityId, String deepLinkUrl) {
        // Dedup check using structured key
        String dedupKey = buildDedupKey(topicKey, entityType, entityId, user.getId(), resendDelaySeconds);
        if (resendDelaySeconds > 0 && entityType != null && entityId != null) {
            boolean exists = alertRepository.findFirstByUser_IdAndEntityTypeAndEntityIdAndResendCutoffAtAfter(
                    user.getId(), entityType, entityId, LocalDateTime.now()).isPresent();
            if (exists) return;
        }
        if (dedupKey != null && alertRepository.existsByDedupKey(dedupKey)) return;

        Alert alert = new Alert();
        alert.setUser(user);
        alert.setTopicKey(topicKey);
        alert.setName(name);
        alert.setDescription(description);
        alert.setAlertState(AlertState.NEW);
        alert.setImportance(importance != null ? importance : AlertImportance.NORMAL);
        alert.setChannel(NotificationChannel.IN_APP);
        alert.setEntityType(entityType);
        alert.setEntityId(entityId);
        alert.setDeepLinkUrl(deepLinkUrl);
        alert.setDedupKey(dedupKey);
        alert.setExpiresAt(LocalDateTime.now().plusDays(30));
        if (resendDelaySeconds > 0) {
            alert.setResendCutoffAt(LocalDateTime.now().plusSeconds(resendDelaySeconds));
        }
        alertRepository.save(alert);
    }

    private String buildDedupKey(String topicKey, String entityType, Long entityId, Long userId, int resendDelaySecs) {
        if (topicKey == null || entityType == null || entityId == null) return null;
        // Daily window for dedup
        String window = resendDelaySecs > 0 ? java.time.LocalDate.now().toString() : "";
        return topicKey + ":" + entityType + ":" + entityId + ":" + userId + ":" + window;
    }

    static String buildDeepLink(String entityType, Long entityId) {
        if (entityType == null || entityId == null) return null;
        return switch (entityType) {
            case "LEAD" -> "/leads/" + entityId;
            case "OPPORTUNITY" -> "/opportunities/" + entityId;
            case "ACCOUNT" -> "/accounts/" + entityId;
            case "ACTIVITY" -> "/activities/" + entityId;
            case "CONTRACT" -> "/contracts/" + entityId;
            case "QUOTE" -> "/quotes/" + entityId;
            case "SALES_ORDER" -> "/sales-orders/" + entityId;
            case "CONTACT" -> "/contacts/" + entityId;
            default -> null;
        };
    }

    @Transactional(readOnly = true)
    public AlertResponse findById(Long id) {
        return AlertResponse.from(alertRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Alert not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getForUser(String username, List<AlertState> states) {
        return userRepository.findByUsername(username).map(user -> {
            List<AlertState> resolved = (states == null || states.isEmpty())
                    ? List.of(AlertState.NEW, AlertState.READ)
                    : states;
            return alertRepository.findByUser_IdAndAlertStateInOrderByCreatedAtDesc(user.getId(), resolved)
                    .stream().map(AlertResponse::from).toList();
        }).orElse(List.of());
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> getPagedForUser(String username, Pageable pageable) {
        return userRepository.findByUsername(username)
                .map(user -> alertRepository.findByUser_IdOrderByCreatedAtDesc(user.getId(), pageable)
                        .map(AlertResponse::from))
                .orElse(Page.empty());
    }

    @Transactional(readOnly = true)
    public long countUnread(String username) {
        return userRepository.findByUsername(username)
                .map(u -> alertRepository.countByUser_IdAndAlertState(u.getId(), AlertState.NEW))
                .orElse(0L);
    }

    public AlertResponse markAsRead(Long id) {
        Alert alert = getOrThrow(id);
        alert.setAlertState(AlertState.READ);
        return AlertResponse.from(alertRepository.save(alert));
    }

    public AlertResponse markAsAccepted(Long id) {
        Alert alert = getOrThrow(id);
        alert.setAlertState(AlertState.ACCEPTED);
        return AlertResponse.from(alertRepository.save(alert));
    }

    public AlertResponse markAsNew(Long id) {
        Alert alert = getOrThrow(id);
        alert.setAlertState(AlertState.NEW);
        return AlertResponse.from(alertRepository.save(alert));
    }

    public void markAllRead(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            List<Alert> news = alertRepository.findByUser_IdAndAlertStateInOrderByCreatedAtDesc(
                    user.getId(), List.of(AlertState.NEW));
            news.forEach(a -> a.setAlertState(AlertState.READ));
            alertRepository.saveAll(news);
        });
    }

    public void delete(Long id) {
        alertRepository.delete(getOrThrow(id));
    }

    /**
     * Mirrors OpenCRX UserHomes.refreshItems():
     * READ alerts older than 3 months → EXPIRED.
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void expireStaleAlerts() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<Alert> stale = alertRepository.findByAlertStateAndCreatedAtBefore(AlertState.READ, threeMonthsAgo);
        stale.forEach(a -> a.setAlertState(AlertState.EXPIRED));
        if (!stale.isEmpty()) alertRepository.saveAll(stale);
    }

    private Alert getOrThrow(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Alert not found: " + id));
    }
}
