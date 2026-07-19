package com.crm.timetracking.controller;

import com.crm.domain.entity.User;
import com.crm.repository.UserRepository;
import com.crm.service.UserService;
import com.crm.timetracking.dto.AttendanceResponse;
import com.crm.timetracking.dto.CorrectionLogEntryResponse;
import com.crm.timetracking.entity.Attendance;
import com.crm.timetracking.enums.AttendanceApprovalStatus;
import com.crm.timetracking.service.AttendanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserRepository    userRepository;
    private final UserService       userService;

    public AttendanceController(AttendanceService attendanceService, UserRepository userRepository,
                                UserService userService) {
        this.attendanceService = attendanceService;
        this.userRepository    = userRepository;
        this.userService       = userService;
    }

    record PunchInRequest(String note, String source, Boolean force) {}
    record EditSessionRequest(OffsetDateTime newStart, OffsetDateTime newEnd) {}
    record ManualEntryRequest(OffsetDateTime startTime, OffsetDateTime endTime, String note) {}
    record RejectRequest(String reason) {}

    @PostMapping("/punch-in")
    public ResponseEntity<AttendanceResponse> punchIn(
            @RequestBody(required = false) PunchInRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long    userId = resolveUserId(userDetails.getUsername());
        String  note   = body != null ? body.note()   : null;
        String  source = body != null ? body.source() : "MANUAL";
        boolean force  = body != null && Boolean.TRUE.equals(body.force());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AttendanceResponse.from(attendanceService.punchIn(userId, note, source, force)));
    }

    @PostMapping("/punch-out")
    public ResponseEntity<AttendanceResponse> punchOut(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails.getUsername());
        return ResponseEntity.ok(AttendanceResponse.from(attendanceService.punchOut(userId)));
    }

    /**
     * Records a clock-out with no matching clock-in. Called when the employee confirms "Clock out"
     * despite having no open session (no clock-in today, or an existing incomplete clock-out line).
     * Produces an incomplete line (no total) to be corrected later.
     */
    @PostMapping("/clock-out-line")
    public ResponseEntity<AttendanceResponse> clockOutLine(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AttendanceResponse.from(attendanceService.createClockOutLine(userId)));
    }

    @GetMapping("/active")
    public ResponseEntity<AttendanceResponse> getActive(
            @RequestParam Long userId, @AuthenticationPrincipal UserDetails userDetails) {
        guardSelfOrManagerOrAdmin(userId, userDetails);
        return attendanceService.findActiveSession(userId)
                .map(a -> ResponseEntity.ok(AttendanceResponse.from(a)))
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<AttendanceResponse>> getMonthly(
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails) {
        guardSelfOrManagerOrAdmin(userId, userDetails);
        return ResponseEntity.ok(
                attendanceService.getMonthlyRecords(userId, year, month).stream()
                        .map(AttendanceResponse::from).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR_MANAGER')")
    public ResponseEntity<AttendanceResponse> editSession(
            @PathVariable Long id,
            @RequestBody EditSessionRequest body) {
        return ResponseEntity.ok(AttendanceResponse.from(
                attendanceService.editSession(id, body.newStart(), body.newEnd())));
    }

    // ── MISSED CLOCK-IN CORRECTION ────────────────────────────────────────────

    /**
     * Employee submits a correction for a session they forgot to clock in/out for.
     * Creates a PENDING record that a manager must approve before it counts toward totals.
     */
    @PostMapping("/manual-entry")
    public ResponseEntity<AttendanceResponse> manualEntry(
            @RequestBody ManualEntryRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                AttendanceResponse.from(
                        attendanceService.createManualEntry(
                                userId, body.startTime(), body.endTime(), body.note())));
    }

    // ── MANAGER APPROVAL ──────────────────────────────────────────────────────

    /**
     * Pending corrections awaiting approval. Full admins see everyone; everyone else sees only
     * the pending corrections of their own direct reports (empty list if they manage no one).
     */
    @GetMapping("/pending-approvals")
    public ResponseEntity<List<AttendanceResponse>> pendingApprovals(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long callerId = resolveUserId(userDetails.getUsername());
        List<Attendance> pending = isAdmin(userDetails)
                ? attendanceService.getPendingApprovals()
                : attendanceService.getPendingApprovalsForUsers(userService.directReportIds(callerId));
        return ResponseEntity.ok(
                pending.stream().map(AttendanceResponse::from).collect(Collectors.toList()));
    }

    /**
     * Unified correction log — full history (any status), optional date-range and status filter.
     * Employees see their own; managers see their team's (plus their own); admins see everyone's
     * unless {@code userId} narrows it to one person (self/manager/admin guarded either way).
     */
    @GetMapping("/corrections")
    public ResponseEntity<List<CorrectionLogEntryResponse>> searchCorrections(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) AttendanceApprovalStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long callerId = resolveUserId(userDetails.getUsername());
        ZoneId zone = ZoneId.of("Asia/Jerusalem");
        OffsetDateTime fromDt = from != null ? from.atStartOfDay(zone).toOffsetDateTime() : null;
        OffsetDateTime toDt   = to != null ? to.plusDays(1).atStartOfDay(zone).toOffsetDateTime() : null;

        List<Long> scopeUserIds;
        if (userId != null) {
            guardSelfOrManagerOrAdmin(userId, userDetails);
            scopeUserIds = List.of(userId);
        } else if (isAdmin(userDetails)) {
            scopeUserIds = null;
        } else {
            scopeUserIds = new ArrayList<>(userService.directReportIds(callerId));
            scopeUserIds.add(callerId);
        }

        List<Attendance> results = attendanceService.searchCorrections(scopeUserIds, fromDt, toDt, status);

        Set<Long> namesNeeded = results.stream()
                .flatMap(a -> java.util.stream.Stream.of(a.getUserId(), a.getApprovedBy()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = namesNeeded.isEmpty() ? Map.of()
                : userRepository.findAllById(namesNeeded).stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername));

        return ResponseEntity.ok(results.stream()
                .map(a -> CorrectionLogEntryResponse.from(a, names.get(a.getUserId()), names.get(a.getApprovedBy())))
                .toList());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AttendanceResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long managerId = resolveUserId(userDetails.getUsername());
        guardManagerOfRecordOrAdmin(id, managerId, userDetails);
        return ResponseEntity.ok(
                AttendanceResponse.from(attendanceService.approve(id, managerId)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AttendanceResponse> reject(
            @PathVariable Long id,
            @RequestBody RejectRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long managerId = resolveUserId(userDetails.getUsername());
        guardManagerOfRecordOrAdmin(id, managerId, userDetails);
        return ResponseEntity.ok(
                AttendanceResponse.from(
                        attendanceService.reject(id, managerId, body.reason())));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void guardSelfOrManagerOrAdmin(Long targetUserId, UserDetails caller) {
        Long callerId = resolveUserId(caller.getUsername());
        if (callerId.equals(targetUserId)) return;
        if (isAdmin(caller)) return;
        if (userService.isManagerOf(callerId, targetUserId)) return;
        throw new AccessDeniedException("Access denied.");
    }

    private void guardManagerOfRecordOrAdmin(Long recordId, Long callerId, UserDetails caller) {
        if (isAdmin(caller)) return;
        Long employeeId = attendanceService.getById(recordId).getUserId();
        if (!userService.isManagerOf(callerId, employeeId)) {
            throw new AccessDeniedException("You do not manage this employee.");
        }
    }

    // ROLE_HR_MANAGER is a standalone role (not part of the general RoleHierarchy) that grants
    // company-wide attendance visibility/approval without the broader powers ROLE_ADMIN carries
    // elsewhere (user management, billing, etc.) — scoped deliberately to this controller only.
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
