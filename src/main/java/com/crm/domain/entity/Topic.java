package com.crm.domain.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "topics")
@EntityListeners(AuditingEntityListener.class)
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FSD topic code, e.g. "L-01", "O-07", "A-02"
    @Column(unique = true)
    private String topicKey;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String entityType;

    private boolean sendAlertEnabled = true;
    private boolean sendMailEnabled = false;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTopicKey() { return topicKey; }
    public void setTopicKey(String topicKey) { this.topicKey = topicKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public boolean isSendAlertEnabled() { return sendAlertEnabled; }
    public void setSendAlertEnabled(boolean sendAlertEnabled) { this.sendAlertEnabled = sendAlertEnabled; }
    public boolean isSendMailEnabled() { return sendMailEnabled; }
    public void setSendMailEnabled(boolean sendMailEnabled) { this.sendMailEnabled = sendMailEnabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
