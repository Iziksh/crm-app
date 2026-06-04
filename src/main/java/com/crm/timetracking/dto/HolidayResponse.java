package com.crm.timetracking.dto;

import com.crm.timetracking.entity.Holiday;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HolidayResponse(
        Long id,
        LocalDate date,
        String name,
        String type,
        String country,
        Short year,
        BigDecimal creditHours
) {
    public static HolidayResponse from(Holiday h) {
        return new HolidayResponse(
                h.getId(), h.getDate(), h.getName(), h.getType(),
                h.getCountry(), h.getYear(), h.getCreditHours());
    }
}
