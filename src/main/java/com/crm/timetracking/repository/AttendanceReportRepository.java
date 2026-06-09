package com.crm.timetracking.repository;

import com.crm.timetracking.entity.AttendanceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceReportRepository extends JpaRepository<AttendanceReport, Long> {

    /** All reports for one user on one day, ordered by entry time then id. */
    List<AttendanceReport> findByUserIdAndReportDateOrderByEntryTimeAscIdAsc(
            Long userId, LocalDate reportDate);

    /** All reports for a user within an inclusive date range, ordered for calendar display. */
    @Query("""
        SELECT r FROM AttendanceReport r
        WHERE r.userId     = :userId
          AND r.reportDate >= :from
          AND r.reportDate <= :to
        ORDER BY r.reportDate ASC, r.entryTime ASC NULLS LAST, r.id ASC
    """)
    List<AttendanceReport> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("from")   LocalDate from,
            @Param("to")     LocalDate to);

    /**
     * Per-day duration sums for calendar cell totals.
     * Returns Object[]{LocalDate reportDate, Long sumDurationMinutes}.
     */
    @Query("""
        SELECT r.reportDate, SUM(r.durationMinutes)
        FROM AttendanceReport r
        WHERE r.userId          = :userId
          AND r.reportDate      >= :from
          AND r.reportDate      <= :to
          AND r.durationMinutes IS NOT NULL
        GROUP BY r.reportDate
        ORDER BY r.reportDate ASC
    """)
    List<Object[]> sumWorkedMinutesByDay(
            @Param("userId") Long userId,
            @Param("from")   LocalDate from,
            @Param("to")     LocalDate to);

    /** All reports for ALL users within a date range — used by ExcelReportService. */
    @Query("""
        SELECT r FROM AttendanceReport r
        WHERE r.reportDate >= :from
          AND r.reportDate <= :to
        ORDER BY r.userId ASC, r.reportDate ASC, r.entryTime ASC NULLS LAST
    """)
    List<AttendanceReport> findAllByDateRange(
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to);
}