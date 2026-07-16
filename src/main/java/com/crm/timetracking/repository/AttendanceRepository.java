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

    /** Pending corrections restricted to a set of employee ids — used for manager-scoped approval views. */
    List<Attendance> findByApprovalStatusAndUserIdInOrderByStartTimeAsc(
            AttendanceApprovalStatus status, List<Long> userIds);

    /**
     * Full correction history (any status) across everyone — used by admins in the Correction Log.
     * Uses boolean "has*" flags rather than "{@code :param IS NULL}" checks: Postgres/Hibernate
     * cannot infer a bind parameter's type from an IS NULL comparison alone (SQLState 42P18,
     * "could not determine data type of parameter"), since that usage gives it no column context.
     * Every parameter here is instead only ever used in a properly-typed column comparison.
     */
    @Query("""
        SELECT a FROM Attendance a
        WHERE a.approvalStatus IS NOT NULL
          AND (:hasFrom = false OR a.startTime >= :from)
          AND (:hasTo = false OR a.startTime < :to)
          AND (:hasStatus = false OR a.approvalStatus = :status)
        ORDER BY a.startTime DESC
        """)
    List<Attendance> searchAllCorrections(
            @Param("hasFrom") boolean hasFrom, @Param("from") OffsetDateTime from,
            @Param("hasTo") boolean hasTo, @Param("to") OffsetDateTime to,
            @Param("hasStatus") boolean hasStatus, @Param("status") AttendanceApprovalStatus status);

    /** Full correction history (any status) restricted to a set of employee ids — self/manager scoped. */
    @Query("""
        SELECT a FROM Attendance a
        WHERE a.userId IN :userIds
          AND a.approvalStatus IS NOT NULL
          AND (:hasFrom = false OR a.startTime >= :from)
          AND (:hasTo = false OR a.startTime < :to)
          AND (:hasStatus = false OR a.approvalStatus = :status)
        ORDER BY a.startTime DESC
        """)
    List<Attendance> searchCorrectionsForUsers(
            @Param("userIds") List<Long> userIds,
            @Param("hasFrom") boolean hasFrom, @Param("from") OffsetDateTime from,
            @Param("hasTo") boolean hasTo, @Param("to") OffsetDateTime to,
            @Param("hasStatus") boolean hasStatus, @Param("status") AttendanceApprovalStatus status);

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
