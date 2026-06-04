package com.crm.domain.entity;

import com.crm.domain.enums.AlertImportance;
import com.crm.domain.enums.AlertState;
import com.crm.domain.enums.NotificationChannel;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alerts_user_state", columnList = "user_id, alertState"),
    @Index(name = "idx_alerts_dedup", columnList = "dedupKey", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertState alertState = AlertState.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertImportance importance = AlertImportance.NORMAL;

    // FSD fields
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel = NotificationChannel.IN_APP;

    private String topicKey;        // e.g. "O-07", "L-03"
    private String entityType;
    private Long entityId;

    @Column(length = 1000)
    private String deepLinkUrl;     // e.g. /opportunities/42

    @Column(unique = true, length = 500)
    private String dedupKey;        // topicKey:entityType:entityId:userId:windowDate

    // Deduplication: suppress new alert while resendCutoffAt > now()
    private LocalDateTime resendCutoffAt;

    private LocalDateTime readAt;
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AlertState getAlertState() { return alertState; }
    public void setAlertState(AlertState alertState) { this.alertState = alertState; }
    public AlertImportance getImportance() { return importance; }
    public void setImportance(AlertImportance importance) { this.importance = importance; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public String getTopicKey() { return topicKey; }
    public void setTopicKey(String topicKey) { this.topicKey = topicKey; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getDeepLinkUrl() { return deepLinkUrl; }
    public void setDeepLinkUrl(String deepLinkUrl) { this.deepLinkUrl = deepLinkUrl; }
    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }
    public LocalDateTime getResendCutoffAt() { return resendCutoffAt; }
    public void setResendCutoffAt(LocalDateTime resendCutoffAt) { this.resendCutoffAt = resendCutoffAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
