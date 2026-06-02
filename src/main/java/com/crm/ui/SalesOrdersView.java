package com.crm.ui;

import com.crm.domain.enums.SalesOrderStatus;
import com.crm.dto.request.SalesOrderLineItemRequest;
import com.crm.dto.request.SalesOrderRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.dto.response.QuoteResponse;
import com.crm.dto.response.SalesOrderLineItemResponse;
import com.crm.dto.response.SalesOrderResponse;
import com.crm.service.AccountService;
import com.crm.service.ContactService;
import com.crm.service.QuoteService;
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
import com.vaadin.flow.component.html.H4;
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

@Route(value = "sales-orders", layout = MainLayout.class)
@PageTitle("Sales Orders | CRM")
@RolesAllowed({"SALES", "ADMIN"})
public class SalesOrdersView extends VerticalLayout {

    private final SalesOrderService salesOrderService;
    private final AccountService accountService;
    private final ContactService contactService;
    private final QuoteService quoteService;

    private final Grid<SalesOrderResponse> grid = new Grid<>(SalesOrderResponse.class, false);
    private final Grid<SalesOrderLineItemResponse> lineItemGrid = new Grid<>(SalesOrderLineItemResponse.class, false);
    private final ComboBox<SalesOrderStatus> statusFilter = new ComboBox<>("Status");
    private final VerticalLayout detailPanel = new VerticalLayout();

    private SalesOrderResponse selectedOrder = null;

    public SalesOrdersView(SalesOrderService salesOrderService,
                           AccountService accountService,
                           ContactService contactService,
                           QuoteService quoteService) {
        this.salesOrderService = salesOrderService;
        this.accountService = accountService;
        this.contactService = contactService;
        this.quoteService = quoteService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        configureDetailPanel();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2("Sales Orders"), toolbar, grid, detailPanel);
        setFlexGrow(1, grid);

        grid.setItems(DataProvider.fromCallbacks(
            query -> {
                int page = query.getLimit() > 0 ? query.getOffset() / query.getLimit() : 0;
                return salesOrderService.findAll(PageRequest.of(page, query.getLimit()), statusFilter.getValue())
                        .getContent().stream();
            },
            query -> (int) salesOrderService.count(statusFilter.getValue())
        ));
    }

    private void configureGrid() {
        grid.setHeight("400px");
        grid.addColumn(SalesOrderResponse::orderNumber).setHeader("Order #").setFlexGrow(0).setWidth("130px");
        grid.addColumn(SalesOrderResponse::accountName).setHeader("Account").setSortable(true).setFlexGrow(2);
        grid.addComponentColumn(o -> statusBadge(o.status())).setHeader("Status").setFlexGrow(0).setWidth("120px");
        grid.addColumn(o -> o.totalAmount() != null ? o.currency() + " " + o.totalAmount() : "").setHeader("Total");
        grid.addColumn(o -> o.orderDate() != null ? o.orderDate().toString() : "").setHeader("Order Date");
        grid.addColumn(o -> o.deliveryDate() != null ? o.deliveryDate().toString() : "").setHeader("Delivery Date");
        grid.addComponentColumn(order -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            if (order.status() == SalesOrderStatus.DELIVERED) {
                Button convert = new Button(VaadinIcon.FILE_TEXT.create(), e -> convertToContract(order));
                convert.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                convert.getElement().setAttribute("title", "Convert to Contract");
                actions.add(convert);
            }

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(order));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(order));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(edit, delete);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setWidth("160px");

        grid.asSingleSelect().addValueChangeListener(e -> {
            selectedOrder = e.getValue();
            refreshDetailPanel();
        });
    }

    private void configureDetailPanel() {
        detailPanel.setPadding(false);
        detailPanel.setVisible(false);

        lineItemGrid.addColumn(SalesOrderLineItemResponse::productName).setHeader("Product").setFlexGrow(2);
        lineItemGrid.addColumn(SalesOrderLineItemResponse::quantity).setHeader("Qty");
        lineItemGrid.addColumn(SalesOrderLineItemResponse::unitPrice).setHeader("Unit Price");
        lineItemGrid.addColumn(i -> i.discountPct() + "%").setHeader("Discount");
        lineItemGrid.addColumn(SalesOrderLineItemResponse::lineTotal).setHeader("Total");
        lineItemGrid.addComponentColumn(item -> {
            Button del = new Button(VaadinIcon.TRASH.create(), e -> {
                if (selectedOrder != null) {
                    salesOrderService.removeLineItem(selectedOrder.id(), item.id());
                    selectedOrder = salesOrderService.findById(selectedOrder.id());
                    refreshDetailPanel();
                    refreshGrid();
                }
            });
            del.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return del;
        }).setHeader("").setFlexGrow(0).setWidth("60px");
        lineItemGrid.setHeight("200px");
    }

    private void refreshDetailPanel() {
        detailPanel.removeAll();
        if (selectedOrder == null) { detailPanel.setVisible(false); return; }
        detailPanel.setVisible(true);

        Button addItem = new Button("Add Line Item", VaadinIcon.PLUS.create(), e -> openLineItemDialog());
        addItem.addThemeVariants(ButtonVariant.LUMO_SMALL);

        HorizontalLayout header = new HorizontalLayout(new H4("Line Items"), addItem);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        SalesOrderResponse fresh = salesOrderService.findById(selectedOrder.id());
        lineItemGrid.setItems(fresh.lineItems());
        detailPanel.add(header, lineItemGrid);
    }

    private void openLineItemDialog() {
        if (selectedOrder == null) return;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Line Item");
        dialog.setWidth("420px");

        TextField productName = new TextField("Product Name");
        NumberField qty = new NumberField("Quantity");
        NumberField unitPrice = new NumberField("Unit Price");
        NumberField discount = new NumberField("Discount %");
        discount.setValue(0.0);

        FormLayout form = new FormLayout(productName, qty, unitPrice, discount);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(productName, 2);
        dialog.add(form);

        Button save = new Button("Add", e -> {
            if (productName.getValue().isBlank()) { productName.setInvalid(true); return; }
            SalesOrderLineItemRequest req = new SalesOrderLineItemRequest(
                    productName.getValue(),
                    qty.getValue() != null ? BigDecimal.valueOf(qty.getValue()) : null,
                    unitPrice.getValue() != null ? BigDecimal.valueOf(unitPrice.getValue()) : null,
                    discount.getValue() != null ? BigDecimal.valueOf(discount.getValue()) : BigDecimal.ZERO,
                    0);
            try {
                selectedOrder = salesOrderService.addLineItem(selectedOrder.id(), req);
                refreshDetailPanel();
                refreshGrid();
                dialog.close();
                notify("Line item added", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }

    private HorizontalLayout buildToolbar() {
        statusFilter.setItems(SalesOrderStatus.values());
        statusFilter.setPlaceholder("All Statuses");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("150px");
        statusFilter.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button("New Sales Order", VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(statusFilter, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        return toolbar;
    }

    private void refreshGrid() {
        grid.getDataProvider().refreshAll();
    }

    private void convertToContract(SalesOrderResponse order) {
        try {
            salesOrderService.convertToContract(order.id());
            refreshGrid();
            notify("Sales Order converted to Contract", false);
        } catch (Exception e) {
            notify(e.getMessage(), true);
        }
    }

    private void openDialog(SalesOrderResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Sales Order" : "Edit Sales Order");
        dialog.setWidth("600px");

        ComboBox<SalesOrderStatus> status = new ComboBox<>("Status");
        status.setItems(SalesOrderStatus.values());
        status.setValue(SalesOrderStatus.PENDING);

        DatePicker orderDate = new DatePicker("Order Date");
        DatePicker deliveryDate = new DatePicker("Delivery Date");
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

        List<QuoteResponse> quotes = quoteService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<QuoteResponse> quote = new ComboBox<>("Quote (optional)");
        quote.setItems(quotes);
        quote.setItemLabelGenerator(QuoteResponse::quoteNumber);
        quote.setClearButtonVisible(true);

        TextArea notes = new TextArea("Notes");
        notes.setMinHeight("80px");

        if (existing != null) {
            if (existing.status() != null) status.setValue(existing.status());
            if (existing.orderDate() != null) orderDate.setValue(existing.orderDate());
            if (existing.deliveryDate() != null) deliveryDate.setValue(existing.deliveryDate());
            if (existing.currency() != null) currency.setValue(existing.currency());
            if (existing.accountId() != null)
                accounts.stream().filter(a -> a.id().equals(existing.accountId())).findFirst().ifPresent(account::setValue);
            if (existing.quoteId() != null)
                quotes.stream().filter(q -> q.id().equals(existing.quoteId())).findFirst().ifPresent(quote::setValue);
        }

        FormLayout form = new FormLayout(status, currency, orderDate, deliveryDate, account, contact, quote, notes);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(notes, 2);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            SalesOrderRequest req = new SalesOrderRequest(
                    existing != null ? existing.orderNumber() : "SO",
                    status.getValue(), orderDate.getValue(), deliveryDate.getValue(),
                    currency.getValue(), notes.getValue(),
                    quote.getValue() != null ? quote.getValue().id() : null,
                    account.getValue() != null ? account.getValue().id() : null,
                    contact.getValue() != null ? contact.getValue().id() : null,
                    null, null);
            try {
                if (existing == null) salesOrderService.create(req, "admin");
                else salesOrderService.update(existing.id(), req);
                refreshGrid();
                dialog.close();
                notify("Sales Order saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(SalesOrderResponse order) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Sales Order");
        confirm.setText("Delete order " + order.orderNumber() + "?");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            salesOrderService.delete(order.id());
            selectedOrder = null;
            refreshGrid();
            refreshDetailPanel();
            notify("Sales Order deleted", false);
        });
        confirm.open();
    }

    private Span statusBadge(SalesOrderStatus status) {
        if (status == null) return new Span();
        Span badge = new Span(status.name());
        String theme = switch (status) {
            case PENDING -> "badge contrast";
            case CONFIRMED -> "badge primary";
            case DELIVERED -> "badge success";
            case CANCELLED -> "badge error";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    private static void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}
