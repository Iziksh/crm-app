package com.crm.timetracking.controller;

import com.crm.domain.entity.User;
import com.crm.repository.UserRepository;
import com.crm.service.UserService;
import com.crm.timetracking.dto.MonthLockResponse;
import com.crm.timetracking.service.AttendanceMonthLockService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance-month-lock")
public class AttendanceMonthLockController {

    private final AttendanceMonthLockService lockService;
    private final UserRepository             userRepository;
    private final UserService                userService;

    public AttendanceMonthLockController(AttendanceMonthLockService lockService,
                                         UserRepository userRepository,
                                         UserService userService) {
        this.lockService  = lockService;
        this.userRepository = userRepository;
        this.userService    = userService;
    }

    record RejectRequest(String reason) {}

    @GetMapping
    public ResponseEntity<MonthLockResponse> getStatus(
            @RequestParam Long userId, @RequestParam int year, @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails) {
        guardSelfOrManagerOrAdmin(userId, userDetails);
        return ResponseEntity.ok(lockService.getStatus(userId, year, month));
    }

    /** Employee locks their own month for manager review. */
    @PostMapping("/submit")
    public ResponseEntity<MonthLockResponse> submit(
            @RequestParam int year, @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails.getUsername());
        return ResponseEntity.ok(lockService.submit(userId, year, month));
    }

    @PostMapping("/approve")
    public ResponseEntity<MonthLockResponse> approve(
            @RequestParam Long userId, @RequestParam int year, @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long managerId = resolveUserId(userDetails.getUsername());
        guardManagerOrAdmin(userId, managerId, userDetails);
        return ResponseEntity.ok(lockService.approve(userId, year, month, managerId));
    }

    @PostMapping("/reject")
    public ResponseEntity<MonthLockResponse> reject(
            @RequestParam Long userId, @RequestParam int year, @RequestParam int month,
            @RequestBody RejectRequest body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long managerId = resolveUserId(userDetails.getUsername());
        guardManagerOrAdmin(userId, managerId, userDetails);
        return ResponseEntity.ok(lockService.reject(userId, year, month, managerId, body.reason()));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void guardSelfOrManagerOrAdmin(Long targetUserId, UserDetails caller) {
        Long callerId = resolveUserId(caller.getUsername());
        if (callerId.equals(targetUserId)) return;
        if (isAdmin(caller)) return;
        if (userService.isManagerOf(callerId, targetUserId)) return;
        throw new AccessDeniedException("Access denied.");
    }

    private void guardManagerOrAdmin(Long targetUserId, Long callerId, UserDetails caller) {
        if (isAdmin(caller)) return;
        if (!userService.isManagerOf(callerId, targetUserId)) {
            throw new AccessDeniedException("You do not manage this employee.");
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
