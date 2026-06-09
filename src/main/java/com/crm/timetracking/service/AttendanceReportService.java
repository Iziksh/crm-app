package com.crm.timetracking.service;

import com.crm.repository.UserRepository;
import com.crm.timetracking.dto.*;
import com.crm.timetracking.entity.AttendanceReport;
import com.crm.timetracking.entity.Holiday;
import com.crm.timetracking.enums.AttendanceReportType;
import com.crm.timetracking.exception.AttendanceValidationException;
import com.crm.timetracking.repository.AttendanceReportRepository;
import com.crm.timetracking.repository.HolidayRepository;
import com.crm.timetracking.util.DurationCalculator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceReportService {

    // Standard workday = 8 hours = 480 minutes (Israeli labour law baseline).
    private static final int DEFAULT_STANDARD_MINUTES = 480;

    private final AttendanceReportRepository reportRepo;
    private final HolidayRepository          holidayRepo;
    private final UserRepository             userRepo;

    public AttendanceReportService(AttendanceReportRepository reportRepo,
                                   HolidayRepository holidayRepo,
                                   UserRepository userRepo) {
        this.reportRepo  = reportRepo;
        this.holidayRepo = holidayRepo;
        this.userRepo    = userRepo;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public AttendanceReportResponse createReport(Long userId, AttendanceReportRequest req) {
        validate(req, null, userId);
        AttendanceReport report = new AttendanceReport();
        applyRequest(report, userId, req);
        return AttendanceReportResponse.from(reportRepo.save(report));
    }

    // ── EDIT ──────────────────────────────────────────────────────────────────

    public AttendanceReportResponse editReport(Long reportId, AttendanceReportRequest req) {
        AttendanceReport report = getOrThrow(reportId);
        validate(req, reportId, report.getUserId());
        applyRequest(report, report.getUserId(), req);
        return AttendanceReportResponse.from(reportRepo.save(report));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    public void deleteReport(Long reportId) {
        reportRepo.delete(getOrThrow(reportId));
    }

    // ── MONTH QUERY ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AttendanceReportResponse> getReportsForMonth(Long userId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = YearMonth.of(year, month).atEndOfMonth();
        return reportRepo.findByUserIdAndDateRange(userId, from, to)
                .stream().map(AttendanceReportResponse::from).toList();
    }

    // ── DAY QUERY ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AttendanceReportResponse> getReportsForDay(Long userId, LocalDate date) {
        return reportRepo
                .findByUserIdAndReportDateOrderByEntryTimeAscIdAsc(userId, date)
                .stream()
                .map(AttendanceReportResponse::from)
                .toList();
    }

    // ── MONTHLY CALENDAR ──────────────────────────────────────────────────────

    /**
     * Returns a full monthly calendar for one user.
     *
     * Israeli workweek: Sunday–Thursday. Friday and Saturday are non-work days
     * (standardMinutes=0). Holiday credit comes from the holidays table (Phase 24).
     */
    @Transactional(readOnly = true)
    public MonthlyCalendarResponse getMonthlyCalendar(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate from      = yearMonth.atDay(1);
        LocalDate to        = yearMonth.atEndOfMonth();

        // One query for all manual reports in the month
        List<AttendanceReport> allReports = reportRepo.findByUserIdAndDateRange(userId, from, to);
        Map<LocalDate, List<AttendanceReport>> byDate = allReports.stream()
                .collect(Collectors.groupingBy(
                        AttendanceReport::getReportDate, LinkedHashMap::new, Collectors.toList()));

        // One query for all holidays in the month (Phase 22 repo)
        Map<LocalDate, Holiday> holidayMap = holidayRepo.findByDateBetween(from, to).stream()
                .collect(Collectors.toMap(Holiday::getDate, h -> h, (a, b) -> a));

        String username = userRepo.findById(userId)
                .map(u -> u.getUsername())
                .orElse("unknown");

        List<DayCalendarEntry> days = new ArrayList<>();
        int totalWorked   = 0;
        int totalStandard = 0;

        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DayOfWeek dow       = d.getDayOfWeek();
            boolean   isWeekend = (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY);
            Holiday   holiday   = holidayMap.get(d);
            boolean   isHoliday = holiday != null;
            int       standard  = resolveStandardMinutes(isWeekend, isHoliday, holiday);

            List<AttendanceReport> dayReports = byDate.getOrDefault(d, List.of());
            int worked = computeWorkedMinutes(dayReports, standard);

            days.add(new DayCalendarEntry(
                    d,
                    dayReports.stream().map(AttendanceReportResponse::from).toList(),
                    worked,
                    standard,
                    isWeekend,
                    isHoliday,
                    isHoliday ? holiday.getName() : null,
                    worked - standard
            ));

            totalWorked   += worked;
            totalStandard += standard;
        }

        return new MonthlyCalendarResponse(
                userId, username, year, month, days,
                totalWorked, totalStandard, totalWorked - totalStandard);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private void validate(AttendanceReportRequest req, Long excludeId, Long userId) {
        if (req.reportType() == AttendanceReportType.PRESENCE
                && (req.entryTime() == null || req.exitTime() == null)) {
            throw new AttendanceValidationException(
                    "סוג דיווח נוכחות דורש שעת כניסה ושעת יציאה");
        }

        if (req.entryTime() != null && req.exitTime() != null) {
            int minutes = DurationCalculator.computeMinutes(req.entryTime(), req.exitTime());

            if (minutes == 0) {
                throw new AttendanceValidationException(
                        "שעת כניסה ויציאה זהות — המשך חייב להיות גדול מאפס");
            }
            if (minutes >= 24 * 60) {
                throw new AttendanceValidationException(
                        "משך משמרת חייב להיות קצר מ-24 שעות");
            }
            if (req.reportType() == AttendanceReportType.PRESENCE) {
                checkNoPresenceOverlap(userId, req.reportDate(),
                        req.entryTime(), req.exitTime(),
                        excludeId != null ? excludeId : -1L);
            }
        }
    }

    /**
     * Loads all PRESENCE reports for (userId, date), excludes the record being edited,
     * then checks pairwise interval overlap using minute-of-day arithmetic so
     * midnight-crossing shifts are handled correctly.
     */
    private void checkNoPresenceOverlap(Long userId, LocalDate date,
                                        LocalTime entry, LocalTime exit, Long excludeId) {
        reportRepo.findByUserIdAndReportDateOrderByEntryTimeAscIdAsc(userId, date)
                .stream()
                .filter(r -> r.getReportType() == AttendanceReportType.PRESENCE)
                .filter(r -> !r.getId().equals(excludeId))
                .filter(r -> r.getEntryTime() != null && r.getExitTime() != null)
                .forEach(r -> {
                    if (timeRangesOverlap(entry, exit, r.getEntryTime(), r.getExitTime())) {
                        throw new AttendanceValidationException(
                                "קיימת חפיפה עם דיווח נוכחות אחר באותו יום");
                    }
                });
    }

    /**
     * Returns true when two time ranges overlap, handling midnight-crossing by converting
     * to minute-of-day [0, 2879): if end < start, end += 1440.
     */
    private boolean timeRangesOverlap(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        int start1 = s1.toSecondOfDay() / 60;
        int end1   = s1.isAfter(e1) ? e1.toSecondOfDay() / 60 + 1440 : e1.toSecondOfDay() / 60;
        int start2 = s2.toSecondOfDay() / 60;
        int end2   = s2.isAfter(e2) ? e2.toSecondOfDay() / 60 + 1440 : e2.toSecondOfDay() / 60;
        return !(end1 <= start2 || end2 <= start1);
    }

    /** Populates all mutable fields and recomputes durationMinutes on every save. */
    private void applyRequest(AttendanceReport report, Long userId, AttendanceReportRequest req) {
        report.setUserId(userId);
        report.setReportDate(req.reportDate());
        report.setEntryTime(req.entryTime());
        report.setExitTime(req.exitTime());
        report.setNote(req.note());
        report.setReportType(req.reportType());
        report.setEquateToStandard(req.equateToStandard());
        report.setDurationMinutes(
                (req.entryTime() != null && req.exitTime() != null)
                        ? DurationCalculator.computeMinutes(req.entryTime(), req.exitTime())
                        : null);
    }

    /**
     * Resolves expected (standard) minutes for one calendar day:
     * - Weekend (Fri/Sat): 0
     * - Holiday on a workday: creditHours × 60
     * - Normal workday: DEFAULT_STANDARD_MINUTES (480)
     */
    private int resolveStandardMinutes(boolean isWeekend, boolean isHoliday, Holiday holiday) {
        if (isWeekend) return 0;
        if (isHoliday) return holiday.getCreditHours()
                .multiply(BigDecimal.valueOf(60)).intValue();
        return DEFAULT_STANDARD_MINUTES;
    }

    /**
     * Computes total "counted" minutes for one calendar day:
     * - PRESENCE: adds actual durationMinutes
     * - creditsStandardHours types: credits standardMinutes once
     * - ABSENCE: contributes 0
     * - equateToStandard flag: overrides to standardMinutes, applied once (first wins)
     */
    private int computeWorkedMinutes(List<AttendanceReport> reports, int standardMinutes) {
        int     worked           = 0;
        boolean standardCredited = false;

        for (AttendanceReport r : reports) {
            if (r.isEquateToStandard() && !standardCredited) {
                worked += standardMinutes;
                standardCredited = true;
                continue;
            }
            if (r.getReportType().isCountsAsWorked() && r.getDurationMinutes() != null) {
                worked += r.getDurationMinutes();
            } else if (r.getReportType().isCreditsStandardHours() && !standardCredited) {
                worked += standardMinutes;
                standardCredited = true;
            }
        }
        return worked;
    }

    private AttendanceReport getOrThrow(Long id) {
        return reportRepo.findById(id).orElseThrow(
                () -> new EntityNotFoundException("AttendanceReport " + id + " not found"));
    }
}