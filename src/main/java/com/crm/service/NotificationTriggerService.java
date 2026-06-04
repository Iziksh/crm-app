package com.crm.service;

import com.crm.domain.entity.*;
import com.crm.domain.enums.*;
import com.crm.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scans the database on defined schedules and creates ScheduledTask records
 * for each matching entity. The ScheduledTaskProcessor picks them up within 60s.
 *
 * Each method maps to one FSD topic key (L-xx, O-xx, A-xx, T-xx).
 */
@Service
public class NotificationTriggerService {

    private final ScheduledTaskService taskService;
    private final NotificationConfigRepository configRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final ContractRepository contractRepository;
    private final ActivityRepository activityRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public NotificationTriggerService(ScheduledTaskService taskService,
                                       NotificationConfigRepository configRepository,
                                       LeadRepository leadRepository,
                                       OpportunityRepository opportunityRepository,
                                       ContractRepository contractRepository,
                                       ActivityRepository activityRepository,
                                       AccountRepository accountRepository,
                                       UserRepository userRepository) {
        this.taskService = taskService;
        this.configRepository = configRepository;
        this.leadRepository = leadRepository;
        this.opportunityRepository = opportunityRepository;
        this.contractRepository = contractRepository;
        this.activityRepository = activityRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    // ── L-02 Unassigned Leads — every 15 minutes ────────────────────────────
    @Scheduled(fixedDelay = 900_000)
    @Transactional
    public void checkUnassignedLeads() {
        if (!isEnabled("L-02")) return;
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(60);
        List<Lead> leads = leadRepository.findUnassigned(
                List.of(LeadStatus.WON, LeadStatus.LOST), cutoff);
        User fallback = getFirstAdmin();
        for (Lead l : leads) {
            taskService.createTask("L-02", "Lead Unassigned",
                    "LEAD", l.getId(), fallback, AlertImportance.HIGH,
                    "assignedTo", null);
        }
    }

    // ── L-03 Stagnant Leads — daily 08:00 ────────────────────────────────────
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkStagnantLeads() {
        if (!isEnabled("L-03")) return;
        int days = getDays("L-03", 5);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<Lead> leads = leadRepository.findStagnant(
                List.of(LeadStatus.NEW, LeadStatus.CONTACTED), cutoff);
        for (Lead l : leads) {
            User recipient = l.getAssignedTo();
            taskService.createTask("L-03", "Lead Status Stagnant",
                    "LEAD", l.getId(), recipient, AlertImportance.NORMAL,
                    "status", "WON,LOST");
        }
    }

    // ── L-08 Lead Close Date Passed — daily 08:00 ────────────────────────────
    @Scheduled(cron = "0 5 8 * * *")
    @Transactional
    public void checkLeadOverdueCloseDate() {
        if (!isEnabled("L-08")) return;
        List<Lead> leads = leadRepository.findOverdueCloseDate(
                LocalDate.now(), List.of(LeadStatus.WON, LeadStatus.LOST));
        for (Lead l : leads) {
            taskService.createTask("L-08", "Lead Close Date Passed",
                    "LEAD", l.getId(), l.getAssignedTo(), AlertImportance.HIGH,
                    "status", "WON,LOST");
        }
    }

    // ── O-05 Stagnant Deals — daily 08:00 ────────────────────────────────────
    @Scheduled(cron = "0 10 8 * * *")
    @Transactional
    public void checkStagnantDeals() {
        if (!isEnabled("O-05")) return;
        int days = getDays("O-05", 14);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<Opportunity> opps = opportunityRepository.findStagnant(
                List.of(OpportunityStage.WON, OpportunityStage.LOST), cutoff);
        for (Opportunity o : opps) {
            taskService.createTask("O-05", "Stagnant Deal",
                    "OPPORTUNITY", o.getId(), o.getAssignedTo(), AlertImportance.HIGH,
                    "stage", "WON,LOST");
        }
    }

    // ── O-07 Close Date Approaching — daily 07:00 ────────────────────────────
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void checkApproachingCloseDates() {
        if (!isEnabled("O-07")) return;
        int days = getDays("O-07", 3);
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(days);
        List<Opportunity> opps = opportunityRepository.findApproachingCloseDate(
                from, to, List.of(OpportunityStage.WON, OpportunityStage.LOST));
        for (Opportunity o : opps) {
            taskService.createTask("O-07", "Close Date Approaching",
                    "OPPORTUNITY", o.getId(), o.getAssignedTo(), AlertImportance.HIGH,
                    "stage", "WON,LOST");
        }
    }

    // ── O-08 Close Date Passed — daily 08:00 ─────────────────────────────────
    @Scheduled(cron = "0 15 8 * * *")
    @Transactional
    public void checkOverdueCloseDates() {
        if (!isEnabled("O-08")) return;
        List<Opportunity> opps = opportunityRepository.findOverdueCloseDate(
                LocalDate.now(), List.of(OpportunityStage.WON, OpportunityStage.LOST));
        for (Opportunity o : opps) {
            taskService.createTask("O-08", "Close Date Passed",
                    "OPPORTUNITY", o.getId(), o.getAssignedTo(), AlertImportance.URGENT,
                    "stage", "WON,LOST");
        }
    }

    // ── A-01 Dormant Accounts — every Monday 08:00 ───────────────────────────
    @Scheduled(cron = "0 0 8 * * MON")
    @Transactional
    public void checkDormantAccounts() {
        if (!isEnabled("A-01")) return;
        int days = configRepository.findByTopicKeyAndWorkspaceIdIsNull("A-01")
                .map(c -> c.getDormancyDays() != null ? c.getDormancyDays() : 90).orElse(90);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        User admin = getFirstAdmin();
        List<Account> accounts = accountRepository.findDormant(cutoff);
        for (Account a : accounts) {
            taskService.createTask("A-01", "Dormant Account",
                    "ACCOUNT", a.getId(), admin, AlertImportance.NORMAL,
                    null, null);
        }
    }

    // ── A-02 Contract Expiring Soon — daily 07:00 ────────────────────────────
    @Scheduled(cron = "0 5 7 * * *")
    @Transactional
    public void checkExpiringContracts() {
        if (!isEnabled("A-02")) return;
        int days = configRepository.findByTopicKeyAndWorkspaceIdIsNull("A-02")
                .map(c -> c.getExpiryWarningDays() != null ? c.getExpiryWarningDays() : 30).orElse(30);
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(days);
        List<Contract> contracts = contractRepository.findExpiringBetween(
                ContractStatus.ACTIVE, from, to);
        for (Contract c : contracts) {
            taskService.createTask("A-02", "Contract Expiring Soon",
                    "CONTRACT", c.getId(), c.getAssignedTo(), AlertImportance.HIGH,
                    "status", "EXPIRED,TERMINATED");
        }
    }

    // ── A-03 Contract Expired — daily 08:00 ──────────────────────────────────
    @Scheduled(cron = "0 20 8 * * *")
    @Transactional
    public void checkExpiredContracts() {
        if (!isEnabled("A-03")) return;
        List<Contract> contracts = contractRepository.findExpiredActive(
                ContractStatus.ACTIVE, LocalDate.now());
        User admin = getFirstAdmin();
        for (Contract c : contracts) {
            User recipient = c.getAssignedTo() != null ? c.getAssignedTo() : admin;
            taskService.createTask("A-03", "Contract Expired",
                    "CONTRACT", c.getId(), recipient, AlertImportance.URGENT,
                    "status", "EXPIRED,TERMINATED");
        }
    }

    // ── T-02 Activity Due Tomorrow — daily 18:00 ─────────────────────────────
    @Scheduled(cron = "0 0 18 * * *")
    @Transactional
    public void checkActivitiesDueTomorrow() {
        if (!isEnabled("T-02")) return;
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Activity> activities = activityRepository.findDueOn(
                tomorrow, List.of(ActivityStatus.RESOLVED, ActivityStatus.CLOSED));
        for (Activity a : activities) {
            taskService.createTask("T-02", "Activity Due Tomorrow",
                    "ACTIVITY", a.getId(), a.getAssignedTo(), AlertImportance.NORMAL,
                    "status", "RESOLVED,CLOSED");
        }
    }

    // ── T-03 Activity Overdue — daily 08:00 ──────────────────────────────────
    @Scheduled(cron = "0 25 8 * * *")
    @Transactional
    public void checkOverdueActivities() {
        if (!isEnabled("T-03")) return;
        List<Activity> activities = activityRepository.findOverdue(
                LocalDate.now(), List.of(ActivityStatus.RESOLVED, ActivityStatus.CLOSED));
        for (Activity a : activities) {
            taskService.createTask("T-03", "Activity Overdue",
                    "ACTIVITY", a.getId(), a.getAssignedTo(), AlertImportance.HIGH,
                    "status", "RESOLVED,CLOSED");
        }
    }

    // ── T-05 Meeting in 1 Hour — every 5 minutes ─────────────────────────────
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void checkUpcomingMeetings() {
        if (!isEnabled("T-05")) return;
        // Activities of type MEETING due today (coarse check; processor refines)
        List<Activity> meetings = activityRepository.findMeetingsOn(
                ActivityType.MEETING, LocalDate.now(),
                List.of(ActivityStatus.RESOLVED, ActivityStatus.CLOSED));
        for (Activity a : meetings) {
            taskService.createTask("T-05", "Meeting in 1 Hour",
                    "ACTIVITY", a.getId(), a.getAssignedTo(), AlertImportance.HIGH,
                    "status", "RESOLVED,CLOSED");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isEnabled(String topicKey) {
        return configRepository.findByTopicKeyAndWorkspaceIdIsNull(topicKey)
                .map(NotificationConfig::isEnabled).orElse(true);
    }

    private int getDays(String topicKey, int defaultValue) {
        return configRepository.findByTopicKeyAndWorkspaceIdIsNull(topicKey)
                .map(c -> c.getStagnantDays() != null ? c.getStagnantDays() : defaultValue)
                .orElse(defaultValue);
    }

    private User getFirstAdmin() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains("ROLE_ADMIN"))
                .findFirst().orElse(null);
    }
}
