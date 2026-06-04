package com.crm.timetracking.scheduler;

import com.crm.timetracking.service.IsraeliHolidayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Year;

@Component
public class HolidayRecalculationJob {

    private static final Logger log = LoggerFactory.getLogger(HolidayRecalculationJob.class);
    private final IsraeliHolidayService holidayService;

    public HolidayRecalculationJob(IsraeliHolidayService holidayService) {
        this.holidayService = holidayService;
    }

    // Fires at 02:00 AM on October 1 every year — pre-computes next year's holidays
    @Scheduled(cron = "0 0 2 1 10 *")
    public void recalculateUpcomingYearHolidays() {
        int nextYear = Year.now().getValue() + 1;
        log.info("Holiday recalculation job started for year {}", nextYear);
        var result = holidayService.generateHolidaysForYear(nextYear);
        log.info("Persisted {} Israeli holidays for year {}", result.size(), nextYear);
    }
}
