package com.crm.timetracking.enums;

public enum AttendanceMonthLockStatus {
    /** Default state — the employee's timesheet for this month is still editable. */
    OPEN,
    /** Employee has locked the month; awaiting manager review. Not editable by the employee. */
    SUBMITTED,
    /** Manager approved the month. Locked — only an admin override can reopen it. */
    APPROVED,
    /** Manager sent the month back for corrections. Editable again by the employee. */
    REJECTED
}
