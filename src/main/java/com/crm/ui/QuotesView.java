package com.crm.ui;

import com.crm.domain.enums.QuoteStatus;
import com.crm.dto.request.QuoteLineItemRequest;
import com.crm.dto.request.QuoteRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.dto.response.OpportunityResponse;
import com.crm.dto.response.QuoteLineItemResponse;
import com.crm.dto.response.QuoteResponse;
import com.crm.service.AccountService;
import com.crm.service.ContactService;
import com.crm.service.OpportunityService;
import com.crm.service.QuoteService;
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

@Route(value = "quotes", layout = MainLayout.class)
@PageTitle("Quotes | CRM")
@RolesAllowed({"SALES", "ADMIN"})
public class QuotesView extends VerticalLayout {

    private final QuoteService quoteService;
    private final AccountService accountService;
    private final ContactService contactService;
    private final OpportunityService opportunityService;

    private final Grid<QuoteResponse> grid = new Grid<>(QuoteResponse.class, false);
    private final Grid<QuoteLineItemResponse> lineItemGrid = new Grid<>(QuoteLineItemResponse.class, false);
    private final ComboBox<QuoteStatus> statusFilter = new ComboBox<>("Status");
    private final VerticalLayout detailPanel = new VerticalLayout();

    private QuoteResponse selectedQuote = null;

    public QuotesView(QuoteService quoteService,
                      AccountService accountService,
                      ContactService contactService,
                      OpportunityService opportunityService) {
        this.quoteService = quoteService;
        this.accountService = accountService;
        this.contactService = contactService;
        this.opportunityService = opportunityService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        configureDetailPanel();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2("Quotes"), toolbar, grid, detailPanel);
        setFlexGrow(1, grid);

        grid.setItems(DataProvider.fromCallbacks(
            query -> {
                int page = query.getLimit() > 0 ? query.getOffset() / query.getLimit() : 0;
                return quoteService.findAll(PageRequest.of(page, query.getLimit()), statusFilter.getValue())
                        .getContent().stream();
            },
            query -> (int) quoteService.count(statusFilter.getValue())
        ));
    }

    private void configureGrid() {
        grid.setHeight("400px");
        grid.addColumn(QuoteResponse::quoteNumber).setHeader("Quote #").setFlexGrow(0).setWidth("130px");
        grid.addColumn(QuoteResponse::title).setHeader("Title").setSortable(true).setFlexGrow(2);
        grid.addColumn(QuoteResponse::accountName).setHeader("Account").setSortable(true);
        grid.addComponentColumn(q -> statusBadge(q.status())).setHeader("Status").setFlexGrow(0).setWidth("110px");
        grid.addColumn(q -> q.totalAmount() != null ? q.currency() + " " + q.totalAmount() : "")
                .setHeader("Total").setSortable(true);
        grid.addColumn(q -> q.validUntil() != null ? q.validUntil().toString() : "").setHeader("Valid Until");
        grid.addComponentColumn(quote -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            if (quote.status() == QuoteStatus.WON) {
                Button convert = new Button(VaadinIcon.CART.create(), e -> convertToOrder(quote));
                convert.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                convert.getElement().setAttribute("title", "Convert to Sales Order");
                actions.add(convert);
            }

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(quote));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(quote));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(edit, delete);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setWidth("160px");

        grid.asSingleSelect().addValueChangeListener(e -> {
            selectedQuote = e.getValue();
            refreshDetailPanel();
        });
    }

    private void configureDetailPanel() {
        detailPanel.setPadding(false);
        detailPanel.setVisible(false);

        lineItemGrid.addColumn(QuoteLineItemResponse::productName).setHeader("Product").setFlexGrow(2);
        lineItemGrid.addColumn(QuoteLineItemResponse::quantity).setHeader("Qty");
        lineItemGrid.addColumn(QuoteLineItemResponse::unitPrice).setHeader("Unit Price");
        lineItemGrid.addColumn(i -> i.discountPct() + "%").setHeader("Discount");
        lineItemGrid.addColumn(QuoteLineItemResponse::lineTotal).setHeader("Total");
        lineItemGrid.addComponentColumn(item -> {
            Button del = new Button(VaadinIcon.TRASH.create(), e -> {
                if (selectedQuote != null) {
                    quoteService.removeLineItem(selectedQuote.id(), item.id());
                    selectedQuote = quoteService.findById(selectedQuote.id());
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
        if (selectedQuote == null) {
            detailPanel.setVisible(false);
            return;
        }
        detailPanel.setVisible(true);

        Button addItem = new Button("Add Line Item", VaadinIcon.PLUS.create(), e -> openLineItemDialog());
        addItem.addThemeVariants(ButtonVariant.LUMO_SMALL);

        HorizontalLayout header = new HorizontalLayout(new H4("Line Items"), addItem);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        QuoteResponse fresh = quoteService.findById(selectedQuote.id());
        lineItemGrid.setItems(fresh.lineItems());
        detailPanel.add(header, lineItemGrid);
    }

    private void openLineItemDialog() {
        if (selectedQuote == null) return;
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
            QuoteLineItemRequest req = new QuoteLineItemRequest(
                    productName.getValue(),
                    qty.getValue() != null ? BigDecimal.valueOf(qty.getValue()) : null,
                    unitPrice.getValue() != null ? BigDecimal.valueOf(unitPrice.getValue()) : null,
                    discount.getValue() != null ? BigDecimal.valueOf(discount.getValue()) : BigDecimal.ZERO,
                    0);
            try {
                selectedQuote = quoteService.addLineItem(selectedQuote.id(), req);
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
        statusFilter.setItems(QuoteStatus.values());
        statusFilter.setPlaceholder("All Statuses");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("150px");
        statusFilter.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button("New Quote", VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(statusFilter, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        return toolbar;
    }

    private void refreshGrid() {
        grid.getDataProvider().refreshAll();
    }

    private void convertToOrder(QuoteResponse quote) {
        try {
            quoteService.convertToOrder(quote.id());
            refreshGrid();
            notify("Quote converted to Sales Order", false);
        } catch (Exception e) {
            notify(e.getMessage(), true);
        }
    }

    private void openDialog(QuoteResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Quote" : "Edit Quote");
        dialog.setWidth("600px");

        TextField title = new TextField("Title");

        ComboBox<QuoteStatus> status = new ComboBox<>("Status");
        status.setItems(QuoteStatus.values());
        status.setValue(QuoteStatus.DRAFT);

        DatePicker validUntil = new DatePicker("Valid Until");
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

        List<OpportunityResponse> opps = opportunityService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<OpportunityResponse> opportunity = new ComboBox<>("Opportunity");
        opportunity.setItems(opps);
        opportunity.setItemLabelGenerator(OpportunityResponse::name);
        opportunity.setClearButtonVisible(true);

        TextArea notes = new TextArea("Notes");
        notes.setMinHeight("80px");

        if (existing != null) {
            title.setValue(nvl(existing.title()));
            if (existing.status() != null) status.setValue(existing.status());
            if (existing.validUntil() != null) validUntil.setValue(existing.validUntil());
            if (existing.currency() != null) currency.setValue(existing.currency());
            if (existing.accountId() != null)
                accounts.stream().filter(a -> a.id().equals(existing.accountId())).findFirst().ifPresent(account::setValue);
            if (existing.contactId() != null)
                contacts.stream().filter(c -> c.id().equals(existing.contactId())).findFirst().ifPresent(contact::setValue);
            if (existing.opportunityId() != null)
                opps.stream().filter(o -> o.id().equals(existing.opportunityId())).findFirst().ifPresent(opportunity::setValue);
        }

        FormLayout form = new FormLayout(title, status, validUntil, currency, account, contact, opportunity, notes);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(title, 2);
        form.setColspan(notes, 2);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            if (title.getValue().isBlank()) { title.setInvalid(true); return; }
            QuoteRequest req = new QuoteRequest(
                    title.getValue(), status.getValue(), validUntil.getValue(), currency.getValue(),
                    notes.getValue(),
                    opportunity.getValue() != null ? opportunity.getValue().id() : null,
                    account.getValue() != null ? account.getValue().id() : null,
                    contact.getValue() != null ? contact.getValue().id() : null,
                    null, null);
            try {
                if (existing == null) quoteService.create(req, "admin");
                else quoteService.update(existing.id(), req);
                refreshGrid();
                dialog.close();
                notify("Quote saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(QuoteResponse quote) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Quote");
        confirm.setText("Delete \"" + quote.title() + "\"?");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            quoteService.delete(quote.id());
            selectedQuote = null;
            refreshGrid();
            refreshDetailPanel();
            notify("Quote deleted", false);
        });
        confirm.open();
    }

    private Span statusBadge(QuoteStatus status) {
        if (status == null) return new Span();
        Span badge = new Span(status.name());
        String theme = switch (status) {
            case DRAFT -> "badge contrast";
            case SENT -> "badge primary";
            case WON -> "badge success";
            case LOST -> "badge error";
            case EXPIRED -> "badge error small";
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
