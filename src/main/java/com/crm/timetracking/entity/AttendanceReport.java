package com.crm.timetracking.entity;

import com.crm.timetracking.enums.AttendanceReportType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "attendance_report",
    indexes = {
        @Index(name = "idx_ar_user_date",      columnList = "user_id, report_date"),
        @Index(name = "idx_ar_user_date_type", columnList = "user_id, report_date, report_type")
    }
)
public class AttendanceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain Long — mirrors Attendance.userId (Phase 22): keeps timetracking decoupled from User entity.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "entry_time")
    private LocalTime entryTime;        // null for absence-style types

    @Column(name = "exit_time")
    private LocalTime exitTime;         // null for absence-style types

    // Computed on every save; null when no clock times are present.
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 32)
    private AttendanceReportType reportType = AttendanceReportType.PRESENCE;

    @Column(name = "equate_to_standard", nullable = false)
    private boolean equateToStandard = false;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = OffsetDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public AttendanceReport() {}

    public Long getId()                        { return id; }
    public Long getUserId()                    { return userId; }
    public LocalDate getReportDate()           { return reportDate; }
    public LocalTime getEntryTime()            { return entryTime; }
    public LocalTime getExitTime()             { return exitTime; }
    public Integer getDurationMinutes()        { return durationMinutes; }
    public String getNote()                    { return note; }
    public AttendanceReportType getReportType(){ return reportType; }
    public boolean isEquateToStandard()        { return equateToStandard; }
    public OffsetDateTime getCreatedAt()       { return createdAt; }
    public OffsetDateTime getUpdatedAt()       { return updatedAt; }

    public void setUserId(Long userId)                         { this.userId = userId; }
    public void setReportDate(LocalDate reportDate)            { this.reportDate = reportDate; }
    public void setEntryTime(LocalTime entryTime)              { this.entryTime = entryTime; }
    public void setExitTime(LocalTime exitTime)                { this.exitTime = exitTime; }
    public void setDurationMinutes(Integer durationMinutes)    { this.durationMinutes = durationMinutes; }
    public void setNote(String note)                           { this.note = note; }
    public void setReportType(AttendanceReportType reportType) { this.reportType = reportType; }
    public void setEquateToStandard(boolean equateToStandard)  { this.equateToStandard = equateToStandard; }
}