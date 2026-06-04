package com.crm.repository;

import com.crm.domain.entity.ScheduledTask;
import com.crm.domain.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {

    // Processor: fetch due tasks in priority order
    @Query("SELECT t FROM ScheduledTask t WHERE t.status = 'PENDING' AND t.scheduledAt <= :now ORDER BY t.priority ASC, t.scheduledAt ASC")
    List<ScheduledTask> findDueTasks(@Param("now") LocalDateTime now, Pageable pageable);

    // Dedup: already have a pending/processing task for this entity+workflow?
    boolean existsByWorkflowKeyAndTargetEntityTypeAndTargetEntityIdAndStatusIn(
            String workflowKey, String entityType, Long entityId, List<TaskStatus> statuses);

    // Admin UI
    Page<ScheduledTask> findByStatusOrderByScheduledAtDesc(TaskStatus status, Pageable pageable);
    Page<ScheduledTask> findAllByOrderByScheduledAtDesc(Pageable pageable);

    // Stats
    long countByStatus(TaskStatus status);

    @Query("SELECT COUNT(t) FROM ScheduledTask t WHERE t.status = 'COMPLETED' AND t.completedAt >= :since")
    long countCompletedSince(@Param("since") LocalDateTime since);

    // Retry: find failed tasks eligible for retry
    @Query("SELECT t FROM ScheduledTask t WHERE t.status = 'FAILED' AND t.attemptCount < t.maxAttempts")
    List<ScheduledTask> findRetryable();
}
