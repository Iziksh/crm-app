package com.crm.service;

import com.crm.domain.entity.*;
import com.crm.domain.enums.*;
import com.crm.dto.response.ScheduledTaskResponse;
import com.crm.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ScheduledTaskService {

    private final ScheduledTaskRepository taskRepository;
    private final NotificationConfigRepository configRepository;
    private final AlertService alertService;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final ContractRepository contractRepository;
    private final ActivityRepository activityRepository;
    private final AccountRepository accountRepository;

    public ScheduledTaskService(ScheduledTaskRepository taskRepository,
                                 NotificationConfigRepository configRepository,
                                 AlertService alertService,
                                 UserRepository userRepository,
                                 LeadRepository leadRepository,
                                 OpportunityRepository opportunityRepository,
                                 ContractRepository contractRepository,
                                 ActivityRepository activityRepository,
                                 AccountRepository accountRepository) {
        this.taskRepository = taskRepository;
        this.configRepository = configRepository;
        this.alertService = alertService;
        this.userRepository = userRepository;
        this.leadRepository = leadRepository;
        this.opportunityRepository = opportunityRepository;
        this.contractRepository = contractRepository;
        this.activityRepository = activityRepository;
        this.accountRepository = accountRepository;
    }

    // ── Admin query methods ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ScheduledTaskResponse> findAll(Pageable pageable) {
        return taskRepository.findAllByOrderByScheduledAtDesc(pageable).map(ScheduledTaskResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ScheduledTaskResponse> findByStatus(TaskStatus status, Pageable pageable) {
        return taskRepository.findByStatusOrderByScheduledAtDesc(status, pageable).map(ScheduledTaskResponse::from);
    }

    @Transactional(readOnly = true)
    public ScheduledTaskResponse findById(Long id) {
        return ScheduledTaskResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public long countByStatus(TaskStatus status) {
        return taskRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countCompletedToday() {
        return taskRepository.countCompletedSince(LocalDateTime.now().toLocalDate().atStartOfDay());
    }

    // ── Admin overrides ──────────────────────────────────────────────────────

    public ScheduledTaskResponse runNow(Long id) {
        ScheduledTask task = getOrThrow(id);
        if (task.getStatus() == TaskStatus.SUSPENDED || task.getStatus() == TaskStatus.PENDING) {
            task.setScheduledAt(LocalDateTime.now());
            task.setStatus(TaskStatus.PENDING);
            taskRepository.save(task);
            executeWithReEvaluation(task);
        }
        return ScheduledTaskResponse.from(task);
    }

    public ScheduledTaskResponse suspend(Long id, String reason) {
        ScheduledTask task = getOrThrow(id);
        task.setStatus(TaskStatus.SUSPENDED);
        task.setSuspendedReason(reason);
        return ScheduledTaskResponse.from(taskRepository.save(task));
    }

    public ScheduledTaskResponse resume(Long id) {
        ScheduledTask task = getOrThrow(id);
        if (task.getStatus() == TaskStatus.SUSPENDED) {
            task.setStatus(TaskStatus.PENDING);
        }
        return ScheduledTaskResponse.from(taskRepository.save(task));
    }

    public ScheduledTaskResponse cancel(Long id, String reason) {
        ScheduledTask task = getOrThrow(id);
        task.setStatus(TaskStatus.CANCELLED);
        task.setFailureReason(reason);
        return ScheduledTaskResponse.from(taskRepository.save(task));
    }

    public ScheduledTaskResponse retry(Long id) {
        ScheduledTask task = getOrThrow(id);
        task.setStatus(TaskStatus.PENDING);
        task.setAttemptCount(0);
        task.setScheduledAt(LocalDateTime.now().plusMinutes(5));
        task.setFailureReason(null);
        return ScheduledTaskResponse.from(taskRepository.save(task));
    }

    // ── Core execution with FSD re-evaluation failsafe ───────────────────────

    /**
     * Performs re-evaluation check one moment before execution.
     * Returns null if OK to proceed, or the cancellation reason string.
     */
    public String executeWithReEvaluation(ScheduledTask task) {
        task.setStatus(TaskStatus.PROCESSING);
        task.setAttemptCount(task.getAttemptCount() + 1);
        task.setLastAttemptedAt(LocalDateTime.now());
        task = taskRepository.save(task); // reassign so lazy proxies (recipient etc.) load in this session

        String cancelReason = reEvaluate(task);
        if (cancelReason != null) {
            task.setStatus(TaskStatus.CANCELLED);
            task.setFailureReason("Re-evaluation: " + cancelReason);
            taskRepository.save(task);
            return cancelReason;
        }

        try {
            fireAlert(task);
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            if (task.getAttemptCount() >= task.getMaxAttempts()) {
                task.setStatus(TaskStatus.FAILED);
                task.setFailedAt(LocalDateTime.now());
            } else {
                task.setStatus(TaskStatus.PENDING);
                task.setScheduledAt(LocalDateTime.now().plusMinutes(5L * task.getAttemptCount()));
            }
            task.setFailureReason(e.getMessage());
        }
        taskRepository.save(task);
        return null;
    }

    private String reEvaluate(ScheduledTask task) {
        String key = task.getWorkflowKey();
        Long entityId = task.getTargetEntityId();

        return switch (key) {
            case "L-02" -> {
                Lead l = leadRepository.findById(entityId).orElse(null);
                if (l == null) yield "Lead deleted";
                if (l.getAssignedTo() != null) yield "Lead is now assigned";
                if (l.getStatus() == LeadStatus.WON || l.getStatus() == LeadStatus.LOST) yield "Lead in terminal status";
                yield null;
            }
            case "L-03" -> {
                Lead l = leadRepository.findById(entityId).orElse(null);
                if (l == null) yield "Lead deleted";
                if (l.getStatus() != LeadStatus.NEW && l.getStatus() != LeadStatus.CONTACTED) yield "Lead status changed";
                yield null;
            }
            case "L-08" -> {
                Lead l = leadRepository.findById(entityId).orElse(null);
                if (l == null) yield "Lead deleted";
                if (l.getStatus() == LeadStatus.WON || l.getStatus() == LeadStatus.LOST) yield "Lead in terminal status";
                if (l.getCloseDate() != null && !l.getCloseDate().isBefore(LocalDate.now())) yield "Close date extended";
                yield null;
            }
            case "O-05" -> {
                Opportunity o = opportunityRepository.findById(entityId).orElse(null);
                if (o == null) yield "Opportunity deleted";
                if (o.getStage() == OpportunityStage.WON || o.getStage() == OpportunityStage.LOST) yield "Stage is terminal";
                yield null;
            }
            case "O-07" -> {
                Opportunity o = opportunityRepository.findById(entityId).orElse(null);
                if (o == null) yield "Opportunity deleted";
                if (o.getStage() == OpportunityStage.WON || o.getStage() == OpportunityStage.LOST) yield "Stage is terminal";
                if (o.getCloseDate() == null || o.getCloseDate().isAfter(LocalDate.now().plusDays(3))) yield "Close date no longer approaching";
                yield null;
            }
            case "O-08" -> {
                Opportunity o = opportunityRepository.findById(entityId).orElse(null);
                if (o == null) yield "Opportunity deleted";
                if (o.getStage() == OpportunityStage.WON || o.getStage() == OpportunityStage.LOST) yield "Stage is terminal";
                if (o.getCloseDate() == null || !o.getCloseDate().isBefore(LocalDate.now())) yield "Close date not yet passed";
                yield null;
            }
            case "A-02" -> {
                Contract c = contractRepository.findById(entityId).orElse(null);
                if (c == null) yield "Contract deleted";
                if (c.getStatus() != ContractStatus.ACTIVE) yield "Contract no longer active";
                if (c.getEndDate() == null || c.getEndDate().isAfter(LocalDate.now().plusDays(30))) yield "Expiry date extended";
                yield null;
            }
            case "A-03" -> {
                Contract c = contractRepository.findById(entityId).orElse(null);
                if (c == null) yield "Contract deleted";
                if (c.getStatus() != ContractStatus.ACTIVE) yield "Contract already updated";
                if (c.getEndDate() == null || !c.getEndDate().isBefore(LocalDate.now())) yield "Contract not yet expired";
                yield null;
            }
            case "T-02", "T-03", "T-05" -> {
                Activity a = activityRepository.findById(entityId).orElse(null);
                if (a == null) yield "Activity deleted";
                if (a.getStatus() == ActivityStatus.RESOLVED || a.getStatus() == ActivityStatus.CLOSED) yield "Activity already resolved";
                yield null;
            }
            case "A-01" -> {
                Account acc = accountRepository.findById(entityId).orElse(null);
                if (acc == null) yield "Account deleted";
                yield null; // dormancy re-check is complex; trust the trigger
            }
            default -> null; // unknown workflow key — proceed
        };
    }

    private void fireAlert(ScheduledTask task) {
        if (task.getRecipient() == null) return;
        NotificationConfig config = configRepository
                .findByTopicKeyAndWorkspaceIdIsNull(task.getWorkflowKey())
                .orElse(null);
        if (config != null && !config.isEnabled()) return;

        String title = buildTitle(task);
        String body = buildBody(task);
        String deepLink = buildDeepLink(task.getTargetEntityType(), task.getTargetEntityId());
        AlertImportance importance = task.getPriority() != null ? task.getPriority() : AlertImportance.NORMAL;

        alertService.sendAlertFull(
                task.getRecipient().getUsername(),
                task.getWorkflowKey(),
                title, body, importance,
                60,
                task.getTargetEntityType(), task.getTargetEntityId(),
                deepLink
        );
    }

    private String buildTitle(ScheduledTask task) {
        return switch (task.getWorkflowKey()) {
            case "L-02" -> "Unassigned lead needs attention";
            case "L-03" -> "Lead stagnant: no progress in " + getDays(task.getWorkflowKey()) + " days";
            case "L-08" -> "Lead close date has passed";
            case "O-05" -> "Deal stagnant: no update in " + getDays(task.getWorkflowKey()) + " days";
            case "O-07" -> "Opportunity closing soon";
            case "O-08" -> "Opportunity close date has passed";
            case "A-01" -> "Dormant account — no activity in 90 days";
            case "A-02" -> "Contract expiring within 30 days";
            case "A-03" -> "Contract has expired";
            case "T-02" -> "Activity due tomorrow";
            case "T-03" -> "Overdue activity requires action";
            case "T-05" -> "Meeting starting soon";
            default -> task.getWorkflowName();
        };
    }

    private String buildBody(ScheduledTask task) {
        return task.getWorkflowName() + " — " + task.getTargetEntityType() + " #" + task.getTargetEntityId();
    }

    private String buildDeepLink(String entityType, Long entityId) {
        return switch (entityType) {
            case "LEAD" -> "/leads/" + entityId;
            case "OPPORTUNITY" -> "/opportunities/" + entityId;
            case "ACCOUNT" -> "/accounts/" + entityId;
            case "ACTIVITY" -> "/activities/" + entityId;
            case "CONTRACT" -> "/contracts/" + entityId;
            case "QUOTE" -> "/quotes/" + entityId;
            case "SALES_ORDER" -> "/sales-orders/" + entityId;
            case "CONTACT" -> "/contacts/" + entityId;
            default -> "/" + entityType.toLowerCase() + "s/" + entityId;
        };
    }

    private int getDays(String key) {
        return configRepository.findByTopicKeyAndWorkspaceIdIsNull(key)
                .map(c -> c.getStagnantDays() != null ? c.getStagnantDays() : 14)
                .orElse(14);
    }

    // ── Task creation helper (used by NotificationTriggerService) ───────────

    public boolean existsPendingTask(String workflowKey, String entityType, Long entityId) {
        return taskRepository.existsByWorkflowKeyAndTargetEntityTypeAndTargetEntityIdAndStatusIn(
                workflowKey, entityType, entityId, List.of(TaskStatus.PENDING, TaskStatus.PROCESSING));
    }

    public ScheduledTask createTask(String workflowKey, String workflowName,
                                     String entityType, Long entityId,
                                     User recipient, AlertImportance priority,
                                     String cancelIfField, String cancelIfValue) {
        if (existsPendingTask(workflowKey, entityType, entityId)) return null;
        ScheduledTask task = new ScheduledTask();
        task.setWorkflowKey(workflowKey);
        task.setWorkflowName(workflowName);
        task.setTargetEntityType(entityType);
        task.setTargetEntityId(entityId);
        task.setRecipient(recipient);
        task.setPriority(priority);
        task.setScheduledAt(LocalDateTime.now());
        task.setCancelIfField(cancelIfField);
        task.setCancelIfValue(cancelIfValue);
        return taskRepository.save(task);
    }

    public ScheduledTaskResponse createTaskForUser(String workflowKey, String workflowName,
                                                    String entityType, Long entityId,
                                                    Long recipientUserId, AlertImportance priority) {
        User recipient = recipientUserId != null
                ? userRepository.findById(recipientUserId).orElse(null)
                : null;
        ScheduledTask task = createTask(workflowKey, workflowName, entityType, entityId,
                recipient, priority, null, null);
        if (task == null) throw new IllegalStateException(
                "A pending task for this workflow/entity already exists.");
        return ScheduledTaskResponse.from(task);
    }

    private ScheduledTask getOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("ScheduledTask not found: " + id));
    }
}
