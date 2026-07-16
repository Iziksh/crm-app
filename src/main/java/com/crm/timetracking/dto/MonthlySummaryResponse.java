package com.crm.timetracking.dto;

public record MonthlySummaryResponse(
        Long   userId,
        String username,
        int    year,
        int    month,
        int    standardDays,
        int    actualDays,
        int    totalWorkedMinutes,
        int    totalStandardMinutes,
        int    totalDeltaMinutes,
        int    totalRegularMinutes,
        int    totalOvertime125Minutes,
        int    totalOvertime150Minutes,
        Double attendancePercent  // null when standard is 0 (e.g. no workdays yet in the month)
) {}
