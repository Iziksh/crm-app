package com.crm.timetracking.dto;

import com.crm.timetracking.entity.AttendanceReport;
import com.crm.timetracking.enums.AttendanceReportType;
import com.crm.timetracking.util.DurationCalculator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record AttendanceReportResponse(
        Long                 id,
        Long                 userId,
        LocalDate            reportDate,
        LocalTime            entryTime,
        LocalTime            exitTime,
        Integer              durationMinutes,
        String               durationFormatted,   // "H:mm" or null
        String               note,
        AttendanceReportType reportType,
        String               reportTypeLabel,     // Hebrew label from enum
        boolean              equateToStandard,
        OffsetDateTime       createdAt
) {
    public static AttendanceReportResponse from(AttendanceReport r) {
        return new AttendanceReportResponse(
                r.getId(),
                r.getUserId(),
                r.getReportDate(),
                r.getEntryTime(),
                r.getExitTime(),
                r.getDurationMinutes(),
                r.getDurationMinutes() != null
                        ? DurationCalculator.formatMinutes(r.getDurationMinutes()) : null,
                r.getNote(),
                r.getReportType(),
                r.getReportType().getHebrewLabel(),
                r.isEquateToStandard(),
                r.getCreatedAt()
        );
    }
}