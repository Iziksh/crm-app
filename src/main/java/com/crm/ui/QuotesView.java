package com.crm.ui;

import com.crm.domain.enums.QuoteStatus;
import com.crm.dto.request.QuoteLineItemRequest;
import com.crm.dto.request.QuoteRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.dto.response.OpportunityResponse;
import com.crm.dto.response.ProductResponse;
import com.crm.dto.response.QuoteLineItemResponse;
import com.crm.dto.response.QuoteResponse;
import com.crm.service.AccountService;
import com.crm.service.ContactService;
import com.crm.service.OpportunityService;
import com.crm.service.ProductService;
import com.crm.service.QuoteService;
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
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "quotes", layout = MainLayout.class)
@RolesAllowed({"SALES", "ADMIN"})
public class QuotesView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final QuoteService quoteService;
    private final AccountService accountService;
    private final ContactService contactService;
    private final OpportunityService opportunityService;
    private final ProductService productService;

    private final Grid<QuoteResponse> grid = new Grid<>(QuoteResponse.class, false);
    private final Grid<QuoteLineItemResponse> lineItemGrid = new Grid<>(QuoteLineItemResponse.class, false);
    private final ComboBox<QuoteStatus> statusFilter = new ComboBox<>();
    private final VerticalLayout detailPanel = new VerticalLayout();

    private QuoteResponse selectedQuote = null;

    public QuotesView(QuoteService quoteService,
                      AccountService accountService,
                      ContactService contactService,
                      OpportunityService opportunityService,
                      ProductService productService,
                      TranslationService i18n) {
        this.quoteService = quoteService;
        this.accountService = accountService;
        this.contactService = contactService;
        this.opportunityService = opportunityService;
        this.productService = productService;
        this.i18n = i18n;
        setSizeFull();
        setPadding(true);

        configureGrid();
        configureDetailPanel();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2(i18n.translate("view.quotes.title")), toolbar, grid, detailPanel);
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

    @Override
    public String getPageTitle() {
        return i18n.translate("page.quotes");
    }

    private void configureGrid() {
        grid.setHeight("400px");
        grid.addColumn(QuoteResponse::quoteNumber).setHeader(i18n.translate("view.quotes.column.quoteNumber")).setFlexGrow(0).setWidth("130px");
        grid.addColumn(QuoteResponse::title).setHeader(i18n.translate("common.title")).setSortable(true).setFlexGrow(2);
        grid.addColumn(QuoteResponse::accountName).setHeader(i18n.translate("common.account")).setSortable(true);
        grid.addComponentColumn(q -> statusBadge(q.status())).setHeader(i18n.translate("common.status")).setFlexGrow(0).setWidth("110px");
        grid.addColumn(q -> q.totalAmount() != null ? q.currency() + " " + q.totalAmount() : "")
                .setHeader(i18n.translate("common.total")).setSortable(true);
        grid.addColumn(q -> q.validUntil() != null ? q.validUntil().toString() : "").setHeader(i18n.translate("view.quotes.validUntil"));
        grid.addComponentColumn(quote -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            if (quote.status() == QuoteStatus.WON) {
                Button convert = new Button(VaadinIcon.CART.create(), e -> convertToOrder(quote));
                convert.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                convert.getElement().setAttribute("title", i18n.translate("view.quotes.convertToSalesOrder"));
                actions.add(convert);
            }

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(quote));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(quote));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(edit, delete);
            return actions;
        }).setHeader(i18n.translate("common.actions")).setFlexGrow(0).setWidth("160px");

        grid.asSingleSelect().addValueChangeListener(e -> {
            selectedQuote = e.getValue();
            refreshDetailPanel();
        });
    }

    private void configureDetailPanel() {
        detailPanel.setPadding(false);
        detailPanel.setVisible(false);

        lineItemGrid.addColumn(QuoteLineItemResponse::productName).setHeader(i18n.translate("common.product")).setFlexGrow(2);
        lineItemGrid.addColumn(QuoteLineItemResponse::quantity).setHeader(i18n.translate("common.qty"));
        lineItemGrid.addColumn(QuoteLineItemResponse::unitPrice).setHeader(i18n.translate("common.unitPrice"));
        lineItemGrid.addColumn(i -> i.discountPct() + "%").setHeader(i18n.translate("common.discount"));
        lineItemGrid.addColumn(QuoteLineItemResponse::lineTotal).setHeader(i18n.translate("common.total"));
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

        Button addItem = new Button(i18n.translate("common.addLineItem"), VaadinIcon.PLUS.create(), e -> openLineItemDialog());
        addItem.addThemeVariants(ButtonVariant.LUMO_SMALL);

        HorizontalLayout header = new HorizontalLayout(new H4(i18n.translate("common.lineItems")), addItem);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        QuoteResponse fresh = quoteService.findById(selectedQuote.id());
        lineItemGrid.setItems(fresh.lineItems());
        detailPanel.add(header, lineItemGrid);
    }

    private void openLineItemDialog() {
        if (selectedQuote == null) return;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(i18n.translate("common.addLineItem"));
        dialog.setWidth("440px");

        ComboBox<ProductResponse> productCombo = new ComboBox<>(i18n.translate("common.product"));
        productCombo.setItems(productService.findAll(PageRequest.of(0, 500), null).getContent());
        productCombo.setItemLabelGenerator(p -> p.sku() + " — " + p.name());
        productCombo.setClearButtonVisible(true);
        productCombo.setWidthFull();

        TextField productName = new TextField(i18n.translate("common.productName"));
        productName.setPlaceholder(i18n.translate("common.orTypeManually"));

        NumberField qty = new NumberField(i18n.translate("common.quantity"));
        NumberField unitPrice = new NumberField(i18n.translate("common.unitPrice"));
        NumberField discount = new NumberField(i18n.translate("common.discountPercent"));
        discount.setValue(0.0);

        productCombo.addValueChangeListener(e -> {
            ProductResponse p = e.getValue();
            if (p != null) {
                productName.setValue(p.name());
                unitPrice.setValue(p.unitPrice() != null ? p.unitPrice().doubleValue() : 0.0);
            }
        });

        FormLayout form = new FormLayout(productCombo, productName, qty, unitPrice, discount);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(productCombo, 2);
        form.setColspan(productName, 2);
        dialog.add(form);

        Button save = new Button(i18n.translate("common.add"), e -> {
            String name = productName.getValue().isBlank()
                    ? (productCombo.getValue() != null ? productCombo.getValue().name() : "")
                    : productName.getValue();
            if (name.isBlank()) { productName.setInvalid(true); return; }
            QuoteLineItemRequest req = new QuoteLineItemRequest(
                    name,
                    qty.getValue() != null ? BigDecimal.valueOf(qty.getValue()) : null,
                    unitPrice.getValue() != null ? BigDecimal.valueOf(unitPrice.getValue()) : null,
                    discount.getValue() != null ? BigDecimal.valueOf(discount.getValue()) : BigDecimal.ZERO,
                    0);
            try {
                selectedQuote = quoteService.addLineItem(selectedQuote.id(), req);
                refreshDetailPanel();
                refreshGrid();
                dialog.close();
                notify(i18n.translate("view.quotes.notification.lineItemAdded"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button(i18n.translate("common.cancel"), e -> dialog.close()), save);
        dialog.open();
    }

    private HorizontalLayout buildToolbar() {
        statusFilter.setLabel(i18n.translate("common.status"));
        statusFilter.setItems(QuoteStatus.values());
        statusFilter.setItemLabelGenerator(i18n::translateEnum);
        statusFilter.setPlaceholder(i18n.translate("common.allStatuses"));
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("150px");
        statusFilter.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button(i18n.translate("view.quotes.newQuote"), VaadinIcon.PLUS.create(), e -> openDialog(null));
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
            notify(i18n.translate("view.quotes.notification.converted"), false);
        } catch (Exception e) {
            notify(e.getMessage(), true);
        }
    }

    private void openDialog(QuoteResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.quotes.newQuote")
                : i18n.translate("view.quotes.editQuote"));
        dialog.setWidth("600px");

        TextField title = new TextField(i18n.translate("common.title"));

        ComboBox<QuoteStatus> status = new ComboBox<>(i18n.translate("common.status"));
        status.setItems(QuoteStatus.values());
        status.setItemLabelGenerator(i18n::translateEnum);
        status.setValue(QuoteStatus.DRAFT);

        DatePicker validUntil = new DatePicker(i18n.translate("view.quotes.validUntil"));
        TextField currency = new TextField(i18n.translate("common.currency"));
        currency.setValue("USD");

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

        List<OpportunityResponse> opps = opportunityService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<OpportunityResponse> opportunity = new ComboBox<>(i18n.translate("common.opportunity"));
        opportunity.setItems(opps);
        opportunity.setItemLabelGenerator(OpportunityResponse::name);
        opportunity.setClearButtonVisible(true);

        TextArea notes = new TextArea(i18n.translate("common.notes"));
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

        Button save = new Button(i18n.translate("common.save"), e -> {
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
                notify(i18n.translate("view.quotes.notification.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button(i18n.translate("common.cancel"), e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(QuoteResponse quote) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(i18n.translate("view.quotes.deleteQuote"));
        confirm.setText(i18n.translate("dialog.deleteConfirm", quote.title()));
        confirm.setConfirmText(i18n.translate("common.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            quoteService.delete(quote.id());
            selectedQuote = null;
            refreshGrid();
            refreshDetailPanel();
            notify(i18n.translate("view.quotes.notification.deleted"), false);
        });
        confirm.open();
    }

    private Span statusBadge(QuoteStatus status) {
        if (status == null) return new Span();
        Span badge = new Span(i18n.translateEnum(status));
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

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
