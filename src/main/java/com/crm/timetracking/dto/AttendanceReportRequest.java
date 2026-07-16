package com.crm.timetracking.dto;

import com.crm.timetracking.enums.AttendanceReportType;
import com.crm.timetracking.enums.WorkType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceReportRequest(
        @NotNull LocalDate            reportDate,
        LocalTime                     entryTime,       // null allowed for absence-style types
        LocalTime                     exitTime,        // null allowed for absence-style types
        String                        note,
        @NotNull AttendanceReportType reportType,
        boolean                       equateToStandard,
        WorkType                      workType,        // null unless reportType == PRESENCE
        String                        projectTag       // free-text project/cost-center label
) {}