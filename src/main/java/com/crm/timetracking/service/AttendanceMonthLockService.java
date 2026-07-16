package com.crm.timetracking.service;

import com.crm.timetracking.dto.MonthLockResponse;
import com.crm.timetracking.entity.AttendanceMonthLock;
import com.crm.timetracking.enums.AttendanceMonthLockStatus;
import com.crm.timetracking.exception.AttendanceValidationException;
import com.crm.timetracking.repository.AttendanceMonthLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class AttendanceMonthLockService {

    private final AttendanceMonthLockRepository lockRepo;

    public AttendanceMonthLockService(AttendanceMonthLockRepository lockRepo) {
        this.lockRepo = lockRepo;
    }

    @Transactional(readOnly = true)
    public MonthLockResponse getStatus(Long userId, int year, int month) {
        return lockRepo.findByUserIdAndYearAndMonth(userId, year, month)
                .map(MonthLockResponse::from)
                .orElseGet(() -> MonthLockResponse.openDefault(userId, year, month));
    }

    /** True if the month is not open for editing (SUBMITTED or APPROVED) — used to block report edits. */
    @Transactional(readOnly = true)
    public boolean isLocked(Long userId, int year, int month) {
        AttendanceMonthLockStatus status = lockRepo.findByUserIdAndYearAndMonth(userId, year, month)
                .map(AttendanceMonthLock::getStatus)
                .orElse(AttendanceMonthLockStatus.OPEN);
        return status == AttendanceMonthLockStatus.SUBMITTED || status == AttendanceMonthLockStatus.APPROVED;
    }

    /** Employee locks the month for manager review. */
    public MonthLockResponse submit(Long userId, int year, int month) {
        AttendanceMonthLock lock = getOrCreate(userId, year, month);
        if (lock.getStatus() == AttendanceMonthLockStatus.SUBMITTED
                || lock.getStatus() == AttendanceMonthLockStatus.APPROVED) {
            throw new AttendanceValidationException("This month has already been submitted.");
        }
        lock.setStatus(AttendanceMonthLockStatus.SUBMITTED);
        lock.setSubmittedAt(OffsetDateTime.now());
        lock.setRejectionReason(null);
        return MonthLockResponse.from(lockRepo.save(lock));
    }

    /** Manager approves a submitted month. */
    public MonthLockResponse approve(Long userId, int year, int month, Long managerId) {
        AttendanceMonthLock lock = requireSubmitted(userId, year, month);
        lock.setStatus(AttendanceMonthLockStatus.APPROVED);
        lock.setApprovedBy(managerId);
        lock.setApprovedAt(OffsetDateTime.now());
        return MonthLockResponse.from(lockRepo.save(lock));
    }

    /** Manager sends a submitted month back to the employee for corrections. */
    public MonthLockResponse reject(Long userId, int year, int month, Long managerId, String reason) {
        AttendanceMonthLock lock = requireSubmitted(userId, year, month);
        lock.setStatus(AttendanceMonthLockStatus.REJECTED);
        lock.setApprovedBy(managerId);
        lock.setApprovedAt(OffsetDateTime.now());
        lock.setRejectionReason(reason);
        return MonthLockResponse.from(lockRepo.save(lock));
    }

    private AttendanceMonthLock requireSubmitted(Long userId, int year, int month) {
        AttendanceMonthLock lock = lockRepo.findByUserIdAndYearAndMonth(userId, year, month)
                .orElseThrow(() -> new AttendanceValidationException("This month has not been submitted yet."));
        if (lock.getStatus() != AttendanceMonthLockStatus.SUBMITTED) {
            throw new AttendanceValidationException("This month is not awaiting approval.");
        }
        return lock;
    }

    private AttendanceMonthLock getOrCreate(Long userId, int year, int month) {
        return lockRepo.findByUserIdAndYearAndMonth(userId, year, month)
                .orElseGet(() -> new AttendanceMonthLock(userId, year, month));
    }
}
