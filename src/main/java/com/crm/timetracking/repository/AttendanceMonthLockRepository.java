package com.crm.timetracking.repository;

import com.crm.timetracking.entity.AttendanceMonthLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceMonthLockRepository extends JpaRepository<AttendanceMonthLock, Long> {
    Optional<AttendanceMonthLock> findByUserIdAndYearAndMonth(Long userId, int year, int month);
}
