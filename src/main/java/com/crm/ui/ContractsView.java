package com.crm.ui;

import com.crm.domain.enums.ContractStatus;
import com.crm.dto.request.ContractRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.dto.response.ContractResponse;
import com.crm.dto.response.SalesOrderResponse;
import com.crm.service.AccountService;
import com.crm.service.ContactService;
import com.crm.service.ContractService;
import com.crm.service.SalesOrderService;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.data.provider.DataProvider;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "contracts", layout = MainLayout.class)
@PageTitle("Contracts | CRM")
@RolesAllowed({"SALES", "ADMIN"})
public class ContractsView extends VerticalLayout {

    private final ContractService contractService;
    private final AccountService accountService;
    private final ContactService contactService;
    private final SalesOrderService salesOrderService;

    private final Grid<ContractResponse> grid = new Grid<>(ContractResponse.class, false);
    private final ComboBox<ContractStatus> statusFilter = new ComboBox<>("Status");

    public ContractsView(ContractService contractService,
                         AccountService accountService,
                         ContactService contactService,
                         SalesOrderService salesOrderService) {
        this.contractService = contractService;
        this.accountService = accountService;
        this.contactService = contactService;
        this.salesOrderService = salesOrderService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2("Contracts"), toolbar, grid);
        setFlexGrow(1, grid);

        grid.setItems(DataProvider.fromCallbacks(
            query -> {
                int page = query.getLimit() > 0 ? query.getOffset() / query.getLimit() : 0;
                return contractService.findAll(PageRequest.of(page, query.getLimit()), statusFilter.getValue())
                        .getContent().stream();
            },
            query -> (int) contractService.count(statusFilter.getValue())
        ));
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(ContractResponse::contractNumber).setHeader("Contract #").setFlexGrow(0).setWidth("140px");
        grid.addColumn(ContractResponse::title).setHeader("Title").setSortable(true).setFlexGrow(2);
        grid.addColumn(ContractResponse::accountName).setHeader("Account").setSortable(true);
        grid.addComponentColumn(c -> statusBadge(c.status())).setHeader("Status").setFlexGrow(0).setWidth("110px");
        grid.addColumn(c -> c.totalValue() != null ? c.currency() + " " + c.totalValue() : "").setHeader("Value");
        grid.addColumn(c -> c.startDate() != null ? c.startDate().toString() : "").setHeader("Start Date");
        grid.addColumn(c -> c.endDate() != null ? c.endDate().toString() : "").setHeader("End Date");
        grid.addComponentColumn(contract -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(contract));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(contract));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(edit, delete);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setWidth("110px");
    }

    private HorizontalLayout buildToolbar() {
        statusFilter.setItems(ContractStatus.values());
        statusFilter.setPlaceholder("All Statuses");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("150px");
        statusFilter.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button("New Contract", VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(statusFilter, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        return toolbar;
    }

    private void refreshGrid() {
        grid.getDataProvider().refreshAll();
    }

    private void openDialog(ContractResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Contract" : "Edit Contract");
        dialog.setWidth("640px");

        TextField title = new TextField("Title");

        ComboBox<ContractStatus> status = new ComboBox<>("Status");
        status.setItems(ContractStatus.values());
        status.setValue(ContractStatus.DRAFT);

        DatePicker startDate = new DatePicker("Start Date");
        DatePicker endDate = new DatePicker("End Date");

        NumberField totalValue = new NumberField("Total Value");
        TextField currency = new TextField("Currency");
        currency.setValue("USD");

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

        List<SalesOrderResponse> orders = salesOrderService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<SalesOrderResponse> salesOrder = new ComboBox<>("Sales Order (optional)");
        salesOrder.setItems(orders);
        salesOrder.setItemLabelGenerator(SalesOrderResponse::orderNumber);
        salesOrder.setClearButtonVisible(true);

        TextArea description = new TextArea("Description");
        description.setMinHeight("70px");
        TextArea terms = new TextArea("Terms");
        terms.setMinHeight("70px");

        if (existing != null) {
            title.setValue(nvl(existing.title()));
            if (existing.status() != null) status.setValue(existing.status());
            if (existing.startDate() != null) startDate.setValue(existing.startDate());
            if (existing.endDate() != null) endDate.setValue(existing.endDate());
            if (existing.totalValue() != null) totalValue.setValue(existing.totalValue().doubleValue());
            if (existing.currency() != null) currency.setValue(existing.currency());
            if (existing.accountId() != null)
                accounts.stream().filter(a -> a.id().equals(existing.accountId())).findFirst().ifPresent(account::setValue);
            if (existing.contactId() != null)
                contacts.stream().filter(c -> c.id().equals(existing.contactId())).findFirst().ifPresent(contact::setValue);
            if (existing.salesOrderId() != null)
                orders.stream().filter(o -> o.id().equals(existing.salesOrderId())).findFirst().ifPresent(salesOrder::setValue);
        }

        FormLayout form = new FormLayout(title, status, startDate, endDate, totalValue, currency,
                account, contact, salesOrder, description, terms);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(title, 2);
        form.setColspan(description, 2);
        form.setColspan(terms, 2);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            if (title.getValue().isBlank()) { title.setInvalid(true); return; }
            BigDecimal val = totalValue.getValue() != null ? BigDecimal.valueOf(totalValue.getValue()) : null;
            ContractRequest req = new ContractRequest(
                    title.getValue(), status.getValue(), startDate.getValue(), endDate.getValue(),
                    val, currency.getValue(), description.getValue(), terms.getValue(),
                    salesOrder.getValue() != null ? salesOrder.getValue().id() : null,
                    account.getValue() != null ? account.getValue().id() : null,
                    contact.getValue() != null ? contact.getValue().id() : null,
                    null);
            try {
                if (existing == null) contractService.create(req, "admin");
                else contractService.update(existing.id(), req);
                refreshGrid();
                dialog.close();
                notify("Contract saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(ContractResponse contract) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Contract");
        confirm.setText("Delete \"" + contract.title() + "\"?");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            contractService.delete(contract.id());
            refreshGrid();
            notify("Contract deleted", false);
        });
        confirm.open();
    }

    private Span statusBadge(ContractStatus status) {
        if (status == null) return new Span();
        Span badge = new Span(status.name());
        String theme = switch (status) {
            case DRAFT -> "badge contrast";
            case ACTIVE -> "badge success";
            case EXPIRED -> "badge error";
            case TERMINATED -> "badge error small";
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
