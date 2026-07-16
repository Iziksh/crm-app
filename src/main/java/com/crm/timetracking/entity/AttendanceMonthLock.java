package com.crm.timetracking.entity;

import com.crm.timetracking.enums.AttendanceMonthLockStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "attendance_month_lock",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "year", "month"})
)
public class AttendanceMonthLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "\"year\"", nullable = false)
    private int year;

    @Column(name = "\"month\"", nullable = false)
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceMonthLockStatus status = AttendanceMonthLockStatus.OPEN;

    @Column(name = "submitted_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime submittedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    protected AttendanceMonthLock() {}

    public AttendanceMonthLock(Long userId, int year, int month) {
        this.userId = userId;
        this.year = year;
        this.month = month;
    }

    public Long getId()                                    { return id; }
    public Long getUserId()                                { return userId; }
    public int getYear()                                   { return year; }
    public int getMonth()                                  { return month; }
    public AttendanceMonthLockStatus getStatus()           { return status; }
    public OffsetDateTime getSubmittedAt()                 { return submittedAt; }
    public Long getApprovedBy()                            { return approvedBy; }
    public OffsetDateTime getApprovedAt()                  { return approvedAt; }
    public String getRejectionReason()                     { return rejectionReason; }

    public void setStatus(AttendanceMonthLockStatus status) { this.status = status; }
    public void setSubmittedAt(OffsetDateTime submittedAt)  { this.submittedAt = submittedAt; }
    public void setApprovedBy(Long approvedBy)              { this.approvedBy = approvedBy; }
    public void setApprovedAt(OffsetDateTime approvedAt)    { this.approvedAt = approvedAt; }
    public void setRejectionReason(String rejectionReason)  { this.rejectionReason = rejectionReason; }
}
