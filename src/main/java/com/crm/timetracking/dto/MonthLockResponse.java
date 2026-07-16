package com.crm.timetracking.dto;

import com.crm.timetracking.entity.AttendanceMonthLock;
import com.crm.timetracking.enums.AttendanceMonthLockStatus;

import java.time.OffsetDateTime;

public record MonthLockResponse(
        Long                       userId,
        int                        year,
        int                        month,
        AttendanceMonthLockStatus  status,
        OffsetDateTime             submittedAt,
        Long                       approvedBy,
        OffsetDateTime             approvedAt,
        String                     rejectionReason
) {
    /** Default OPEN state for a month that has never been submitted (no row yet). */
    public static MonthLockResponse openDefault(Long userId, int year, int month) {
        return new MonthLockResponse(userId, year, month, AttendanceMonthLockStatus.OPEN, null, null, null, null);
    }

    public static MonthLockResponse from(AttendanceMonthLock lock) {
        return new MonthLockResponse(
                lock.getUserId(), lock.getYear(), lock.getMonth(), lock.getStatus(),
                lock.getSubmittedAt(), lock.getApprovedBy(), lock.getApprovedAt(), lock.getRejectionReason());
    }
}
