package com.crm.timetracking.repository;

import com.crm.timetracking.entity.Attendance;
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
}
