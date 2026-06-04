package com.crm.timetracking.service;

import com.crm.timetracking.entity.Attendance;
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
