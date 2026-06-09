package com.crm.timetracking.service;

import com.crm.timetracking.entity.Attendance;
import com.crm.timetracking.enums.AttendanceApprovalStatus;
import com.crm.timetracking.repository.AttendanceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepo;

    public AttendanceService(AttendanceRepository attendanceRepo) {
        this.attendanceRepo = attendanceRepo;
    }

    @Transactional
    public Attendance punchIn(Long userId, String note, String source) {
        if (attendanceRepo.existsByUserIdAndEndTimeIsNull(userId)) {
            throw new IllegalStateException(
                    "User " + userId + " already has an active session. Punch out first.");
        }
        Attendance session = new Attendance(
                userId, OffsetDateTime.now(), source != null ? source : "MANUAL");
        session.setNote(note);
        return attendanceRepo.save(session);
    }

    @Transactional
    public Attendance punchOut(Long userId) {
        Attendance session = attendanceRepo.findByUserIdAndEndTimeIsNull(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "No active session found for user " + userId + ". Cannot punch out."));
        OffsetDateTime now = OffsetDateTime.now();
        session.setEndTime(now);
        session.setDurationSeconds(Duration.between(session.getStartTime(), now).getSeconds());
        return attendanceRepo.save(session);
    }

    @Transactional(readOnly = true)
    public Optional<Attendance> findActiveSession(Long userId) {
        return attendanceRepo.findByUserIdAndEndTimeIsNull(userId);
    }

    @Transactional(readOnly = true)
    public List<Attendance> getMonthlyRecords(Long userId, int year, int month) {
        ZoneId zone = ZoneId.of("Asia/Jerusalem");
        OffsetDateTime from = YearMonth.of(year, month).atDay(1).atStartOfDay(zone).toOffsetDateTime();
        return attendanceRepo.findByUserIdAndPeriod(userId, from, from.plusMonths(1));
    }

    // ── MISSED CLOCK-IN CORRECTION ────────────────────────────────────────────

    /**
     * Employee submits a correction for a session they forgot to clock in/out for.
     * Both start and end must be provided. The record is created with
     * approvalStatus=PENDING and source='EMPLOYEE_CORRECTION'; it is visible
     * in the monthly view but not counted in totals until a manager approves it.
     */
    @Transactional
    public Attendance createManualEntry(Long userId, OffsetDateTime start,
                                        OffsetDateTime end, String note) {
        if (end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("Exit time must be after entry time.");
        }
        Attendance session = new Attendance(userId, start, "EMPLOYEE_CORRECTION");
        session.setEndTime(end);
        session.setDurationSeconds(Duration.between(start, end).getSeconds());
        session.setNote(note);
        session.setApprovalStatus(AttendanceApprovalStatus.PENDING);
        return attendanceRepo.save(session);
    }

    // ── MANAGER APPROVAL ──────────────────────────────────────────────────────

    /** Returns all sessions awaiting manager approval, across all users. */
    @Transactional(readOnly = true)
    public List<Attendance> getPendingApprovals() {
        return attendanceRepo.findByApprovalStatusOrderByStartTimeAsc(
                AttendanceApprovalStatus.PENDING);
    }

    /**
     * Manager approves a manual correction. The session now counts toward
     * monthly totals identically to a normal punch session.
     */
    @Transactional
    public Attendance approve(Long sessionId, Long managerId) {
        Attendance session = getPendingOrThrow(sessionId);
        session.setApprovalStatus(AttendanceApprovalStatus.APPROVED);
        session.setApprovedBy(managerId);
        session.setApprovedAt(OffsetDateTime.now());
        session.setRejectionReason(null);
        return attendanceRepo.save(session);
    }

    /**
     * Manager rejects a manual correction. The session remains visible in the
     * employee's history (status=REJECTED) but is excluded from all totals.
     */
    @Transactional
    public Attendance reject(Long sessionId, Long managerId, String reason) {
        Attendance session = getPendingOrThrow(sessionId);
        session.setApprovalStatus(AttendanceApprovalStatus.REJECTED);
        session.setApprovedBy(managerId);
        session.setApprovedAt(OffsetDateTime.now());
        session.setRejectionReason(reason);
        return attendanceRepo.save(session);
    }

    private Attendance getPendingOrThrow(Long sessionId) {
        Attendance session = attendanceRepo.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Attendance record " + sessionId + " not found"));
        if (session.getApprovalStatus() != AttendanceApprovalStatus.PENDING) {
            throw new IllegalStateException(
                    "Session " + sessionId + " is not in PENDING state.");
        }
        return session;
    }

    // ── EDIT (manager override) ───────────────────────────────────────────────

    @Transactional
    public Attendance editSession(Long sessionId, OffsetDateTime newStart, OffsetDateTime newEnd) {
        Attendance session = attendanceRepo.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Attendance record " + sessionId + " not found"));
        if (newEnd != null && !newEnd.isAfter(newStart)) {
            throw new IllegalArgumentException("end_time must be after start_time");
        }
        session.setStartTime(newStart);
        session.setEndTime(newEnd);
        session.setDurationSeconds(
                newEnd != null ? Duration.between(newStart, newEnd).getSeconds() : null);
        return attendanceRepo.save(session);
    }
}
