package com.crm.config;

import com.crm.domain.entity.NotificationConfig;
import com.crm.domain.entity.Topic;
import com.crm.domain.entity.User;
import com.crm.domain.entity.Workspace;
import com.crm.repository.NotificationConfigRepository;
import com.crm.repository.TopicRepository;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TopicRepository topicRepository;
    private final NotificationConfigRepository configRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String admin2Email;

    public DataInitializer(UserRepository userRepository,
                           WorkspaceRepository workspaceRepository,
                           TopicRepository topicRepository,
                           NotificationConfigRepository configRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.email:admin@crm.com}") String adminEmail,
                           @Value("${app.admin2.email:vladik@crm.internal}") String admin2Email) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.topicRepository = topicRepository;
        this.configRepository = configRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.admin2Email = admin2Email;
    }

    @Override
    public void run(ApplicationArguments args) {
        User admin;
        if (!userRepository.existsByUsername("admin")) {
            admin = new User();
            admin.setUsername("admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(Set.of("ROLE_USER", "ROLE_ADMIN"));
            admin = userRepository.save(admin);
        } else {
            admin = userRepository.findByUsername("admin").orElseThrow();
            // Keep email in sync with app.admin.email
            if (!adminEmail.equals(admin.getEmail())) {
                // If another user owns this email, move them to a placeholder first
                userRepository.findByEmail(adminEmail).ifPresent(other -> {
                    if (!other.getUsername().equals("admin")) {
                        other.setEmail(other.getUsername() + "@crm.internal");
                        userRepository.save(other);
                    }
                });
                admin.setEmail(adminEmail);
                admin = userRepository.save(admin);
            }
        }

        if (!userRepository.existsByUsername("VladiK")) {
            User vladik = new User();
            vladik.setUsername("VladiK");
            vladik.setEmail(admin2Email);
            vladik.setPassword(passwordEncoder.encode("admin123"));
            vladik.setRoles(Set.of("ROLE_USER", "ROLE_ADMIN"));
            userRepository.save(vladik);
        }

        if (workspaceRepository.count() == 0) {
            Workspace defaultWs = new Workspace();
            defaultWs.setName("Default");
            defaultWs.setDescription("Default workspace");
            defaultWs.setCreatedBy(admin);
            defaultWs.getMembers().add(admin);
            workspaceRepository.save(defaultWs);
        }

        seedTopics();
        seedNotificationConfigs();
    }

    private void seedTopics() {
        // FSD §1 — all 32 topics (topicKey, name, entityType, sendMail)
        List<String[]> topics = List.of(
            // Lead topics
            new String[]{"L-01", "New Lead Arrived",               "LEAD",         "false"},
            new String[]{"L-02", "Lead Unassigned",                "LEAD",         "false"},
            new String[]{"L-03", "Lead Status Stagnant",           "LEAD",         "false"},
            new String[]{"L-04", "Lead Assigned to Me",            "LEAD",         "false"},
            new String[]{"L-05", "Lead Converted",                 "LEAD",         "false"},
            new String[]{"L-06", "Lead Marked Lost",               "LEAD",         "false"},
            new String[]{"L-07", "High-Value Lead",                "LEAD",         "false"},
            new String[]{"L-08", "Lead Close Date Passed",         "LEAD",         "false"},
            // Opportunity topics
            new String[]{"O-01", "Opportunity Created",            "OPPORTUNITY",  "false"},
            new String[]{"O-02", "Opportunity Stage Advanced",     "OPPORTUNITY",  "false"},
            new String[]{"O-03", "Opportunity Won",                "OPPORTUNITY",  "false"},
            new String[]{"O-04", "Opportunity Lost",               "OPPORTUNITY",  "false"},
            new String[]{"O-05", "Stagnant Deal",                  "OPPORTUNITY",  "false"},
            new String[]{"O-06", "High-Value Opportunity",         "OPPORTUNITY",  "false"},
            new String[]{"O-07", "Close Date Approaching",         "OPPORTUNITY",  "false"},
            new String[]{"O-08", "Close Date Passed",              "OPPORTUNITY",  "false"},
            new String[]{"O-09", "Quote Generated",                "QUOTE",        "false"},
            new String[]{"O-10", "Quote Won",                      "QUOTE",        "false"},
            // Account topics
            new String[]{"A-01", "Dormant Account",                "ACCOUNT",      "false"},
            new String[]{"A-02", "Contract Expiring Soon",         "CONTRACT",     "false"},
            new String[]{"A-03", "Contract Expired",               "CONTRACT",     "false"},
            new String[]{"A-04", "Contract Activated",             "CONTRACT",     "false"},
            new String[]{"A-05", "Account Modified",               "ACCOUNT",      "false"},
            new String[]{"A-06", "New Contact Added",              "CONTACT",      "false"},
            new String[]{"A-07", "Sales Order Delivered",          "SALES_ORDER",  "false"},
            // Team / Task topics
            new String[]{"T-01", "Activity Assigned",              "ACTIVITY",     "false"},
            new String[]{"T-02", "Activity Due Tomorrow",          "ACTIVITY",     "false"},
            new String[]{"T-03", "Activity Overdue",               "ACTIVITY",     "false"},
            new String[]{"T-04", "Activity Resolved",              "ACTIVITY",     "false"},
            new String[]{"T-05", "Meeting in 1 Hour",              "ACTIVITY",     "false"},
            new String[]{"T-06", "Note Added",                     "ACTIVITY_NOTE","false"},
            new String[]{"T-07", "Attachment Uploaded",            "ATTACHMENT",   "false"},
            // OpenCRX legacy topics (kept for Subscription system)
            new String[]{null,   "Account Modifications",          "ACCOUNT",      "false"},
            new String[]{null,   "Activity Modifications",         "ACTIVITY",     "false"},
            new String[]{null,   "Activity Follow Up Modifications","ACTIVITY_NOTE","false"},
            new String[]{null,   "Lead Modifications",             "LEAD",         "false"},
            new String[]{null,   "Opportunity Modifications",      "OPPORTUNITY",  "false"},
            new String[]{null,   "Quote Modifications",            "QUOTE",        "false"},
            new String[]{null,   "Sales Order Modifications",      "SALES_ORDER",  "false"},
            new String[]{null,   "Contract Modifications",         "CONTRACT",     "false"},
            new String[]{null,   "Product Modifications",          "PRODUCT",      "false"},
            new String[]{null,   "Contact Modifications",          "CONTACT",      "false"},
            new String[]{null,   "Alert Modifications",            "ALERT",        "true"}
        );

        for (String[] row : topics) {
            String topicKey  = row[0];
            String name      = row[1];
            String entity    = row[2];
            boolean sendMail = Boolean.parseBoolean(row[3]);

            if (!topicRepository.existsByName(name)) {
                Topic t = new Topic();
                t.setTopicKey(topicKey);
                t.setName(name);
                t.setEntityType(entity);
                t.setSendMailEnabled(sendMail);
                topicRepository.save(t);
            } else if (topicKey != null) {
                // Update existing topic with topicKey if not yet set
                topicRepository.findByName(name).ifPresent(existing -> {
                    if (existing.getTopicKey() == null) {
                        existing.setTopicKey(topicKey);
                        topicRepository.save(existing);
                    }
                });
            }
        }
    }

    private void seedNotificationConfigs() {
        // Default global configs per FSD §4.5 thresholds
        List<Object[]> configs = List.of(
            // {topicKey, stagnantDays, highValueThreshold, expiryWarningDays, dormancyDays, enabled}
            new Object[]{"L-02", null,  null,                          null, null, true},
            new Object[]{"L-03", 5,     null,                          null, null, true},
            new Object[]{"L-07", null,  new BigDecimal("50000"),       null, null, true},
            new Object[]{"L-08", null,  null,                          null, null, true},
            new Object[]{"O-05", 14,    null,                          null, null, true},
            new Object[]{"O-06", null,  new BigDecimal("100000"),      null, null, true},
            new Object[]{"O-07", 3,     null,                          null, null, true},
            new Object[]{"O-08", null,  null,                          null, null, true},
            new Object[]{"A-01", null,  null,                          null, 90,   true},
            new Object[]{"A-02", null,  null,                          30,   null, true},
            new Object[]{"A-03", null,  null,                          null, null, true},
            new Object[]{"T-02", null,  null,                          null, null, true},
            new Object[]{"T-03", null,  null,                          null, null, true},
            new Object[]{"T-05", null,  null,                          null, null, true}
        );

        for (Object[] row : configs) {
            String topicKey = (String) row[0];
            if (!configRepository.existsByTopicKeyAndWorkspaceIdIsNull(topicKey)) {
                NotificationConfig cfg = new NotificationConfig();
                cfg.setTopicKey(topicKey);
                cfg.setWorkspaceId(null);
                cfg.setEnabled((Boolean) row[5]);
                cfg.setStagnantDays((Integer) row[1]);
                cfg.setHighValueThreshold((BigDecimal) row[2]);
                cfg.setExpiryWarningDays((Integer) row[3]);
                cfg.setDormancyDays((Integer) row[4]);
                configRepository.save(cfg);
            }
        }
    }
}
