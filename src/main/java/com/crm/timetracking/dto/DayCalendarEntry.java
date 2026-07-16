package com.crm.timetracking.dto;

import java.time.LocalDate;
import java.util.List;

public record DayCalendarEntry(
        LocalDate                      date,
        List<AttendanceReportResponse> reports,
        int                            totalWorkedMinutes,
        int                            standardMinutes,
        boolean                        isWeekend,
        boolean                        isHoliday,
        String                         holidayName,     // null if not a holiday
        int                            deltaMinutes,    // totalWorkedMinutes - standardMinutes
        int                            regularMinutes,      // min(totalWorkedMinutes, standardMinutes)
        int                            overtime125Minutes,  // first 120 min beyond standard
        int                            overtime150Minutes   // beyond 120 min over standard
) {}