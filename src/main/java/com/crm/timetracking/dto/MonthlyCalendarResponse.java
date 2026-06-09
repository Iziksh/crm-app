package com.crm.timetracking.dto;

import java.util.List;

public record MonthlyCalendarResponse(
        Long                   userId,
        String                 username,
        int                    year,
        int                    month,
        List<DayCalendarEntry> days,
        int                    totalWorkedMinutes,
        int                    totalStandardMinutes,
        int                    totalDeltaMinutes
) {}