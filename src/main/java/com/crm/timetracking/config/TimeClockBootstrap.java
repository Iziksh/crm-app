package com.crm.timetracking.config;

import com.crm.config.performance.StartupPerformanceProfiler;
import com.crm.timetracking.repository.HolidayRepository;
import com.crm.timetracking.service.IsraeliHolidayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class TimeClockBootstrap {

    private static final Logger log = LoggerFactory.getLogger(TimeClockBootstrap.class);

    @Bean
    ApplicationRunner bootstrapHolidays(IsraeliHolidayService svc, HolidayRepository repo) {
        return args -> StartupPerformanceProfiler.time("phase.time-clock-bootstrap", () -> {
            try {
                int year = LocalDate.now().getYear();
                if (repo.findByYearAndCountry((short) year, "IL").isEmpty()) {
                    log.info("Bootstrapping Israeli holidays for {} and {}", year, year + 1);
                    StartupPerformanceProfiler.time("phase.holiday-generation",
                            () -> {
                                svc.generateHolidaysForYear(year);
                                svc.generateHolidaysForYear(year + 1);
                            });
                    log.info("Israeli holiday bootstrap complete.");
                }
            } catch (Exception e) {
                log.error("Holiday bootstrap failed — app will continue without holidays: {}", e.getMessage());
            }
        });
    }
}
