package com.crm.timetracking.entity;

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

    public Long getId()                        { return id; }
    public Long getUserId()                    { return userId; }
    public OffsetDateTime getStartTime()       { return startTime; }
    public OffsetDateTime getEndTime()         { return endTime; }
    public Long getDurationSeconds()           { return durationSeconds; }
    public String getNote()                    { return note; }
    public String getSource()                  { return source; }
    public OffsetDateTime getCreatedAt()       { return createdAt; }
    public OffsetDateTime getUpdatedAt()       { return updatedAt; }

    public void setStartTime(OffsetDateTime startTime)       { this.startTime = startTime; }
    public void setEndTime(OffsetDateTime endTime)           { this.endTime = endTime; }
    public void setDurationSeconds(Long durationSeconds)     { this.durationSeconds = durationSeconds; }
    public void setNote(String note)                         { this.note = note; }
    public void setSource(String source)                     { this.source = source; }
}
