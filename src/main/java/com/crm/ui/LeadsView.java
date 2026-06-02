package com.crm.ui;

import com.crm.domain.enums.LeadSource;
import com.crm.domain.enums.LeadStatus;
import com.crm.dto.request.LeadRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.dto.response.LeadResponse;
import com.crm.service.AccountService;
import com.crm.service.ContactService;
import com.crm.service.LeadService;
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
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "leads", layout = MainLayout.class)
@PageTitle("Leads | CRM")
@RolesAllowed({"SALES", "ADMIN"})
public class LeadsView extends VerticalLayout {

    private final LeadService leadService;
    private final AccountService accountService;
    private final ContactService contactService;

    private final Grid<LeadResponse> grid = new Grid<>(LeadResponse.class, false);
    private final ComboBox<LeadStatus> statusFilter = new ComboBox<>("Status");
    private final TextField searchField = new TextField();

    public LeadsView(LeadService leadService,
                     AccountService accountService,
                     ContactService contactService) {
        this.leadService = leadService;
        this.accountService = accountService;
        this.contactService = contactService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2("Leads"), toolbar, grid);
        setFlexGrow(1, grid);

        grid.setItems(DataProvider.fromCallbacks(
            query -> {
                int page = query.getLimit() > 0 ? query.getOffset() / query.getLimit() : 0;
                return leadService.findAll(
                    PageRequest.of(page, query.getLimit()),
                    statusFilter.getValue(), searchField.getValue()
                ).getContent().stream();
            },
            query -> (int) leadService.count(statusFilter.getValue(), searchField.getValue())
        ));
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(LeadResponse::title).setHeader("Title").setSortable(true).setFlexGrow(2);
        grid.addColumn(LeadResponse::company).setHeader("Company").setSortable(true);
        grid.addComponentColumn(l -> statusBadge(l.status())).setHeader("Status").setFlexGrow(0).setWidth("120px");
        grid.addColumn(l -> l.estimatedValue() != null ? l.currency() + " " + l.estimatedValue() : "")
                .setHeader("Est. Value").setSortable(true);
        grid.addColumn(l -> l.source() != null ? l.source().name() : "").setHeader("Source");
        grid.addColumn(l -> l.closeDate() != null ? l.closeDate().toString() : "").setHeader("Close Date").setSortable(true);
        grid.addColumn(LeadResponse::assignedToName).setHeader("Assigned To");
        grid.addComponentColumn(lead -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            if (lead.status() == LeadStatus.QUALIFIED) {
                Button convert = new Button(VaadinIcon.ARROW_FORWARD.create(), e -> convertLead(lead));
                convert.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                convert.getElement().setAttribute("title", "Convert to Opportunity");
                actions.add(convert);
            }

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(lead));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(lead));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(edit, delete);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setWidth("160px");
    }

    private HorizontalLayout buildToolbar() {
        statusFilter.setItems(LeadStatus.values());
        statusFilter.setPlaceholder("All Statuses");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("150px");
        statusFilter.addValueChangeListener(e -> refreshGrid());

        searchField.setPlaceholder("Search title or company…");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button("New Lead", VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(statusFilter, searchField, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        toolbar.setWidthFull();
        toolbar.expand(searchField);
        return toolbar;
    }

    private void refreshGrid() {
        grid.getDataProvider().refreshAll();
    }

    private void convertLead(LeadResponse lead) {
        try {
            leadService.convert(lead.id());
            refreshGrid();
            notify("Lead converted to Opportunity", false);
        } catch (Exception e) {
            notify(e.getMessage(), true);
        }
    }

    private void openDialog(LeadResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Lead" : "Edit Lead");
        dialog.setWidth("600px");

        TextField title = new TextField("Title");
        TextField firstName = new TextField("First Name");
        TextField lastName = new TextField("Last Name");
        TextField email = new TextField("Email");
        TextField phone = new TextField("Phone");
        TextField company = new TextField("Company");

        ComboBox<LeadStatus> status = new ComboBox<>("Status");
        status.setItems(LeadStatus.values());
        status.setValue(LeadStatus.NEW);

        ComboBox<LeadSource> source = new ComboBox<>("Source");
        source.setItems(LeadSource.values());

        NumberField estimatedValue = new NumberField("Est. Value");
        TextField currency = new TextField("Currency");
        currency.setValue("USD");
        currency.setWidth("80px");

        DatePicker closeDate = new DatePicker("Close Date");
        TextArea notes = new TextArea("Notes");
        notes.setMinHeight("80px");

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
            firstName.setValue(nvl(existing.firstName()));
            lastName.setValue(nvl(existing.lastName()));
            email.setValue(nvl(existing.email()));
            phone.setValue(nvl(existing.phone()));
            company.setValue(nvl(existing.company()));
            if (existing.status() != null) status.setValue(existing.status());
            if (existing.source() != null) source.setValue(existing.source());
            if (existing.estimatedValue() != null) estimatedValue.setValue(existing.estimatedValue().doubleValue());
            if (existing.currency() != null) currency.setValue(existing.currency());
            if (existing.closeDate() != null) closeDate.setValue(existing.closeDate());
            notes.setValue(nvl(existing.notes()));
            if (existing.accountId() != null) {
                accounts.stream().filter(a -> a.id().equals(existing.accountId()))
                        .findFirst().ifPresent(account::setValue);
            }
            if (existing.contactId() != null) {
                contacts.stream().filter(c -> c.id().equals(existing.contactId()))
                        .findFirst().ifPresent(contact::setValue);
            }
        }

        FormLayout form = new FormLayout(title, company, firstName, lastName, email, phone,
                status, source, estimatedValue, currency, closeDate, account, contact, notes);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(title, 2);
        form.setColspan(notes, 2);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            if (title.getValue().isBlank()) {
                title.setInvalid(true);
                title.setErrorMessage("Title is required");
                return;
            }
            BigDecimal val = estimatedValue.getValue() != null
                    ? BigDecimal.valueOf(estimatedValue.getValue()) : null;
            LeadRequest req = new LeadRequest(
                    title.getValue(), firstName.getValue(), lastName.getValue(),
                    email.getValue(), phone.getValue(), company.getValue(),
                    status.getValue(), source.getValue(), val, currency.getValue(),
                    closeDate.getValue(), notes.getValue(), null,
                    account.getValue() != null ? account.getValue().id() : null,
                    contact.getValue() != null ? contact.getValue().id() : null);
            try {
                if (existing == null) leadService.create(req, "admin");
                else leadService.update(existing.id(), req);
                refreshGrid();
                dialog.close();
                notify("Lead saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(LeadResponse lead) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Lead");
        confirm.setText("Delete \"" + lead.title() + "\"?");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            leadService.delete(lead.id());
            refreshGrid();
            notify("Lead deleted", false);
        });
        confirm.open();
    }

    private Span statusBadge(LeadStatus status) {
        if (status == null) return new Span();
        Span badge = new Span(status.name());
        String theme = switch (status) {
            case NEW -> "badge contrast";
            case CONTACTED -> "badge primary";
            case QUALIFIED -> "badge";
            case WON -> "badge success";
            case LOST -> "badge error";
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
