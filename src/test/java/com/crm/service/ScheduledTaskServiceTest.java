package com.crm.service;

import com.crm.domain.entity.ScheduledTask;
import com.crm.domain.enums.AlertImportance;
import com.crm.domain.enums.TaskStatus;
import com.crm.repository.AccountRepository;
import com.crm.repository.ActivityRepository;
import com.crm.repository.ContractRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.NotificationConfigRepository;
import com.crm.repository.OpportunityRepository;
import com.crm.repository.ScheduledTaskRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledTaskServiceTest {

    @Mock ScheduledTaskRepository taskRepository;
    @Mock NotificationConfigRepository configRepository;
    @Mock AlertService alertService;
    @Mock UserRepository userRepository;
    @Mock LeadRepository leadRepository;
    @Mock OpportunityRepository opportunityRepository;
    @Mock ContractRepository contractRepository;
    @Mock ActivityRepository activityRepository;
    @Mock AccountRepository accountRepository;

    @InjectMocks ScheduledTaskService taskService;

    // ── retry ────────────────────────────────────────────────────────────────

    @Test
    void retry_resetsAttemptCountAndSchedulesPending() {
        ScheduledTask task = failedTask(3);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        taskService.retry(1L);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getAttemptCount()).isZero();
        assertThat(task.getFailureReason()).isNull();
        assertThat(task.getScheduledAt()).isAfter(LocalDateTime.now().minusSeconds(10));
    }

    // ── suspend / resume / cancel ─────────────────────────────────────────────

    @Test
    void suspend_setsSuspendedStatus() {
        ScheduledTask task = pendingTask();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        taskService.suspend(1L, "waiting for data");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.SUSPENDED);
        assertThat(task.getSuspendedReason()).isEqualTo("waiting for data");
    }

    @Test
    void resume_setsPendingStatus_whenSuspended() {
        ScheduledTask task = pendingTask();
        task.setStatus(TaskStatus.SUSPENDED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        taskService.resume(1L);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void cancel_setsCancelledStatus() {
        ScheduledTask task = pendingTask();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        taskService.cancel(1L, "no longer needed");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(task.getFailureReason()).isEqualTo("no longer needed");
    }

    // ── executeWithReEvaluation ───────────────────────────────────────────────

    @Test
    void execute_marksCompleted_whenFireAlertSucceeds() {
        ScheduledTask task = pendingTask();
        task.setWorkflowKey("A-05"); // default re-eval → null (proceed)
        // No recipient → fireAlert returns early without calling alertService
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        taskService.executeWithReEvaluation(task);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    void execute_schedulesPending_onFirstFailure() {
        ScheduledTask task = pendingTask();
        task.setWorkflowKey("A-05");
        task.setAttemptCount(0);
        // Simulate alertService throwing when recipient is non-null
        // We can't easily trigger fireAlert to throw without a recipient,
        // so instead test the retry logic by mocking save to simulate a throw
        // from inside fireAlert via a workaround: stub alertService.sendAlertFull to throw
        doThrow(new RuntimeException("DB error"))
                .when(alertService).sendAlertFull(any(), any(), any(), any(), any(), anyInt(), any(), any(), any());

        com.crm.domain.entity.User recipient = new com.crm.domain.entity.User();
        recipient.setUsername("admin");
        task.setRecipient(recipient);

        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(configRepository.findByTopicKeyAndWorkspaceIdIsNull(any())).thenReturn(Optional.empty());

        taskService.executeWithReEvaluation(task);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING); // retryable
        assertThat(task.getAttemptCount()).isEqualTo(1);
        assertThat(task.getFailureReason()).isEqualTo("DB error");
        assertThat(task.getScheduledAt()).isAfter(LocalDateTime.now().minusSeconds(10));
    }

    @Test
    void execute_marksFailedAfterMaxAttempts() {
        ScheduledTask task = pendingTask();
        task.setWorkflowKey("A-05");
        task.setAttemptCount(2); // one more attempt → hits max (3)

        com.crm.domain.entity.User recipient = new com.crm.domain.entity.User();
        recipient.setUsername("admin");
        task.setRecipient(recipient);

        doThrow(new RuntimeException("constraint violation"))
                .when(alertService).sendAlertFull(any(), any(), any(), any(), any(), anyInt(), any(), any(), any());

        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(configRepository.findByTopicKeyAndWorkspaceIdIsNull(any())).thenReturn(Optional.empty());

        taskService.executeWithReEvaluation(task);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getFailedAt()).isNotNull();
        assertThat(task.getFailureReason()).isEqualTo("constraint violation");
    }

    @Test
    void execute_cancelledByReEvaluation_whenLeadDeleted() {
        ScheduledTask task = pendingTask();
        task.setWorkflowKey("L-02");
        task.setTargetEntityId(999L);
        when(leadRepository.findById(999L)).thenReturn(Optional.empty());
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String reason = taskService.executeWithReEvaluation(task);

        assertThat(reason).isEqualTo("Lead deleted");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ScheduledTask pendingTask() {
        ScheduledTask t = new ScheduledTask();
        t.setId(1L);
        t.setWorkflowKey("A-05");
        t.setWorkflowName("Account Modified");
        t.setTargetEntityType("ACCOUNT");
        t.setTargetEntityId(1L);
        t.setStatus(TaskStatus.PENDING);
        t.setPriority(AlertImportance.NORMAL);
        t.setScheduledAt(LocalDateTime.now());
        t.setAttemptCount(0);
        t.setMaxAttempts(3);
        return t;
    }

    private ScheduledTask failedTask(int attempts) {
        ScheduledTask t = pendingTask();
        t.setStatus(TaskStatus.FAILED);
        t.setAttemptCount(attempts);
        t.setFailureReason("some error");
        t.setFailedAt(LocalDateTime.now().minusMinutes(5));
        return t;
    }
}
