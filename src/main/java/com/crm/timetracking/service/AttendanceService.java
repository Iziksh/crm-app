package com.crm.timetracking.service;

import com.crm.timetracking.entity.Attendance;
import com.crm.timetracking.enums.AttendanceApprovalStatus;
import com.crm.timetracking.exception.AttendanceValidationException;
import com.crm.timetracking.repository.AttendanceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    /** Israeli labour-law daily ceiling: no more than 12 worked hours may be recorded per day. */
    static final long MAX_DAILY_SECONDS = 12L * 3600L;

    private static final ZoneId ZONE = ZoneId.of("Asia/Jerusalem");

    /** Marks an exit recorded with no matching clock-in — an incomplete line with no total. */
    static final String SOURCE_CLOCK_OUT_LINE = "MANUAL_CLOCK_OUT";

    /** Marks an entry left without an exit — an incomplete line with no total (abandoned clock-in). */
    static final String SOURCE_CLOCK_IN_INCOMPLETE = "MANUAL_CLOCK_IN";

    private final AttendanceRepository attendanceRepo;

    public AttendanceService(AttendanceRepository attendanceRepo) {
        this.attendanceRepo = attendanceRepo;
    }

    public Attendance punchIn(Long userId, String note, String source) {
        return punchIn(userId, note, source, false);
    }

    /**
     * Starts a new work session. If the user already has an open entry (an entry without an exit):
     * <ul>
     *   <li>{@code force == false} (default): rejected — the caller must punch out first.</li>
     *   <li>{@code force == true}: the previous open entry is abandoned in place as an incomplete
     *       line (its entry is kept, its exit stays empty, no total) and a fresh session is started
     *       as the new active one. Used when the employee confirms "Clock in" despite the warning
     *       that a prior entry has no exit.</li>
     * </ul>
     */
    @Transactional
    public Attendance punchIn(Long userId, String note, String source, boolean force) {
        attendanceRepo.findByUserIdAndEndTimeIsNull(userId).ifPresent(open -> {
            if (!force) {
                throw new IllegalStateException(
                        "User " + userId + " already has an active session. Punch out first.");
            }
            // Abandon the open entry: keep its entry time, leave its exit empty (no total).
            // endTime is set to the abandonment instant purely so it is no longer "active";
            // the UI hides it and shows the exit cell empty (see SOURCE_CLOCK_IN_INCOMPLETE).
            open.setEndTime(OffsetDateTime.now());
            open.setDurationSeconds(null);
            open.setSource(SOURCE_CLOCK_IN_INCOMPLETE);
            // saveAndFlush, not save: the UPDATE clearing this row's open state must hit the DB
            // BEFORE the new open row is inserted below. Hibernate orders INSERTs ahead of UPDATEs
            // within a single flush, which would momentarily leave two end_time-IS-NULL rows and
            // trip the uq_attendance_active_session partial unique index.
            attendanceRepo.saveAndFlush(open);
        });
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
        long durationSeconds = Duration.between(session.getStartTime(), now).getSeconds();
        assertDailyLimit(userId, session.getStartTime(), durationSeconds, session.getId());
        session.setEndTime(now);
        session.setDurationSeconds(durationSeconds);
        return attendanceRepo.save(session);
    }

    /**
     * Records an exit for which there was no matching clock-in — used when the employee clicks
     * "Clock out" with no open session (either they forgot to clock in, or they already have an
     * incomplete clock-out line for the day). The line has NO entry: it carries no total
     * (durationSeconds = null) and the UI leaves its "in" cell empty. startTime is set to the exit
     * instant purely as the day anchor so the line still lists under today (all monthly/day queries
     * key off startTime); it is not shown as an entry time. It stands as an incomplete record for
     * the employee or a manager to correct later.
     */
    @Transactional
    public Attendance createClockOutLine(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        // The exit happened at `now`. The schema's chk_end_after_start requires end_time > start_time,
        // so the throwaway anchor entry sits one second earlier; the UI hides this entry and the line
        // carries no total (null duration), so the one-second sliver is never shown.
        Attendance line = new Attendance(userId, now.minusSeconds(1), SOURCE_CLOCK_OUT_LINE);
        line.setEndTime(now);
        line.setDurationSeconds(null);
        return attendanceRepo.save(line);
    }

    @Transactional(readOnly = true)
    public Optional<Attendance> findActiveSession(Long userId) {
        return attendanceRepo.findByUserIdAndEndTimeIsNull(userId);
    }

    @Transactional(readOnly = true)
    public List<Attendance> getMonthlyRecords(Long userId, int year, int month) {
        OffsetDateTime from = YearMonth.of(year, month).atDay(1).atStartOfDay(ZONE).toOffsetDateTime();
        return attendanceRepo.findByUserIdAndPeriod(userId, from, from.plusMonths(1));
    }

    /**
     * Enforces the 12-hour daily ceiling: the sum of all non-rejected worked seconds already
     * recorded on {@code dayAnchor}'s calendar day (Asia/Jerusalem), plus {@code addedSeconds}
     * about to be recorded, may not exceed {@link #MAX_DAILY_SECONDS}. The session identified by
     * {@code excludeSessionId} (if any) is left out of the existing total so an in-place update
     * counts its new value, not its old one. Throws {@link AttendanceValidationException} otherwise.
     */
    private void assertDailyLimit(Long userId, OffsetDateTime dayAnchor,
                                  long addedSeconds, Long excludeSessionId) {
        LocalDate day = dayAnchor.atZoneSameInstant(ZONE).toLocalDate();
        OffsetDateTime from = day.atStartOfDay(ZONE).toOffsetDateTime();
        OffsetDateTime to   = from.plusDays(1);
        long existing = attendanceRepo.findByUserIdAndPeriodExcludingRejected(userId, from, to).stream()
                .filter(a -> excludeSessionId == null || !excludeSessionId.equals(a.getId()))
                .mapToLong(a -> a.getDurationSeconds() != null ? a.getDurationSeconds() : 0L)
                .sum();
        if (existing + addedSeconds > MAX_DAILY_SECONDS) {
            throw new AttendanceValidationException(
                    "Total hours for the day exceed 12. Please reduce the hours to 12.");
        }
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
        assertDailyLimit(userId, start, Duration.between(start, end).getSeconds(), null);
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

    /** Returns sessions awaiting approval, restricted to the given set of employee ids. */
    @Transactional(readOnly = true)
    public List<Attendance> getPendingApprovalsForUsers(List<Long> userIds) {
        if (userIds.isEmpty()) return List.of();
        return attendanceRepo.findByApprovalStatusAndUserIdInOrderByStartTimeAsc(
                AttendanceApprovalStatus.PENDING, userIds);
    }

    /**
     * Full correction history for the Correction Log — any status, optional date range and
     * status filter. {@code userIds == null} means unrestricted (admin/HR view of everyone).
     */
    @Transactional(readOnly = true)
    public List<Attendance> searchCorrections(List<Long> userIds, OffsetDateTime from, OffsetDateTime to,
                                              AttendanceApprovalStatus status) {
        boolean hasFrom = from != null;
        boolean hasTo = to != null;
        boolean hasStatus = status != null;
        return userIds == null
                ? attendanceRepo.searchAllCorrections(hasFrom, from, hasTo, to, hasStatus, status)
                : attendanceRepo.searchCorrectionsForUsers(userIds, hasFrom, from, hasTo, to, hasStatus, status);
    }

    @Transactional(readOnly = true)
    public Attendance getById(Long id) {
        return attendanceRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attendance record " + id + " not found"));
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
