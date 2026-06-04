package com.crm.ui;

import com.crm.domain.enums.AlertImportance;
import com.crm.domain.enums.TaskStatus;
import com.crm.dto.response.ScheduledTaskResponse;
import com.crm.dto.response.TopicResponse;
import com.crm.dto.response.UserSummaryResponse;
import com.crm.service.ScheduledTaskService;
import com.crm.service.TopicService;
import com.crm.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "scheduled-tasks", layout = MainLayout.class)
@PageTitle("Task Queue | CRM")
@RolesAllowed("ADMIN")
public class ScheduledTasksView extends VerticalLayout {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final ScheduledTaskService taskService;
    private final TopicService topicService;
    private final UserService userService;
    private final Grid<ScheduledTaskResponse> grid = new Grid<>(ScheduledTaskResponse.class, false);
    private final ComboBox<TaskStatus> statusFilter = new ComboBox<>("Status");

    public ScheduledTasksView(ScheduledTaskService taskService,
                              TopicService topicService,
                              UserService userService) {
        this.taskService = taskService;
        this.topicService = topicService;
        this.userService = userService;
        setSizeFull();
        setPadding(true);

        configureGrid();

        statusFilter.setItems(TaskStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.setPlaceholder("All statuses");
        statusFilter.addValueChangeListener(e -> refresh());

        Button refreshBtn = new Button("Refresh", e -> refresh());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button createBtn = new Button("Create Task", e -> openCreateTaskDialog());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Stats row
        HorizontalLayout stats = buildStatsRow();

        add(stats, new HorizontalLayout(statusFilter, refreshBtn, createBtn), grid);
        refresh();
    }

    private HorizontalLayout buildStatsRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();

        row.add(statBadge("Pending",    taskService.countByStatus(TaskStatus.PENDING),    "badge primary"));
        row.add(statBadge("Failed",     taskService.countByStatus(TaskStatus.FAILED),     "badge error"));
        row.add(statBadge("Suspended",  taskService.countByStatus(TaskStatus.SUSPENDED),  "badge contrast"));
        row.add(statBadge("Done Today", taskService.countCompletedToday(),                "badge success"));
        return row;
    }

    private Span statBadge(String label, long count, String theme) {
        Span badge = new Span(label + ": " + count);
        badge.getElement().getThemeList().add(theme);
        badge.getStyle().set("margin-right", "8px");
        return badge;
    }

    private void configureGrid() {
        grid.addColumn(t -> t.workflowKey() + " — " + t.workflowName())
                .setHeader("Workflow").setFlexGrow(2);
        grid.addColumn(t -> t.targetEntityType() + " #" + t.targetEntityId())
                .setHeader("Target");
        grid.addColumn(ScheduledTaskResponse::recipientUsername).setHeader("Recipient");
        grid.addComponentColumn(t -> {
            Span badge = new Span(t.status().name());
            badge.getElement().getThemeList().add(statusTheme(t.status()));
            if (t.failureReason() != null && !t.failureReason().isBlank()) {
                badge.setTitle(t.failureReason()); // browser tooltip on hover
            }
            return badge;
        }).setHeader("Status");
        grid.addComponentColumn(t -> {
            Span badge = new Span(t.priority() != null ? t.priority().name() : "NORMAL");
            badge.getElement().getThemeList().add(priorityTheme(t));
            return badge;
        }).setHeader("Priority");
        grid.addColumn(t -> t.scheduledAt() != null ? t.scheduledAt().format(FMT) : "")
                .setHeader("Scheduled At");
        grid.addColumn(t -> t.createdAt() != null ? t.createdAt().format(FMT) : "")
                .setHeader("Created At");
        grid.addColumn(t -> t.attemptCount() + "/" + t.maxAttempts()).setHeader("Tries");
        grid.addComponentColumn(this::buildActions).setHeader("Actions").setWidth("280px").setFlexGrow(0);
        grid.setWidthFull();
        grid.setMinHeight("400px");
        grid.addItemClickListener(e -> openDetailDialog(e.getItem()));
    }

    private HorizontalLayout buildActions(ScheduledTaskResponse t) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);

        Button runNow = new Button("Run Now", e -> {
            try {
                taskService.runNow(t.id());
                refresh();
                Notification.show("Task executed", 2000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        runNow.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        runNow.setEnabled(t.status() == TaskStatus.PENDING || t.status() == TaskStatus.SUSPENDED);

        if (t.status() == TaskStatus.SUSPENDED) {
            Button resume = new Button("Resume", e -> { taskService.resume(t.id()); refresh(); });
            resume.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
            row.add(runNow, resume);
        } else if (t.status() == TaskStatus.PENDING) {
            Button suspend = new Button("Suspend", e -> openSuspendDialog(t));
            suspend.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            row.add(runNow, suspend);
        } else if (t.status() == TaskStatus.FAILED) {
            Button retry = new Button("Retry", e -> { taskService.retry(t.id()); refresh(); });
            retry.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            row.add(retry);
        }

        if (t.status() != TaskStatus.COMPLETED && t.status() != TaskStatus.CANCELLED) {
            Button cancel = new Button("Cancel", e -> openCancelDialog(t));
            cancel.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            row.add(cancel);
        }

        return row;
    }

    private void openSuspendDialog(ScheduledTaskResponse t) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Suspend Task");
        TextField reasonField = new TextField("Reason");
        reasonField.setWidthFull();
        dlg.add(new VerticalLayout(reasonField));
        Button confirm = new Button("Suspend", e -> {
            taskService.suspend(t.id(), reasonField.getValue());
            refresh();
            dlg.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dlg.getFooter().add(new Button("Cancel", ev -> dlg.close()), confirm);
        dlg.open();
    }

    private void openCancelDialog(ScheduledTaskResponse t) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Cancel Task — This cannot be undone");
        TextField reasonField = new TextField("Reason");
        reasonField.setValue("Cancelled by admin");
        reasonField.setWidthFull();
        dlg.add(new VerticalLayout(reasonField));
        Button confirm = new Button("Cancel Task", e -> {
            taskService.cancel(t.id(), reasonField.getValue());
            refresh();
            dlg.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_ERROR);
        dlg.getFooter().add(new Button("Go Back", ev -> dlg.close()), confirm);
        dlg.open();
    }

    private void openCreateTaskDialog() {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Create Scheduled Task");
        dlg.setWidth("480px");

        // Topic picker — only FSD topics that have a topicKey
        List<TopicResponse> topics = topicService.findAll().stream()
                .filter(t -> t.topicKey() != null)
                .sorted((a, b) -> a.topicKey().compareTo(b.topicKey()))
                .toList();

        ComboBox<TopicResponse> topicPicker = new ComboBox<>("Workflow (Topic)");
        topicPicker.setItems(topics);
        topicPicker.setItemLabelGenerator(t -> t.topicKey() + " — " + t.name());
        topicPicker.setWidthFull();
        topicPicker.setRequired(true);

        TextField entityTypeField = new TextField("Entity Type");
        entityTypeField.setWidthFull();
        entityTypeField.setReadOnly(true);
        entityTypeField.setPlaceholder("Auto-filled from topic");

        topicPicker.addValueChangeListener(e -> {
            if (e.getValue() != null) entityTypeField.setValue(e.getValue().entityType());
            else entityTypeField.clear();
        });

        IntegerField entityIdField = new IntegerField("Entity ID");
        entityIdField.setWidthFull();
        entityIdField.setMin(1);
        entityIdField.setRequired(true);

        List<UserSummaryResponse> users = userService.findAll();
        ComboBox<UserSummaryResponse> recipientPicker = new ComboBox<>("Recipient");
        recipientPicker.setItems(users);
        recipientPicker.setItemLabelGenerator(u -> u.username() + (u.email() != null ? " — " + u.email() : ""));
        recipientPicker.setWidthFull();

        ComboBox<AlertImportance> priorityPicker = new ComboBox<>("Priority");
        priorityPicker.setItems(AlertImportance.values());
        priorityPicker.setValue(AlertImportance.NORMAL);
        priorityPicker.setWidthFull();

        VerticalLayout body = new VerticalLayout(topicPicker, entityTypeField, entityIdField,
                recipientPicker, priorityPicker);
        body.setPadding(false);
        body.setSpacing(true);
        dlg.add(body);

        Button confirm = new Button("Create", e -> {
            if (topicPicker.getValue() == null) {
                topicPicker.setInvalid(true);
                return;
            }
            if (entityIdField.getValue() == null || entityIdField.getValue() < 1) {
                entityIdField.setInvalid(true);
                entityIdField.setErrorMessage("Enter a valid entity ID");
                return;
            }
            TopicResponse topic = topicPicker.getValue();
            Long recipientId = recipientPicker.getValue() != null ? recipientPicker.getValue().id() : null;
            try {
                taskService.createTaskForUser(
                        topic.topicKey(), topic.name(),
                        topic.entityType(), entityIdField.getValue().longValue(),
                        recipientId, priorityPicker.getValue());
                refresh();
                dlg.close();
                Notification.show("Task created", 2000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalStateException ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dlg.getFooter().add(new Button("Cancel", e -> dlg.close()), confirm);
        dlg.open();
    }

    private void openDetailDialog(ScheduledTaskResponse t) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Task #" + t.id() + " — " + t.workflowKey() + " " + t.workflowName());
        dlg.setWidth("520px");

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);

        body.add(detailRow("Status",      t.status().name()));
        body.add(detailRow("Target",      t.targetEntityType() + " #" + t.targetEntityId()));
        body.add(detailRow("Recipient",   t.recipientUsername() != null ? t.recipientUsername() : "—"));
        body.add(detailRow("Priority",    t.priority() != null ? t.priority().name() : "NORMAL"));
        body.add(detailRow("Attempts",    t.attemptCount() + " / " + t.maxAttempts()));
        if (t.scheduledAt() != null)    body.add(detailRow("Scheduled At",    t.scheduledAt().format(FMT)));
        if (t.lastAttemptedAt() != null) body.add(detailRow("Last Attempt",   t.lastAttemptedAt().format(FMT)));
        if (t.completedAt() != null)    body.add(detailRow("Completed At",    t.completedAt().format(FMT)));
        if (t.failureReason() != null && !t.failureReason().isBlank()) {
            Paragraph reason = new Paragraph(t.failureReason());
            reason.getStyle()
                    .set("color", "#d32f2f")
                    .set("font-size", "13px")
                    .set("white-space", "pre-wrap")
                    .set("word-break", "break-all")
                    .set("margin", "8px 0 0 0");
            body.add(detailRow("Failure Reason", ""));
            body.add(reason);
        }
        if (t.cancelIfField() != null)  body.add(detailRow("Cancel Condition", t.cancelIfField() + " = " + t.cancelIfValue()));

        dlg.add(body);
        dlg.getFooter().add(new Button("Close", e -> dlg.close()));
        dlg.open();
    }

    private HorizontalLayout detailRow(String label, String value) {
        Span key = new Span(label + ":");
        key.getStyle().set("font-weight", "600").set("min-width", "130px").set("color", "#555");
        Span val = new Span(value);
        HorizontalLayout row = new HorizontalLayout(key, val);
        row.setSpacing(true);
        row.getStyle().set("padding", "2px 0");
        return row;
    }

    private void refresh() {
        TaskStatus selected = statusFilter.getValue();
        if (selected != null) {
            grid.setItems(taskService.findByStatus(selected,
                    org.springframework.data.domain.PageRequest.of(0, 200)).getContent());
        } else {
            grid.setItems(taskService.findAll(
                    org.springframework.data.domain.PageRequest.of(0, 200)).getContent());
        }
    }

    private String statusTheme(TaskStatus s) {
        return switch (s) {
            case PENDING    -> "badge primary";
            case PROCESSING -> "badge contrast";
            case COMPLETED  -> "badge success";
            case FAILED     -> "badge error";
            case SUSPENDED  -> "badge contrast";
            case CANCELLED  -> "badge";
            case SKIPPED    -> "badge contrast";
        };
    }

    private String priorityTheme(ScheduledTaskResponse t) {
        if (t.priority() == null) return "badge";
        return switch (t.priority()) {
            case CRITICAL, URGENT -> "badge error small";
            case HIGH             -> "badge primary small";
            case NORMAL           -> "badge small";
            default               -> "badge contrast small";
        };
    }
}
