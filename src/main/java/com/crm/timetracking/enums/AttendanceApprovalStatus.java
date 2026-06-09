package com.crm.timetracking.enums;

public enum AttendanceApprovalStatus {
    /** Employee submitted a missed-clock-in correction; awaiting manager review. */
    PENDING,
    /** Manager confirmed the manual entry is accurate. */
    APPROVED,
    /** Manager rejected the manual entry (incorrect times or not authorised). */
    REJECTED
}