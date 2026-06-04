package com.crm.timetracking.controller;

import com.crm.domain.entity.User;
import com.crm.repository.UserRepository;
import com.crm.timetracking.dto.AttendanceResponse;
import com.crm.timetracking.service.AttendanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserRepository    userRepository;

    public AttendanceController(AttendanceService attendanceService, UserRepository userRepository) {
        this.attendanceService = attendanceService;
        this.userRepository    = userRepository;
    }

    record PunchInRequest(String note, String source) {}
    record EditSessionRequest(OffsetDateTime newStart, OffsetDateTime newEnd) {}

    @PostMapping("/punch-in")
    public ResponseEntity<AttendanceResponse> punchIn(
            @RequestBody(required = false) PunchInRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long   userId = resolveUserId(userDetails.getUsername());
        String note   = body != null ? body.note()   : null;
        String source = body != null ? body.source() : "MANUAL";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AttendanceResponse.from(attendanceService.punchIn(userId, note, source)));
    }

    @PostMapping("/punch-out")
    public ResponseEntity<AttendanceResponse> punchOut(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails.getUsername());
        return ResponseEntity.ok(AttendanceResponse.from(attendanceService.punchOut(userId)));
    }

    @GetMapping("/active")
    public ResponseEntity<AttendanceResponse> getActive(@RequestParam Long userId) {
        return attendanceService.findActiveSession(userId)
                .map(a -> ResponseEntity.ok(AttendanceResponse.from(a)))
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<AttendanceResponse>> getMonthly(
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(
                attendanceService.getMonthlyRecords(userId, year, month).stream()
                        .map(AttendanceResponse::from).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttendanceResponse> editSession(
            @PathVariable Long id,
            @RequestBody EditSessionRequest body) {
        return ResponseEntity.ok(AttendanceResponse.from(
                attendanceService.editSession(id, body.newStart(), body.newEnd())));
    }

    private Long resolveUserId(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }
}
