package com.crm.ui;

import com.crm.domain.enums.ActivityPriority;
import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ActivityType;
import com.crm.dto.request.ActivityRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.ActivityResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.service.AccountService;
import com.crm.service.ActivityService;
import com.crm.service.ContactService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Route(value = "activities", layout = MainLayout.class)
@PageTitle("Activities | CRM")
@RolesAllowed({"SUPPORT", "ADMIN"})
public class ActivitiesView extends VerticalLayout {

    private final ActivityService activityService;
    private final AccountService accountService;
    private final ContactService contactService;

    private final Grid<ActivityResponse> grid = new Grid<>(ActivityResponse.class, false);
    private final TextField searchField = new TextField();
    private final ComboBox<ActivityType> typeFilter = new ComboBox<>("Type");
    private final ComboBox<ActivityStatus> statusFilter = new ComboBox<>("Status");

    public ActivitiesView(ActivityService activityService,
                          AccountService accountService,
                          ContactService contactService) {
        this.activityService = activityService;
        this.accountService = accountService;
        this.contactService = contactService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2("Activities"), toolbar, grid);
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

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(ActivityResponse::title).setHeader("Title").setSortable(true).setFlexGrow(2);
        grid.addComponentColumn(a -> typeBadge(a.type())).setHeader("Type").setFlexGrow(0).setWidth("110px");
        grid.addComponentColumn(a -> statusBadge(a.status())).setHeader("Status").setFlexGrow(0).setWidth("120px");
        grid.addComponentColumn(a -> priorityBadge(a.priority())).setHeader("Priority").setFlexGrow(0).setWidth("100px");
        grid.addColumn(ActivityResponse::assignedToName).setHeader("Assigned To").setSortable(true);
        grid.addColumn(ActivityResponse::accountName).setHeader("Account");
        grid.addColumn(a -> a.dueDate() != null ? a.dueDate().toString() : "").setHeader("Due Date").setSortable(true);
        grid.addComponentColumn(activity -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            if (activity.status() != ActivityStatus.RESOLVED && activity.status() != ActivityStatus.CLOSED) {
                Button resolve = new Button(VaadinIcon.CHECK.create(), e -> {
                    activityService.resolve(activity.id());
                    refreshGrid();
                    notify("Activity resolved", false);
                });
                resolve.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                resolve.getElement().setAttribute("title", "Resolve");
                actions.add(resolve);
            }

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(activity));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(activity));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(edit, delete);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setWidth("150px");
    }

    private HorizontalLayout buildToolbar() {
        typeFilter.setItems(ActivityType.values());
        typeFilter.setPlaceholder("All Types");
        typeFilter.setClearButtonVisible(true);
        typeFilter.setWidth("140px");
        typeFilter.addValueChangeListener(e -> refreshGrid());

        statusFilter.setItems(ActivityStatus.values());
        statusFilter.setPlaceholder("All Statuses");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("150px");
        statusFilter.addValueChangeListener(e -> refreshGrid());

        searchField.setPlaceholder("Search title…");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button("New Activity", VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(typeFilter, statusFilter, searchField, addBtn);
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
        dialog.setHeaderTitle(existing == null ? "New Activity" : "Edit Activity");
        dialog.setWidth("560px");

        TextField title = new TextField("Title");
        TextArea description = new TextArea("Description");
        description.setMinHeight("80px");

        ComboBox<ActivityType> type = new ComboBox<>("Type");
        type.setItems(ActivityType.values());

        ComboBox<ActivityStatus> status = new ComboBox<>("Status");
        status.setItems(ActivityStatus.values());
        status.setValue(ActivityStatus.OPEN);

        ComboBox<ActivityPriority> priority = new ComboBox<>("Priority");
        priority.setItems(ActivityPriority.values());
        priority.setValue(ActivityPriority.MEDIUM);

        DatePicker dueDate = new DatePicker("Due Date");

        List<AccountResponse> accounts = accountService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<AccountResponse> account = new ComboBox<>("Account");
        account.setItems(accounts);
        account.setItemLabelGenerator(AccountResponse::name);
        account.setClearButtonVisible(true);

        List<ContactResponse> contacts = contactService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<ContactResponse> contact = new ComboBox<>("Contact");
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
            if (existing.accountId() != null) {
                accounts.stream().filter(a -> a.id().equals(existing.accountId()))
                        .findFirst().ifPresent(account::setValue);
            }
            if (existing.contactId() != null) {
                contacts.stream().filter(c -> c.id().equals(existing.contactId()))
                        .findFirst().ifPresent(contact::setValue);
            }
        }

        FormLayout form = new FormLayout(title, type, status, priority, dueDate, account, contact, description);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(title, 2);
        form.setColspan(description, 2);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            if (title.getValue().isBlank()) {
                title.setInvalid(true);
                title.setErrorMessage("Title is required");
                return;
            }
            Long accountId = account.getValue() != null ? account.getValue().id() : null;
            Long contactId = contact.getValue() != null ? contact.getValue().id() : null;
            ActivityRequest req = new ActivityRequest(
                    title.getValue(), description.getValue(),
                    type.getValue(), status.getValue(), priority.getValue(),
                    dueDate.getValue(), null, accountId, contactId);
            try {
                if (existing == null) activityService.create(req, "admin");
                else activityService.update(existing.id(), req);
                refreshGrid();
                dialog.close();
                notify("Activity saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(ActivityResponse activity) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Activity");
        confirm.setText("Delete \"" + activity.title() + "\"?");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            activityService.delete(activity.id());
            refreshGrid();
            notify("Activity deleted", false);
        });
        confirm.open();
    }

    private Span typeBadge(ActivityType type) {
        if (type == null) return new Span();
        Span badge = new Span(type.name());
        String theme = switch (type) {
            case BUG -> "badge error";
            case FEATURE -> "badge success";
            case MEETING -> "badge primary";
            case CALL -> "badge";
            default -> "badge contrast";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    private Span statusBadge(ActivityStatus status) {
        if (status == null) return new Span();
        Span badge = new Span(status.name().replace('_', ' '));
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
        Span badge = new Span(priority.name());
        String theme = switch (priority) {
            case CRITICAL -> "badge error";
            case HIGH -> "badge error small";
            case MEDIUM -> "badge";
            case LOW -> "badge contrast";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    private static void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
