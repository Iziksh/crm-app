package com.crm.timetracking.dto;

import com.crm.timetracking.entity.Attendance;
import com.crm.timetracking.enums.AttendanceApprovalStatus;

import java.time.OffsetDateTime;

public record AttendanceResponse(
        Long                    id,
        Long                    userId,
        OffsetDateTime          startTime,
        OffsetDateTime          endTime,
        Long                    durationSeconds,
        String                  note,
        String                  source,
        AttendanceApprovalStatus approvalStatus,   // null = normal punch
        Long                    approvedBy,
        OffsetDateTime          approvedAt,
        String                  rejectionReason,
        OffsetDateTime          createdAt
) {
    public static AttendanceResponse from(Attendance a) {
        return new AttendanceResponse(
                a.getId(), a.getUserId(), a.getStartTime(), a.getEndTime(),
                a.getDurationSeconds(), a.getNote(), a.getSource(),
                a.getApprovalStatus(), a.getApprovedBy(), a.getApprovedAt(),
                a.getRejectionReason(), a.getCreatedAt());
    }
}