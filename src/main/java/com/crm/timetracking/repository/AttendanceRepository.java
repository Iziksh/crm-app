package com.crm.timetracking.repository;

import com.crm.timetracking.entity.Attendance;
import com.crm.timetracking.enums.AttendanceApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserIdAndEndTimeIsNull(Long userId);

    boolean existsByUserIdAndEndTimeIsNull(Long userId);

    @Query("""
        SELECT a FROM Attendance a
        WHERE a.userId = :userId
          AND a.startTime >= :from
          AND a.startTime < :to
        ORDER BY a.startTime ASC
        """)
    List<Attendance> findByUserIdAndPeriod(
            @Param("userId") Long userId,
            @Param("from")   OffsetDateTime from,
            @Param("to")     OffsetDateTime to);

    @Query("""
        SELECT a FROM Attendance a
        WHERE a.startTime >= :from
          AND a.startTime < :to
        ORDER BY a.userId ASC, a.startTime ASC
        """)
    List<Attendance> findAllByPeriod(
            @Param("from") OffsetDateTime from,
            @Param("to")   OffsetDateTime to);

    /** All pending corrections across all users — used by the manager approval view. */
    List<Attendance> findByApprovalStatusOrderByStartTimeAsc(AttendanceApprovalStatus status);

    /**
     * Monthly records for one user, excluding REJECTED corrections.
     * Used for work-hour totals: normal punches (approvalStatus=null) and
     * APPROVED corrections both count; PENDING shows but is flagged; REJECTED is hidden.
     */
    @Query("""
        SELECT a FROM Attendance a
        WHERE a.userId    = :userId
          AND a.startTime >= :from
          AND a.startTime < :to
          AND (a.approvalStatus IS NULL OR a.approvalStatus != 'REJECTED')
        ORDER BY a.startTime ASC
        """)
    List<Attendance> findByUserIdAndPeriodExcludingRejected(
            @Param("userId") Long userId,
            @Param("from")   OffsetDateTime from,
            @Param("to")     OffsetDateTime to);
}
