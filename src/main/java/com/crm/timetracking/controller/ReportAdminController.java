package com.crm.timetracking.controller;

import com.crm.timetracking.scheduler.MonthlyReportJob;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportAdminController {

    private final MonthlyReportJob reportJob;

    public ReportAdminController(MonthlyReportJob reportJob) {
        this.reportJob = reportJob;
    }

    @PostMapping("/monthly/trigger")
    public ResponseEntity<String> triggerManually() {
        reportJob.generateAndSendMonthlyReport();
        return ResponseEntity.ok("Monthly report job triggered.");
    }
}
