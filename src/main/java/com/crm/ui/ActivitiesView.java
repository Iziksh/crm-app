package com.crm.ui;

import com.crm.domain.enums.ActivityPriority;
import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ActivityType;
import com.crm.dto.request.ActivityNoteRequest;
import com.crm.dto.request.ActivityRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.ActivityNoteResponse;
import com.crm.dto.response.ActivityResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.service.AccountService;
import com.crm.service.ActivityService;
import com.crm.service.AttachmentService;
import com.crm.service.ContactService;
import com.crm.service.TranslationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Route(value = "activities", layout = MainLayout.class)
@RolesAllowed({"SUPPORT", "ADMIN"})
public class ActivitiesView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final ActivityService activityService;
    private final AccountService accountService;
    private final ContactService contactService;
    private final SecurityService securityService;
    private final AttachmentService attachmentService;

    private final Grid<ActivityResponse> grid = new Grid<>(ActivityResponse.class, false);
    private final TextField searchField = new TextField();
    private final ComboBox<ActivityType> typeFilter = new ComboBox<>();
    private final ComboBox<ActivityStatus> statusFilter = new ComboBox<>();
    private final VerticalLayout notesPanel = new VerticalLayout();
    private ActivityResponse selectedActivity = null;

    public ActivitiesView(TranslationService i18n, ActivityService activityService,
                          AccountService accountService,
                          ContactService contactService,
                          SecurityService securityService,
                          AttachmentService attachmentService) {
        this.i18n = i18n;
        this.activityService = activityService;
        this.accountService = accountService;
        this.contactService = contactService;
        this.securityService = securityService;
        this.attachmentService = attachmentService;
        setSizeFull();
        setPadding(true);

        typeFilter.setLabel(i18n.translate("common.column.type"));
        statusFilter.setLabel(i18n.translate("common.column.status"));

        configureGrid();
        configureNotesPanel();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2(i18n.translate("view.activities.title")), toolbar, grid, notesPanel);
        setFlexGrow(1, grid);

        grid.setItems(DataProvider.fromCallbacks(
            query -> {
                int page = query.getLimit() > 0 ? query.getOffset() / query.getLimit() : 0;
                return activityService.findAll(
                    PageRequest.of(page, query.getLimit()),
                    typeFilter.getValue(), statusFilter.getValue(), searchField.getValue()
                ).getContent().stream();
            },
            query -> (int) activityService.count(typeFilter.getValue(), statusFilter.getValue(), searchField.getValue())
        ));
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("pageTitle.activities");
    }

    private void configureGrid() {
        grid.setHeight("380px");
        grid.addColumn(ActivityResponse::title).setHeader(i18n.translate("view.activities.column.title")).setSortable(true).setFlexGrow(2);
        grid.addComponentColumn(a -> typeBadge(a.type())).setHeader(i18n.translate("common.column.type")).setFlexGrow(0).setWidth("110px");
        grid.addComponentColumn(a -> statusBadge(a.status())).setHeader(i18n.translate("common.column.status")).setFlexGrow(0).setWidth("120px");
        grid.addComponentColumn(a -> priorityBadge(a.priority())).setHeader(i18n.translate("view.activities.column.priority")).setFlexGrow(0).setWidth("100px");
        grid.addColumn(ActivityResponse::assignedToName).setHeader(i18n.translate("view.activities.column.assignedTo")).setSortable(true);
        grid.addColumn(ActivityResponse::accountName).setHeader(i18n.translate("common.column.account"));
        grid.addColumn(a -> a.dueDate() != null ? a.dueDate().toString() : "").setHeader(i18n.translate("view.activities.column.dueDate")).setSortable(true);
        grid.addComponentColumn(activity -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            if (activity.status() == ActivityStatus.OPEN) {
                Button assign = new Button(VaadinIcon.USER_CHECK.create(), e -> {
                    activityService.assign(activity.id(), securityService.getUsername());
                    refreshGrid();
                    if (selectedActivity != null && selectedActivity.id().equals(activity.id())) {
                        selectedActivity = activityService.findById(activity.id());
                        refreshNotesPanel();
                    }
                    notify(i18n.translate("notification.activity.assigned"), false);
                });
                assign.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                assign.getElement().setAttribute("title", i18n.translate("view.activities.tooltip.assign"));
                actions.add(assign);
            }

            if (activity.status() != ActivityStatus.RESOLVED && activity.status() != ActivityStatus.CLOSED) {
                Button resolve = new Button(VaadinIcon.CHECK.create(), e -> {
                    activityService.resolve(activity.id());
                    refreshGrid();
                    refreshNotesPanelIfSelected(activity.id());
                    notify(i18n.translate("notification.activity.resolved"), false);
                });
                resolve.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                resolve.getElement().setAttribute("title", i18n.translate("view.activities.tooltip.resolve"));
                actions.add(resolve);
            }

            if (activity.status() == ActivityStatus.RESOLVED || activity.status() == ActivityStatus.CLOSED) {
                Button reopen = new Button(VaadinIcon.REFRESH.create(), e -> {
                    activityService.reopen(activity.id());
                    refreshGrid();
                    refreshNotesPanelIfSelected(activity.id());
                    notify(i18n.translate("notification.activity.reopened"), false);
                });
                reopen.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                reopen.getElement().setAttribute("title", i18n.translate("view.activities.tooltip.reopen"));
                actions.add(reopen);
            }

            Button notes = new Button(VaadinIcon.COMMENT.create(), e -> {
                selectedActivity = activityService.findById(activity.id());
                refreshNotesPanel();
            });
            notes.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            notes.getElement().setAttribute("title", i18n.translate("view.activities.tooltip.notes"));

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(activity));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(activity));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(notes, edit, delete);
            return actions;
        }).setHeader(i18n.translate("common.column.actions")).setFlexGrow(0).setWidth("220px");
    }

    private void configureNotesPanel() {
        notesPanel.setPadding(false);
        notesPanel.setVisible(false);
        notesPanel.getStyle().set("border-top", "1px solid var(--lumo-contrast-20pct)").set("margin-top", "8px");
    }

    private void refreshNotesPanelIfSelected(Long activityId) {
        if (selectedActivity != null && selectedActivity.id().equals(activityId)) {
            selectedActivity = activityService.findById(activityId);
            refreshNotesPanel();
        }
    }

    private void refreshNotesPanel() {
        notesPanel.removeAll();
        if (selectedActivity == null) {
            notesPanel.setVisible(false);
            return;
        }
        notesPanel.setVisible(true);

        HorizontalLayout header = new HorizontalLayout(
                new H4(i18n.translate("view.activities.notes.title", selectedActivity.title())),
                new Button(VaadinIcon.CLOSE.create(), e -> {
                    selectedActivity = null;
                    notesPanel.setVisible(false);
                })
        );
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.expand(header.getComponentAt(0));
        notesPanel.add(header, new Hr());

        List<ActivityNoteResponse> notes = selectedActivity.notes();
        if (notes.isEmpty()) {
            notesPanel.add(new Span(i18n.translate("view.activities.notes.empty")));
        } else {
            for (ActivityNoteResponse note : notes) {
                Div card = new Div();
                card.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "4px")
                    .set("padding", "8px 12px")
                    .set("margin-bottom", "6px");
                Span meta = new Span(note.authorName() + "  ·  "
                        + (note.createdAt() != null ? note.createdAt().toString().replace("T", " ").substring(0, 16) : ""));
                meta.getStyle().set("font-size", "0.8em").set("color", "var(--lumo-secondary-text-color)");
                Div text = new Div();
                text.setText(note.text());
                card.add(meta, text);
                notesPanel.add(card);
            }
        }

        notesPanel.add(new Hr());
        TextArea noteText = new TextArea();
        noteText.setPlaceholder(i18n.translate("view.activities.notes.placeholder"));
        noteText.setWidthFull();
        noteText.setMinHeight("70px");
        Button addNote = new Button(i18n.translate("view.activities.button.addNote"), VaadinIcon.COMMENT.create(), e -> {
            if (noteText.getValue().isBlank()) return;
            activityService.addNote(selectedActivity.id(),
                    new ActivityNoteRequest(noteText.getValue()),
                    securityService.getUsername());
            selectedActivity = activityService.findById(selectedActivity.id());
            refreshNotesPanel();
        });
        addNote.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        notesPanel.add(noteText, addNote);
    }

    private HorizontalLayout buildToolbar() {
        typeFilter.setItems(ActivityType.values());
        typeFilter.setItemLabelGenerator(i18n::translateEnum);
        typeFilter.setPlaceholder(i18n.translate("view.activities.filter.allTypes"));
        typeFilter.setClearButtonVisible(true);
        typeFilter.setWidth("150px");
        typeFilter.addValueChangeListener(e -> refreshGrid());

        statusFilter.setItems(ActivityStatus.values());
        statusFilter.setItemLabelGenerator(i18n::translateEnum);
        statusFilter.setPlaceholder(i18n.translate("view.activities.filter.allStatuses"));
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("150px");
        statusFilter.addValueChangeListener(e -> refreshGrid());

        searchField.setPlaceholder(i18n.translate("view.activities.search.placeholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button(i18n.translate("view.activities.button.new"), VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button exportBtn = new Button(i18n.translate("common.button.exportCsv"), VaadinIcon.DOWNLOAD.create());
        exportBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        exportBtn.addClickListener(e -> {
            com.vaadin.flow.server.StreamResource resource = new com.vaadin.flow.server.StreamResource("activities.csv", () -> {
                String[] headers = {"id","title","type","status","priority","due_date","assigned_to","account","contact"};
                java.util.List<String[]> rows = activityService.findAllForExport(typeFilter.getValue(), statusFilter.getValue(), searchField.getValue()).stream().map(a -> new String[]{
                        a.id() != null ? a.id().toString() : "", a.title(), a.type() != null ? a.type().name() : "",
                        a.status() != null ? a.status().name() : "", a.priority() != null ? a.priority().name() : "",
                        a.dueDate() != null ? a.dueDate().toString() : "", a.assignedToName() != null ? a.assignedToName() : "",
                        a.accountName() != null ? a.accountName() : "", a.contactName() != null ? a.contactName() : ""
                }).toList();
                return com.crm.util.CsvExporter.build(headers, rows);
            });
            com.vaadin.flow.component.html.Anchor anchor = new com.vaadin.flow.component.html.Anchor(resource, "");
            anchor.getElement().setAttribute("download", true);
            anchor.getStyle().set("display", "none");
            add(anchor);
            anchor.getElement().executeJs("this.click(); setTimeout(() => this.remove(), 1000)");
        });

        HorizontalLayout toolbar = new HorizontalLayout(typeFilter, statusFilter, searchField, exportBtn, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        toolbar.setWidthFull();
        toolbar.expand(searchField);
        return toolbar;
    }

    private void refreshGrid() {
        grid.getDataProvider().refreshAll();
    }

    private void openDialog(ActivityResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.activities.dialog.new")
                : i18n.translate("view.activities.dialog.edit"));
        dialog.setWidth("560px");

        TextField title = new TextField(i18n.translate("view.activities.column.title"));
        TextArea description = new TextArea(i18n.translate("view.activities.field.description"));
        description.setMinHeight("80px");

        ComboBox<ActivityType> type = new ComboBox<>(i18n.translate("common.column.type"));
        type.setItems(ActivityType.values());
        type.setItemLabelGenerator(i18n::translateEnum);

        ComboBox<ActivityStatus> status = new ComboBox<>(i18n.translate("common.column.status"));
        status.setItems(ActivityStatus.values());
        status.setItemLabelGenerator(i18n::translateEnum);
        status.setValue(ActivityStatus.OPEN);

        ComboBox<ActivityPriority> priority = new ComboBox<>(i18n.translate("view.activities.column.priority"));
        priority.setItems(ActivityPriority.values());
        priority.setItemLabelGenerator(i18n::translateEnum);
        priority.setValue(ActivityPriority.MEDIUM);

        DatePicker dueDate = new DatePicker(i18n.translate("view.activities.column.dueDate"));

        List<AccountResponse> accounts = accountService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<AccountResponse> account = new ComboBox<>(i18n.translate("common.column.account"));
        account.setItems(accounts);
        account.setItemLabelGenerator(AccountResponse::name);
        account.setClearButtonVisible(true);

        List<ContactResponse> contacts = contactService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<ContactResponse> contact = new ComboBox<>(i18n.translate("common.column.contact"));
        contact.setItems(contacts);
        contact.setItemLabelGenerator(c -> c.firstName() + " " + c.lastName());
        contact.setClearButtonVisible(true);

        if (existing != null) {
            title.setValue(nvl(existing.title()));
            description.setValue(nvl(existing.description()));
            if (existing.type() != null) type.setValue(existing.type());
            if (existing.status() != null) status.setValue(existing.status());
            if (existing.priority() != null) priority.setValue(existing.priority());
            if (existing.dueDate() != null) dueDate.setValue(existing.dueDate());
            if (existing.accountId() != null)
                accounts.stream().filter(a -> a.id().equals(existing.accountId())).findFirst().ifPresent(account::setValue);
            if (existing.contactId() != null)
                contacts.stream().filter(c -> c.id().equals(existing.contactId())).findFirst().ifPresent(contact::setValue);
        }

        FormLayout form = new FormLayout(title, type, status, priority, dueDate, account, contact, description);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(title, 2);
        form.setColspan(description, 2);

        AttachmentPanel attachments = new AttachmentPanel(i18n, attachmentService, "ACTIVITY",
                existing != null ? existing.id() : null, securityService.getUsername());

        VerticalLayout body = new VerticalLayout(form, attachments);
        body.setPadding(false);
        dialog.add(body);

        Button save = new Button(i18n.translate("dialog.save"), e -> {
            if (title.getValue().isBlank()) {
                title.setInvalid(true);
                title.setErrorMessage(i18n.translate("view.activities.validation.titleRequired"));
                return;
            }
            ActivityRequest req = new ActivityRequest(
                    title.getValue(), description.getValue(),
                    type.getValue(), status.getValue(), priority.getValue(),
                    dueDate.getValue(), null,
                    account.getValue() != null ? account.getValue().id() : null,
                    contact.getValue() != null ? contact.getValue().id() : null);
            try {
                ActivityResponse saved;
                if (existing == null) saved = activityService.create(req, securityService.getUsername());
                else { activityService.update(existing.id(), req); saved = activityService.findById(existing.id()); }
                attachments.setEntityId(saved.id());
                refreshGrid();
                dialog.close();
                notify(i18n.translate("notification.activity.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button(i18n.translate("dialog.cancel"), e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(ActivityResponse activity) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(i18n.translate("view.activities.delete.header"));
        confirm.setText(i18n.translate("view.activities.delete.text", activity.title()));
        confirm.setConfirmText(i18n.translate("dialog.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            activityService.delete(activity.id());
            if (selectedActivity != null && selectedActivity.id().equals(activity.id())) {
                selectedActivity = null;
                notesPanel.setVisible(false);
            }
            refreshGrid();
            notify(i18n.translate("notification.activity.deleted"), false);
        });
        confirm.open();
    }

    private Span typeBadge(ActivityType type) {
        if (type == null) return new Span();
        Span badge = new Span(i18n.translateEnum(type));
        String theme = switch (type) {
            case BUG -> "badge error";
            case FEATURE -> "badge success";
            case MEETING -> "badge primary";
            case CALL -> "badge";
            case EMAIL -> "badge";
            case SALES_VISIT -> "badge success";
            default -> "badge contrast";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    private Span statusBadge(ActivityStatus status) {
        if (status == null) return new Span();
        Span badge = new Span(i18n.translateEnum(status));
        String theme = switch (status) {
            case OPEN -> "badge primary";
            case IN_PROGRESS -> "badge";
            case RESOLVED -> "badge success";
            case CLOSED -> "badge contrast";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    private Span priorityBadge(ActivityPriority priority) {
        if (priority == null) return new Span();
        Span badge = new Span(i18n.translateEnum(priority));
        String theme = switch (priority) {
            case CRITICAL -> "badge error";
            case HIGH -> "badge error small";
            case MEDIUM -> "badge";
            case LOW -> "badge contrast";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
