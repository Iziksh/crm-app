package com.crm.ui;

import com.crm.domain.enums.AlertImportance;
import com.crm.domain.enums.TaskStatus;
import com.crm.dto.response.ScheduledTaskResponse;
import com.crm.dto.response.TopicResponse;
import com.crm.dto.response.UserSummaryResponse;
import com.crm.service.ScheduledTaskService;
import com.crm.service.TopicService;
import com.crm.service.TranslationService;
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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "scheduled-tasks", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class ScheduledTasksView extends VerticalLayout implements HasDynamicTitle {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final TranslationService i18n;
    private final ScheduledTaskService taskService;
    private final TopicService topicService;
    private final UserService userService;
    private final Grid<ScheduledTaskResponse> grid = new Grid<>(ScheduledTaskResponse.class, false);
    private final ComboBox<TaskStatus> statusFilter = new ComboBox<>();

    public ScheduledTasksView(ScheduledTaskService taskService,
                              TopicService topicService,
                              UserService userService,
                              TranslationService i18n) {
        this.taskService = taskService;
        this.topicService = topicService;
        this.userService = userService;
        this.i18n = i18n;
        setSizeFull();
        setPadding(true);

        configureGrid();

        statusFilter.setLabel(i18n.translate("common.status"));
        statusFilter.setItems(TaskStatus.values());
        statusFilter.setItemLabelGenerator(i18n::translateEnum);
        statusFilter.setClearButtonVisible(true);
        statusFilter.setPlaceholder(i18n.translate("view.scheduledTasks.filter.allStatuses"));
        statusFilter.addValueChangeListener(e -> refresh());

        Button refreshBtn = new Button(i18n.translate("common.refresh"), e -> refresh());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button createBtn = new Button(i18n.translate("view.scheduledTasks.button.createTask"),
                e -> openCreateTaskDialog());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout stats = buildStatsRow();

        add(stats, new HorizontalLayout(statusFilter, refreshBtn, createBtn), grid);
        refresh();
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("page.scheduledTasks");
    }

    private HorizontalLayout buildStatsRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();

        row.add(statBadge(i18n.translate("view.scheduledTasks.stat.pending"),
                taskService.countByStatus(TaskStatus.PENDING), "badge primary"));
        row.add(statBadge(i18n.translate("view.scheduledTasks.stat.failed"),
                taskService.countByStatus(TaskStatus.FAILED), "badge error"));
        row.add(statBadge(i18n.translate("view.scheduledTasks.stat.suspended"),
                taskService.countByStatus(TaskStatus.SUSPENDED), "badge contrast"));
        row.add(statBadge(i18n.translate("view.scheduledTasks.stat.doneToday"),
                taskService.countCompletedToday(), "badge success"));
        return row;
    }

    private Span statBadge(String label, long count, String theme) {
        Span badge = new Span(i18n.translate("view.scheduledTasks.stat.label", label, count));
        badge.getElement().getThemeList().add(theme);
        badge.getStyle().set("margin-inline-end", "8px");
        return badge;
    }

    private void configureGrid() {
        grid.addColumn(t -> t.workflowKey() + " — " + t.workflowName())
                .setHeader(i18n.translate("common.workflow")).setFlexGrow(2);
        grid.addColumn(t -> i18n.translate("view.scheduledTasks.target.format",
                t.targetEntityType(), t.targetEntityId()))
                .setHeader(i18n.translate("common.target"));
        grid.addColumn(ScheduledTaskResponse::recipientUsername).setHeader(i18n.translate("common.recipient"));
        grid.addComponentColumn(t -> {
            Span badge = new Span(i18n.translateEnum(t.status()));
            badge.getElement().getThemeList().add(statusTheme(t.status()));
            if (t.failureReason() != null && !t.failureReason().isBlank()) {
                badge.setTitle(t.failureReason());
            }
            return badge;
        }).setHeader(i18n.translate("common.status"));
        grid.addComponentColumn(t -> {
            AlertImportance priority = t.priority() != null ? t.priority() : AlertImportance.NORMAL;
            Span badge = new Span(i18n.translateEnum(priority));
            badge.getElement().getThemeList().add(priorityTheme(t));
            return badge;
        }).setHeader(i18n.translate("common.priority"));
        grid.addColumn(t -> t.scheduledAt() != null ? t.scheduledAt().format(FMT) : "")
                .setHeader(i18n.translate("common.scheduledAt"));
        grid.addColumn(t -> t.createdAt() != null ? t.createdAt().format(FMT) : "")
                .setHeader(i18n.translate("common.createdAt"));
        grid.addColumn(t -> t.attemptCount() + "/" + t.maxAttempts()).setHeader(i18n.translate("common.tries"));
        grid.addComponentColumn(this::buildActions).setHeader(i18n.translate("common.actions"))
                .setWidth("280px").setFlexGrow(0);
        grid.setWidthFull();
        grid.setMinHeight("400px");
        grid.addItemClickListener(e -> openDetailDialog(e.getItem()));
    }

    private HorizontalLayout buildActions(ScheduledTaskResponse t) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);

        Button runNow = new Button(i18n.translate("view.scheduledTasks.button.runNow"), e -> {
            try {
                taskService.runNow(t.id());
                refresh();
                Notification.show(i18n.translate("view.scheduledTasks.notification.executed"),
                        2000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show(i18n.translate("notification.errorPrefix", ex.getMessage()),
                        3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        runNow.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        runNow.setEnabled(t.status() == TaskStatus.PENDING || t.status() == TaskStatus.SUSPENDED);

        if (t.status() == TaskStatus.SUSPENDED) {
            Button resume = new Button(i18n.translate("view.scheduledTasks.button.resume"),
                    e -> { taskService.resume(t.id()); refresh(); });
            resume.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
            row.add(runNow, resume);
        } else if (t.status() == TaskStatus.PENDING) {
            Button suspend = new Button(i18n.translate("view.scheduledTasks.button.suspend"),
                    e -> openSuspendDialog(t));
            suspend.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            row.add(runNow, suspend);
        } else if (t.status() == TaskStatus.FAILED) {
            Button retry = new Button(i18n.translate("view.scheduledTasks.button.retry"),
                    e -> { taskService.retry(t.id()); refresh(); });
            retry.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            row.add(retry);
        }

        if (t.status() != TaskStatus.COMPLETED && t.status() != TaskStatus.CANCELLED) {
            Button cancel = new Button(i18n.translate("common.cancel"), e -> openCancelDialog(t));
            cancel.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            row.add(cancel);
        }

        return row;
    }

    private void openSuspendDialog(ScheduledTaskResponse t) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle(i18n.translate("view.scheduledTasks.dialog.suspendHeader"));
        TextField reasonField = new TextField(i18n.translate("common.reason"));
        reasonField.setWidthFull();
        dlg.add(new VerticalLayout(reasonField));
        Button confirm = new Button(i18n.translate("view.scheduledTasks.dialog.suspendConfirm"), e -> {
            taskService.suspend(t.id(), reasonField.getValue());
            refresh();
            dlg.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dlg.getFooter().add(new Button(i18n.translate("common.cancel"), ev -> dlg.close()), confirm);
        dlg.open();
    }

    private void openCancelDialog(ScheduledTaskResponse t) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle(i18n.translate("view.scheduledTasks.dialog.cancelHeader"));
        TextField reasonField = new TextField(i18n.translate("common.reason"));
        reasonField.setValue(i18n.translate("view.scheduledTasks.dialog.cancelDefaultReason"));
        reasonField.setWidthFull();
        dlg.add(new VerticalLayout(reasonField));
        Button confirm = new Button(i18n.translate("view.scheduledTasks.button.cancelTask"), e -> {
            taskService.cancel(t.id(), reasonField.getValue());
            refresh();
            dlg.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_ERROR);
        dlg.getFooter().add(new Button(i18n.translate("dialog.goBack"), ev -> dlg.close()), confirm);
        dlg.open();
    }

    private void openCreateTaskDialog() {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle(i18n.translate("view.scheduledTasks.dialog.createHeader"));
        dlg.setWidth("480px");

        List<TopicResponse> topics = topicService.findAll().stream()
                .filter(t -> t.topicKey() != null)
                .sorted((a, b) -> a.topicKey().compareTo(b.topicKey()))
                .toList();

        ComboBox<TopicResponse> topicPicker = new ComboBox<>(i18n.translate("view.scheduledTasks.field.workflowTopic"));
        topicPicker.setItems(topics);
        topicPicker.setItemLabelGenerator(t -> t.topicKey() + " — " + t.name());
        topicPicker.setWidthFull();
        topicPicker.setRequired(true);

        TextField entityTypeField = new TextField(i18n.translate("common.entityType"));
        entityTypeField.setWidthFull();
        entityTypeField.setReadOnly(true);
        entityTypeField.setPlaceholder(i18n.translate("view.scheduledTasks.field.entityTypePlaceholder"));

        topicPicker.addValueChangeListener(e -> {
            if (e.getValue() != null) entityTypeField.setValue(e.getValue().entityType());
            else entityTypeField.clear();
        });

        IntegerField entityIdField = new IntegerField(i18n.translate("common.entityId"));
        entityIdField.setWidthFull();
        entityIdField.setMin(1);
        entityIdField.setRequired(true);

        List<UserSummaryResponse> users = userService.findAll();
        ComboBox<UserSummaryResponse> recipientPicker = new ComboBox<>(i18n.translate("common.recipient"));
        recipientPicker.setItems(users);
        recipientPicker.setItemLabelGenerator(u -> u.username() + (u.email() != null ? " — " + u.email() : ""));
        recipientPicker.setWidthFull();

        ComboBox<AlertImportance> priorityPicker = new ComboBox<>(i18n.translate("common.priority"));
        priorityPicker.setItems(AlertImportance.values());
        priorityPicker.setItemLabelGenerator(i18n::translateEnum);
        priorityPicker.setValue(AlertImportance.NORMAL);
        priorityPicker.setWidthFull();

        VerticalLayout body = new VerticalLayout(topicPicker, entityTypeField, entityIdField,
                recipientPicker, priorityPicker);
        body.setPadding(false);
        body.setSpacing(true);
        dlg.add(body);

        Button confirm = new Button(i18n.translate("view.scheduledTasks.button.create"), e -> {
            if (topicPicker.getValue() == null) {
                topicPicker.setInvalid(true);
                return;
            }
            if (entityIdField.getValue() == null || entityIdField.getValue() < 1) {
                entityIdField.setInvalid(true);
                entityIdField.setErrorMessage(i18n.translate("view.scheduledTasks.validation.entityIdRequired"));
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
                Notification.show(i18n.translate("view.scheduledTasks.notification.created"),
                        2000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalStateException ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dlg.getFooter().add(new Button(i18n.translate("common.cancel"), e -> dlg.close()), confirm);
        dlg.open();
    }

    private void openDetailDialog(ScheduledTaskResponse t) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle(i18n.translate("view.scheduledTasks.dialog.detailHeader",
                t.id(), t.workflowKey(), t.workflowName()));
        dlg.setWidth("520px");

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);

        body.add(detailRow(i18n.translate("common.status"), i18n.translateEnum(t.status())));
        body.add(detailRow(i18n.translate("common.target"),
                i18n.translate("view.scheduledTasks.target.format", t.targetEntityType(), t.targetEntityId())));
        body.add(detailRow(i18n.translate("common.recipient"),
                t.recipientUsername() != null ? t.recipientUsername() : i18n.translate("common.emDash")));
        body.add(detailRow(i18n.translate("common.priority"),
                i18n.translateEnum(t.priority() != null ? t.priority() : AlertImportance.NORMAL)));
        body.add(detailRow(i18n.translate("common.attempts"),
                t.attemptCount() + " / " + t.maxAttempts()));
        if (t.scheduledAt() != null) {
            body.add(detailRow(i18n.translate("common.scheduledAt"), t.scheduledAt().format(FMT)));
        }
        if (t.lastAttemptedAt() != null) {
            body.add(detailRow(i18n.translate("common.lastAttempt"), t.lastAttemptedAt().format(FMT)));
        }
        if (t.completedAt() != null) {
            body.add(detailRow(i18n.translate("common.completedAt"), t.completedAt().format(FMT)));
        }
        if (t.failureReason() != null && !t.failureReason().isBlank()) {
            Paragraph reason = new Paragraph(t.failureReason());
            reason.getStyle()
                    .set("color", "#d32f2f")
                    .set("font-size", "13px")
                    .set("white-space", "pre-wrap")
                    .set("word-break", "break-all")
                    .set("margin", "8px 0 0 0");
            body.add(detailRow(i18n.translate("common.failureReason"), ""));
            body.add(reason);
        }
        if (t.cancelIfField() != null) {
            body.add(detailRow(i18n.translate("common.cancelCondition"),
                    i18n.translate("view.scheduledTasks.detail.cancelConditionFormat",
                            t.cancelIfField(), t.cancelIfValue())));
        }

        dlg.add(body);
        dlg.getFooter().add(new Button(i18n.translate("common.close"), e -> dlg.close()));
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
