package com.crm.domain.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Per-topic notification configuration (global when workspaceId is null).
 * Controls thresholds, channel toggles, and message templates per topic.
 */
@Entity
@Table(name = "notification_configs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"workspaceId", "topicKey"}))
@EntityListeners(AuditingEntityListener.class)
public class NotificationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topicKey;         // matches Topic.topicKey

    private Long workspaceId;        // null = global default

    private boolean enabled = true;

    // Thresholds (used by trigger service)
    private Integer stagnantDays;           // L-03, O-05
    private BigDecimal highValueThreshold;  // L-07, O-06
    private Integer expiryWarningDays;      // A-02
    private Integer dormancyDays;           // A-01

    // Channel toggles
    private boolean inAppEnabled = true;
    private boolean emailEnabled = false;   // off by default, enable in prod with mail config

    // Message templates ({{token}} syntax)
    @Column(columnDefinition = "TEXT")
    private String titleTemplate;

    @Column(columnDefinition = "TEXT")
    private String bodyTemplate;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTopicKey() { return topicKey; }
    public void setTopicKey(String topicKey) { this.topicKey = topicKey; }
    public Long getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(Long workspaceId) { this.workspaceId = workspaceId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Integer getStagnantDays() { return stagnantDays; }
    public void setStagnantDays(Integer stagnantDays) { this.stagnantDays = stagnantDays; }
    public BigDecimal getHighValueThreshold() { return highValueThreshold; }
    public void setHighValueThreshold(BigDecimal highValueThreshold) { this.highValueThreshold = highValueThreshold; }
    public Integer getExpiryWarningDays() { return expiryWarningDays; }
    public void setExpiryWarningDays(Integer expiryWarningDays) { this.expiryWarningDays = expiryWarningDays; }
    public Integer getDormancyDays() { return dormancyDays; }
    public void setDormancyDays(Integer dormancyDays) { this.dormancyDays = dormancyDays; }
    public boolean isInAppEnabled() { return inAppEnabled; }
    public void setInAppEnabled(boolean inAppEnabled) { this.inAppEnabled = inAppEnabled; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    public String getTitleTemplate() { return titleTemplate; }
    public void setTitleTemplate(String titleTemplate) { this.titleTemplate = titleTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
