package com.crm.timetracking.controller;

import com.crm.domain.entity.User;
import com.crm.repository.UserRepository;
import com.crm.timetracking.dto.*;
import com.crm.timetracking.repository.AttendanceReportRepository;
import com.crm.timetracking.service.AttendanceReportService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance-reports")
public class AttendanceReportController {

    private final AttendanceReportService    reportService;
    private final AttendanceReportRepository reportRepo;
    private final UserRepository             userRepository;

    public AttendanceReportController(AttendanceReportService reportService,
                                      AttendanceReportRepository reportRepo,
                                      UserRepository userRepository) {
        this.reportService  = reportService;
        this.reportRepo     = reportRepo;
        this.userRepository = userRepository;
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

        if (!targetUserId.equals(callerUserId) && !isAdmin(userDetails)) {
            throw new AccessDeniedException("Non-admin users may only create reports for themselves.");
        }

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

    /** Edit an existing report. */
    @PutMapping("/{id}")
    public ResponseEntity<AttendanceReportResponse> edit(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceReportRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        guardReportOwnerOrAdmin(id, userDetails);
        return ResponseEntity.ok(reportService.editReport(id, req));
    }

    /** Delete an existing report. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        guardReportOwnerOrAdmin(id, userDetails);
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    // ── SECURITY HELPERS ─────────────────────────────────────────────────────

    private void guardSelfOrAdmin(Long targetUserId, UserDetails caller) {
        if (!isAdmin(caller) && !resolveUserId(caller.getUsername()).equals(targetUserId)) {
            throw new AccessDeniedException("Access denied.");
        }
    }

    /** Throws 403 if the caller does not own the report and is not admin. */
    private void guardReportOwnerOrAdmin(Long reportId, UserDetails caller) {
        if (isAdmin(caller)) return;
        Long callerUserId  = resolveUserId(caller.getUsername());
        Long reportOwnerId = reportRepo.findById(reportId)
                .map(r -> r.getUserId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "AttendanceReport " + reportId + " not found"));
        if (!reportOwnerId.equals(callerUserId)) {
            throw new AccessDeniedException("You do not have permission to modify this report.");
        }
    }

    private boolean isAdmin(UserDetails caller) {
        return caller.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Long resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }
}