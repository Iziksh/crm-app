package com.crm.service;

import com.crm.domain.entity.ScheduledTask;
import com.crm.domain.enums.TaskStatus;
import com.crm.repository.ScheduledTaskRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Picks up PENDING tasks every 60 seconds and executes them with re-evaluation.
 * Batch size: 100 tasks per tick (FSD §4.5.1).
 */
@Service
public class ScheduledTaskProcessor {

    private static final int BATCH_SIZE = 100;

    private final ScheduledTaskRepository taskRepository;
    private final ScheduledTaskService taskService;

    public ScheduledTaskProcessor(ScheduledTaskRepository taskRepository,
                                   ScheduledTaskService taskService) {
        this.taskRepository = taskRepository;
        this.taskService = taskService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void processDueTasks() {
        List<ScheduledTask> dueTasks = taskRepository.findDueTasks(
                LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE));

        for (ScheduledTask task : dueTasks) {
            try {
                taskService.executeWithReEvaluation(task);
            } catch (Exception e) {
                // Log and continue — never let one bad task stop the batch
                System.err.println("ScheduledTaskProcessor: error processing task " + task.getId() + ": " + e.getMessage());
            }
        }
    }

    /** Re-queues failed tasks that still have retries remaining. Runs every 30 minutes. */
    @Scheduled(fixedDelay = 1_800_000)
    public void requeueRetryable() {
        List<ScheduledTask> retryable = taskRepository.findRetryable();
        for (ScheduledTask task : retryable) {
            task.setStatus(TaskStatus.PENDING);
            task.setScheduledAt(LocalDateTime.now().plusMinutes(5L * task.getAttemptCount()));
            taskRepository.save(task);
        }
    }
}
