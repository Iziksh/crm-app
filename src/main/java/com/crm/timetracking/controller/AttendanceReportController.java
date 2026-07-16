package com.crm.timetracking.controller;

import com.crm.domain.entity.User;
import com.crm.repository.UserRepository;
import com.crm.service.UserService;
import com.crm.timetracking.dto.*;
import com.crm.timetracking.entity.AttendanceReport;
import com.crm.timetracking.exception.AttendanceValidationException;
import com.crm.timetracking.repository.AttendanceReportRepository;
import com.crm.timetracking.service.AttendanceMonthLockService;
import com.crm.timetracking.service.AttendanceReportService;
import com.crm.timetracking.util.DurationCalculator;
import com.crm.util.CsvExporter;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/attendance-reports")
public class AttendanceReportController {

    private final AttendanceReportService    reportService;
    private final AttendanceReportRepository reportRepo;
    private final UserRepository             userRepository;
    private final UserService                userService;
    private final AttendanceMonthLockService lockService;

    public AttendanceReportController(AttendanceReportService reportService,
                                      AttendanceReportRepository reportRepo,
                                      UserRepository userRepository,
                                      UserService userService,
                                      AttendanceMonthLockService lockService) {
        this.reportService  = reportService;
        this.reportRepo     = reportRepo;
        this.userRepository = userRepository;
        this.userService    = userService;
        this.lockService    = lockService;
    }

    /**
     * Create a manual attendance report.
     * Non-admin users can only create reports for themselves.
     * Admins may pass ?userId= to create on behalf of another user.
     */
    @PostMapping
    public ResponseEntity<AttendanceReportResponse> create(
            @Valid @RequestBody AttendanceReportRequest req,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long callerUserId = resolveUserId(userDetails.getUsername());
        Long targetUserId = (userId == null) ? callerUserId : userId;

        if (!targetUserId.equals(callerUserId) && !isAdmin(userDetails)
                && !userService.isManagerOf(callerUserId, targetUserId)) {
            throw new AccessDeniedException("You may only create reports for yourself or your direct reports.");
        }
        guardMonthNotLocked(targetUserId, req.reportDate().getYear(), req.reportDate().getMonthValue(), userDetails);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.createReport(targetUserId, req));
    }

    /** List all reports for a user on a specific day. */
    @GetMapping
    public ResponseEntity<List<AttendanceReportResponse>> getForDay(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {

        guardSelfOrAdmin(userId, userDetails);
        return ResponseEntity.ok(reportService.getReportsForDay(userId, date));
    }

    /** Monthly calendar — all days enriched with totals, holidays, weekend flags. */
    @GetMapping("/calendar")
    public ResponseEntity<MonthlyCalendarResponse> getCalendar(
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails) {

        guardSelfOrAdmin(userId, userDetails);
        return ResponseEntity.ok(reportService.getMonthlyCalendar(userId, year, month));
    }

    /** Monthly Summary dashboard rollup — days/hours/percentage, same access rules as the calendar. */
    @GetMapping("/summary")
    public ResponseEntity<MonthlySummaryResponse> getSummary(
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails) {

        guardSelfOrAdmin(userId, userDetails);
        return ResponseEntity.ok(reportService.getMonthlySummary(userId, year, month));
    }

    /** Full daily detail export — one row per calendar day, exact times, day type, work type, overtime split. */
    @GetMapping("/export/daily")
    public ResponseEntity<InputStreamResource> exportDaily(
            @RequestParam Long userId, @RequestParam int year, @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails) {
        guardSelfOrAdmin(userId, userDetails);
        MonthlyCalendarResponse cal = reportService.getMonthlyCalendar(userId, year, month);

        String[] headers = {"date", "day_of_week", "type", "entry_time", "exit_time", "work_type", "project_tag",
                "worked", "standard", "regular", "ot_125", "ot_150", "delta", "note"};
        List<String[]> rows = cal.days().stream().map(d -> new String[]{
                d.date().toString(),
                d.date().getDayOfWeek().toString(),
                d.reports().stream().map(r -> r.reportType().toString()).distinct().collect(Collectors.joining("; ")),
                joinNonBlank(d.reports().stream().map(r -> r.entryTime() != null ? r.entryTime().toString() : "")),
                joinNonBlank(d.reports().stream().map(r -> r.exitTime() != null ? r.exitTime().toString() : "")),
                joinNonBlank(d.reports().stream().map(r -> r.workType() != null ? r.workType().toString() : "")),
                joinNonBlank(d.reports().stream().map(r -> r.projectTag() != null ? r.projectTag() : "")),
                DurationCalculator.formatMinutes(d.totalWorkedMinutes()),
                DurationCalculator.formatMinutes(d.standardMinutes()),
                DurationCalculator.formatMinutes(d.regularMinutes()),
                DurationCalculator.formatMinutes(d.overtime125Minutes()),
                DurationCalculator.formatMinutes(d.overtime150Minutes()),
                (d.deltaMinutes() >= 0 ? "+" : "-") + DurationCalculator.formatMinutes(Math.abs(d.deltaMinutes())),
                joinNonBlank(d.reports().stream().map(r -> r.note() != null ? r.note() : ""))
        }).toList();

        return csv("timesheet-" + cal.username() + "-" + year + "-" + month + ".csv", headers, rows);
    }

    /** Consolidated monthly summary export — single-row, payroll-entry-ready hour split. */
    @GetMapping("/export/monthly-summary")
    public ResponseEntity<InputStreamResource> exportMonthlySummary(
            @RequestParam Long userId, @RequestParam int year, @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails) {
        guardSelfOrAdmin(userId, userDetails);
        MonthlySummaryResponse s = reportService.getMonthlySummary(userId, year, month);

        String[] headers = {"employee", "year", "month", "standard_days", "actual_days", "worked", "standard",
                "delta", "regular", "ot_125", "ot_150", "attendance_percent"};
        String[] row = {
                s.username(), String.valueOf(s.year()), String.valueOf(s.month()),
                String.valueOf(s.standardDays()), String.valueOf(s.actualDays()),
                DurationCalculator.formatMinutes(s.totalWorkedMinutes()),
                DurationCalculator.formatMinutes(s.totalStandardMinutes()),
                (s.totalDeltaMinutes() >= 0 ? "+" : "-") + DurationCalculator.formatMinutes(Math.abs(s.totalDeltaMinutes())),
                DurationCalculator.formatMinutes(s.totalRegularMinutes()),
                DurationCalculator.formatMinutes(s.totalOvertime125Minutes()),
                DurationCalculator.formatMinutes(s.totalOvertime150Minutes()),
                s.attendancePercent() != null ? s.attendancePercent() + "%" : ""
        };
        return csv("monthly-summary-" + s.username() + "-" + year + "-" + month + ".csv", headers, List.<String[]>of(row));
    }

    /** Monthly absence report export — one row per non-PRESENCE leave report. */
    @GetMapping("/export/absences")
    public ResponseEntity<InputStreamResource> exportAbsences(
            @RequestParam Long userId, @RequestParam int year, @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails) {
        guardSelfOrAdmin(userId, userDetails);
        MonthlyCalendarResponse cal = reportService.getMonthlyCalendar(userId, year, month);

        String[] headers = {"date", "type", "duration", "note"};
        List<String[]> rows = cal.days().stream()
                .flatMap(d -> d.reports().stream())
                .filter(r -> r.reportType() != com.crm.timetracking.enums.AttendanceReportType.PRESENCE)
                .map(r -> new String[]{
                        r.reportDate().toString(),
                        r.reportTypeLabel(),
                        r.durationFormatted() != null ? r.durationFormatted() : "Full day",
                        CsvExporter.str(r.note())
                }).toList();

        return csv("absences-" + cal.username() + "-" + year + "-" + month + ".csv", headers, rows);
    }

    private static String joinNonBlank(java.util.stream.Stream<String> values) {
        return values.filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.joining("; "));
    }

    private static ResponseEntity<InputStreamResource> csv(String filename, String[] headers, List<String[]> rows) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(CsvExporter.build(headers, rows)));
    }

    /** Edit an existing report. */
    @PutMapping("/{id}")
    public ResponseEntity<AttendanceReportResponse> edit(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceReportRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        AttendanceReport existing = guardReportOwnerOrAdmin(id, userDetails);
        guardMonthNotLocked(existing.getUserId(), existing.getReportDate().getYear(),
                existing.getReportDate().getMonthValue(), userDetails);
        guardMonthNotLocked(existing.getUserId(), req.reportDate().getYear(),
                req.reportDate().getMonthValue(), userDetails);
        return ResponseEntity.ok(reportService.editReport(id, req));
    }

    /** Delete an existing report. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        AttendanceReport existing = guardReportOwnerOrAdmin(id, userDetails);
        guardMonthNotLocked(existing.getUserId(), existing.getReportDate().getYear(),
                existing.getReportDate().getMonthValue(), userDetails);
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    // ── SECURITY HELPERS ─────────────────────────────────────────────────────

    private void guardSelfOrAdmin(Long targetUserId, UserDetails caller) {
        Long callerId = resolveUserId(caller.getUsername());
        if (callerId.equals(targetUserId)) return;
        if (isAdmin(caller)) return;
        if (userService.isManagerOf(callerId, targetUserId)) return;
        throw new AccessDeniedException("Access denied.");
    }

    /** Throws 403 if the caller does not own the report, manage its owner, or is admin. Returns the report. */
    private AttendanceReport guardReportOwnerOrAdmin(Long reportId, UserDetails caller) {
        AttendanceReport report = reportRepo.findById(reportId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "AttendanceReport " + reportId + " not found"));
        if (isAdmin(caller)) return report;
        Long callerUserId = resolveUserId(caller.getUsername());
        Long reportOwnerId = report.getUserId();
        if (!reportOwnerId.equals(callerUserId) && !userService.isManagerOf(callerUserId, reportOwnerId)) {
            throw new AccessDeniedException("You do not have permission to modify this report.");
        }
        return report;
    }

    /** Throws 400 if the target month is SUBMITTED/APPROVED and the caller isn't an admin. */
    private void guardMonthNotLocked(Long targetUserId, int year, int month, UserDetails caller) {
        if (isAdmin(caller)) return;
        if (lockService.isLocked(targetUserId, year, month)) {
            throw new AttendanceValidationException(
                    "This month has been locked for approval and can no longer be edited.");
        }
    }

    // See AttendanceController.isAdmin — ROLE_HR_MANAGER grants company-wide attendance access
    // without the broader ROLE_ADMIN powers elsewhere; deliberately not part of the RoleHierarchy.
    private boolean isAdmin(UserDetails caller) {
        return caller.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR_MANAGER"));
    }

    private Long resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }
}