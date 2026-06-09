package com.crm.timetracking.entity;

import com.crm.timetracking.enums.AttendanceApprovalStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "attendance",
    indexes = {
        @Index(name = "idx_attendance_user_start", columnList = "user_id, start_time"),
        @Index(name = "idx_attendance_start_time", columnList = "start_time")
    }
)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startTime;

    @Column(name = "end_time", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime endTime;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "source", nullable = false, length = 32)
    private String source = "MANUAL";

    // null = normal punch session (no approval needed).
    // PENDING/APPROVED/REJECTED apply to manually-entered corrections.
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    private AttendanceApprovalStatus approvalStatus;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    protected Attendance() {}

    public Attendance(Long userId, OffsetDateTime startTime, String source) {
        this.userId = userId;
        this.startTime = startTime;
        this.source = source != null ? source : "MANUAL";
    }

    public Long getId()                                      { return id; }
    public Long getUserId()                                  { return userId; }
    public OffsetDateTime getStartTime()                     { return startTime; }
    public OffsetDateTime getEndTime()                       { return endTime; }
    public Long getDurationSeconds()                         { return durationSeconds; }
    public String getNote()                                  { return note; }
    public String getSource()                                { return source; }
    public AttendanceApprovalStatus getApprovalStatus()      { return approvalStatus; }
    public Long getApprovedBy()                              { return approvedBy; }
    public OffsetDateTime getApprovedAt()                    { return approvedAt; }
    public String getRejectionReason()                       { return rejectionReason; }
    public OffsetDateTime getCreatedAt()                     { return createdAt; }
    public OffsetDateTime getUpdatedAt()                     { return updatedAt; }

    public void setStartTime(OffsetDateTime startTime)             { this.startTime = startTime; }
    public void setEndTime(OffsetDateTime endTime)                 { this.endTime = endTime; }
    public void setDurationSeconds(Long durationSeconds)           { this.durationSeconds = durationSeconds; }
    public void setNote(String note)                               { this.note = note; }
    public void setSource(String source)                           { this.source = source; }
    public void setApprovalStatus(AttendanceApprovalStatus s)      { this.approvalStatus = s; }
    public void setApprovedBy(Long approvedBy)                     { this.approvedBy = approvedBy; }
    public void setApprovedAt(OffsetDateTime approvedAt)           { this.approvedAt = approvedAt; }
    public void setRejectionReason(String rejectionReason)         { this.rejectionReason = rejectionReason; }
}
