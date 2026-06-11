package com.crm.config.performance;

import com.crm.service.DashboardStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Simulates DashboardView DB load at startup for profiling. Disable after investigation.
 */
@Component
@Order(100)
@ConditionalOnProperty(name = "app.performance.probe-dashboard", havingValue = "true")
public class DashboardLoadProbe implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DashboardLoadProbe.class);

    private final DashboardStatsService dashboardStatsService;

    public DashboardLoadProbe(DashboardStatsService dashboardStatsService) {
        this.dashboardStatsService = dashboardStatsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("[PERF] Probing dashboard DB load via DashboardStatsService...");
        StartupPerformanceProfiler.time("phase.dashboard-load-total",
                () -> dashboardStatsService.loadStats());
        StartupPerformanceProfiler.logSummary("DASHBOARD DB PROBE");
    }
}
