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
import com.crm.service.TranslationService;
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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "leads", layout = MainLayout.class)
@RolesAllowed({"SALES", "ADMIN"})
public class LeadsView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final LeadService leadService;
    private final AccountService accountService;
    private final ContactService contactService;

    private final Grid<LeadResponse> grid = new Grid<>(LeadResponse.class, false);
    private final ComboBox<LeadStatus> statusFilter = new ComboBox<>();
    private final TextField searchField = new TextField();

    public LeadsView(LeadService leadService,
                     AccountService accountService,
                     ContactService contactService,
                     TranslationService i18n) {
        this.leadService = leadService;
        this.accountService = accountService;
        this.contactService = contactService;
        this.i18n = i18n;
        setSizeFull();
        setPadding(true);

        configureGrid();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2(i18n.translate("view.leads.title")), toolbar, grid);
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

    @Override
    public String getPageTitle() {
        return i18n.translate("page.leads");
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(LeadResponse::title).setHeader(i18n.translate("common.title")).setSortable(true).setFlexGrow(2);
        grid.addColumn(LeadResponse::company).setHeader(i18n.translate("common.company")).setSortable(true);
        grid.addComponentColumn(l -> statusBadge(l.status())).setHeader(i18n.translate("common.status")).setFlexGrow(0).setWidth("120px");
        grid.addColumn(l -> l.estimatedValue() != null ? l.currency() + " " + l.estimatedValue() : "")
                .setHeader(i18n.translate("view.leads.column.estValue")).setSortable(true);
        grid.addColumn(l -> l.source() != null ? i18n.translateEnum(l.source()) : "")
                .setHeader(i18n.translate("view.leads.column.source"));
        grid.addColumn(l -> l.closeDate() != null ? l.closeDate().toString() : "")
                .setHeader(i18n.translate("common.closeDate")).setSortable(true);
        grid.addColumn(LeadResponse::assignedToName).setHeader(i18n.translate("common.assignedTo"));
        grid.addComponentColumn(lead -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            if (lead.status() == LeadStatus.QUALIFIED) {
                Button convert = new Button(VaadinIcon.ARROW_FORWARD.create(), e -> convertLead(lead));
                convert.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                convert.getElement().setAttribute("title", i18n.translate("view.leads.convertToOpportunity"));
                actions.add(convert);
            }

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(lead));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(lead));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(edit, delete);
            return actions;
        }).setHeader(i18n.translate("common.actions")).setFlexGrow(0).setWidth("160px");
    }

    private HorizontalLayout buildToolbar() {
        statusFilter.setLabel(i18n.translate("common.status"));
        statusFilter.setItems(LeadStatus.values());
        statusFilter.setItemLabelGenerator(i18n::translateEnum);
        statusFilter.setPlaceholder(i18n.translate("common.allStatuses"));
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("150px");
        statusFilter.addValueChangeListener(e -> refreshGrid());

        searchField.setPlaceholder(i18n.translate("view.leads.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button(i18n.translate("view.leads.newLead"), VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button exportBtn = new Button(i18n.translate("common.exportCsv"), VaadinIcon.DOWNLOAD.create());
        exportBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        exportBtn.addClickListener(e -> {
            com.vaadin.flow.server.StreamResource resource = new com.vaadin.flow.server.StreamResource("leads.csv", () -> {
                String[] headers = {"id","title","first_name","last_name","email","company","status","source","estimated_value","currency"};
                java.util.List<String[]> rows = leadService.findAllForExport(statusFilter.getValue(), searchField.getValue()).stream().map(l -> new String[]{
                        l.id() != null ? l.id().toString() : "", l.title(),
                        l.firstName() != null ? l.firstName() : "", l.lastName() != null ? l.lastName() : "",
                        l.email() != null ? l.email() : "", l.company() != null ? l.company() : "",
                        l.status() != null ? l.status().name() : "", l.source() != null ? l.source().name() : "",
                        l.estimatedValue() != null ? l.estimatedValue().toString() : "", l.currency() != null ? l.currency() : ""
                }).toList();
                return com.crm.util.CsvExporter.build(headers, rows);
            });
            com.vaadin.flow.component.html.Anchor anchor = new com.vaadin.flow.component.html.Anchor(resource, "");
            anchor.getElement().setAttribute("download", true);
            anchor.getStyle().set("display", "none");
            add(anchor);
            anchor.getElement().executeJs("this.click(); setTimeout(() => this.remove(), 1000)");
        });

        HorizontalLayout toolbar = new HorizontalLayout(statusFilter, searchField, exportBtn, addBtn);
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
            notify(i18n.translate("view.leads.notification.converted"), false);
        } catch (Exception e) {
            notify(e.getMessage(), true);
        }
    }

    private void openDialog(LeadResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.leads.newLead")
                : i18n.translate("view.leads.editLead"));
        dialog.setWidth("600px");

        TextField title = new TextField(i18n.translate("common.title"));
        TextField firstName = new TextField(i18n.translate("common.firstName"));
        TextField lastName = new TextField(i18n.translate("common.lastName"));
        TextField email = new TextField(i18n.translate("common.email"));
        TextField phone = new TextField(i18n.translate("common.phone"));
        TextField company = new TextField(i18n.translate("common.company"));

        ComboBox<LeadStatus> status = new ComboBox<>(i18n.translate("common.status"));
        status.setItems(LeadStatus.values());
        status.setItemLabelGenerator(i18n::translateEnum);
        status.setValue(LeadStatus.NEW);

        ComboBox<LeadSource> source = new ComboBox<>(i18n.translate("view.leads.column.source"));
        source.setItems(LeadSource.values());
        source.setItemLabelGenerator(i18n::translateEnum);

        NumberField estimatedValue = new NumberField(i18n.translate("view.leads.estValue"));
        TextField currency = new TextField(i18n.translate("common.currency"));
        currency.setValue("USD");
        currency.setWidth("80px");

        DatePicker closeDate = new DatePicker(i18n.translate("common.closeDate"));
        TextArea notes = new TextArea(i18n.translate("common.notes"));
        notes.setMinHeight("80px");

        List<AccountResponse> accounts = accountService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<AccountResponse> account = new ComboBox<>(i18n.translate("common.account"));
        account.setItems(accounts);
        account.setItemLabelGenerator(AccountResponse::name);
        account.setClearButtonVisible(true);

        List<ContactResponse> contacts = contactService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<ContactResponse> contact = new ComboBox<>(i18n.translate("common.contact"));
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

        Button save = new Button(i18n.translate("common.save"), e -> {
            if (title.getValue().isBlank()) {
                title.setInvalid(true);
                title.setErrorMessage(i18n.translate("view.leads.validation.titleRequired"));
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
                notify(i18n.translate("view.leads.notification.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button(i18n.translate("common.cancel"), e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(LeadResponse lead) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(i18n.translate("view.leads.deleteLead"));
        confirm.setText(i18n.translate("dialog.deleteConfirm", lead.title()));
        confirm.setConfirmText(i18n.translate("common.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            leadService.delete(lead.id());
            refreshGrid();
            notify(i18n.translate("view.leads.notification.deleted"), false);
        });
        confirm.open();
    }

    private Span statusBadge(LeadStatus status) {
        if (status == null) return new Span();
        Span badge = new Span(i18n.translateEnum(status));
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

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
