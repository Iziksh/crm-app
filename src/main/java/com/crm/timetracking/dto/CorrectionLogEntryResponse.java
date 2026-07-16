package com.crm.timetracking.dto;

import com.crm.timetracking.entity.Attendance;
import com.crm.timetracking.enums.AttendanceApprovalStatus;

import java.time.OffsetDateTime;

/** Unified correction-log row — an Attendance correction enriched with resolved names for display. */
public record CorrectionLogEntryResponse(
        Long                     id,
        Long                     userId,
        String                   employeeName,
        OffsetDateTime           startTime,
        OffsetDateTime           endTime,
        Long                     durationSeconds,
        String                   note,
        String                   source,
        AttendanceApprovalStatus approvalStatus,
        Long                     approvedBy,
        String                   approverName,
        OffsetDateTime           approvedAt,
        String                   rejectionReason,
        OffsetDateTime           createdAt
) {
    public static CorrectionLogEntryResponse from(Attendance a, String employeeName, String approverName) {
        return new CorrectionLogEntryResponse(
                a.getId(), a.getUserId(), employeeName, a.getStartTime(), a.getEndTime(),
                a.getDurationSeconds(), a.getNote(), a.getSource(), a.getApprovalStatus(),
                a.getApprovedBy(), approverName, a.getApprovedAt(), a.getRejectionReason(), a.getCreatedAt());
    }
}
