package com.crm.domain.entity;

import com.crm.domain.enums.AlertImportance;
import com.crm.domain.enums.TaskStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_tasks", indexes = {
    @Index(name = "idx_tasks_scheduled", columnList = "scheduledAt, status"),
    @Index(name = "idx_tasks_entity", columnList = "targetEntityType, targetEntityId, status")
})
@EntityListeners(AuditingEntityListener.class)
public class ScheduledTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Classification
    @Column(nullable = false)
    private String workflowKey;      // FSD code: "O-07", "L-03", "A-01"

    @Column(nullable = false)
    private String workflowName;     // Human-readable: "Close Date Approaching"

    // Target
    @Column(nullable = false)
    private String targetEntityType; // "OPPORTUNITY", "LEAD", "CONTRACT", "ACTIVITY"

    @Column(nullable = false)
    private Long targetEntityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    // Scheduling
    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private AlertImportance priority = AlertImportance.NORMAL;

    private int attemptCount = 0;
    private int maxAttempts = 3;
    private LocalDateTime lastAttemptedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    // Re-evaluation cancel conditions
    private String cancelIfField;    // entity field name to check
    private String cancelIfValue;    // comma-separated: "WON,LOST" or single value "PAID"

    // Admin
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspended_by_id")
    private User suspendedBy;

    @Column(columnDefinition = "TEXT")
    private String suspendedReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    // Getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWorkflowKey() { return workflowKey; }
    public void setWorkflowKey(String workflowKey) { this.workflowKey = workflowKey; }
    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    public String getTargetEntityType() { return targetEntityType; }
    public void setTargetEntityType(String targetEntityType) { this.targetEntityType = targetEntityType; }
    public Long getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(Long targetEntityId) { this.targetEntityId = targetEntityId; }
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public AlertImportance getPriority() { return priority; }
    public void setPriority(AlertImportance priority) { this.priority = priority; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public LocalDateTime getLastAttemptedAt() { return lastAttemptedAt; }
    public void setLastAttemptedAt(LocalDateTime lastAttemptedAt) { this.lastAttemptedAt = lastAttemptedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getCancelIfField() { return cancelIfField; }
    public void setCancelIfField(String cancelIfField) { this.cancelIfField = cancelIfField; }
    public String getCancelIfValue() { return cancelIfValue; }
    public void setCancelIfValue(String cancelIfValue) { this.cancelIfValue = cancelIfValue; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public User getSuspendedBy() { return suspendedBy; }
    public void setSuspendedBy(User suspendedBy) { this.suspendedBy = suspendedBy; }
    public String getSuspendedReason() { return suspendedReason; }
    public void setSuspendedReason(String suspendedReason) { this.suspendedReason = suspendedReason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
