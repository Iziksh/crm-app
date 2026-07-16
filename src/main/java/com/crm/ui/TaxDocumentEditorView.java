package com.crm.ui;

import com.crm.billing.dto.CustomerQuickCreateResponse;
import com.crm.billing.dto.DocumentPaymentRequest;
import com.crm.billing.dto.LineItemRequest;
import com.crm.billing.dto.LineItemResponse;
import com.crm.billing.dto.TaxDocumentCreateRequest;
import com.crm.billing.dto.TaxDocumentResponse;
import com.crm.billing.enums.DiscountType;
import com.crm.billing.enums.DocumentStatus;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.enums.PaymentMethod;
import com.crm.billing.enums.RoundingMode;
import com.crm.billing.enums.VatType;
import com.crm.billing.service.CustomerQuickCreateService;
import com.crm.billing.service.TaxDocumentDraftService;
import com.crm.billing.service.TaxDocumentIssueService;
import com.crm.service.TranslationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Tax document editor (חשבונית מס/קבלה / קבלה / חשבון עסקה / הצעת מחיר / חשבונית זיכוי): document
 * type + currency, customer, date, free text, shared line-item grid with discount(%/₪) + rounding +
 * net/VAT/gross totals, payments-received section, save-draft / issue actions. */
@Route(value = "tax-documents", layout = MainLayout.class)
@RolesAllowed({"SALES", "ADMIN"})
public class TaxDocumentEditorView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final TaxDocumentDraftService draftService;
    private final TaxDocumentIssueService issueService;
    private final CustomerQuickCreateService customerQuickCreateService;
    private final AddonGate addonGate;

    private final Grid<TaxDocumentResponse> grid = new Grid<>(TaxDocumentResponse.class, false);
    private final List<LineItemRequest> pendingLineItems = new ArrayList<>();
    private final Grid<LineItemRequest> lineItemGrid = new Grid<>(LineItemRequest.class, false);

    public TaxDocumentEditorView(TaxDocumentDraftService draftService,
                                  TaxDocumentIssueService issueService,
                                  CustomerQuickCreateService customerQuickCreateService,
                                  AddonGate addonGate,
                                  TranslationService i18n) {
        this.draftService = draftService;
        this.issueService = issueService;
        this.customerQuickCreateService = customerQuickCreateService;
        this.addonGate = addonGate;
        this.i18n = i18n;
        setSizeFull();
        setPadding(true);

        if (!addonGate.currentUserHasBillingDocuments()) {
            add(addonGate.disabledNotice(i18n));
            return;
        }

        configureGrid();
        Button addBtn = new Button(i18n.translate("view.taxDocuments.new"), VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new H2(i18n.translate("view.taxDocuments.title")), addBtn, grid);
        setFlexGrow(1, grid);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("view.taxDocuments.title");
    }

    private void configureGrid() {
        grid.setHeight("500px");
        grid.addColumn(d -> i18n.translateEnum(d.documentType())).setHeader(i18n.translate("common.type")).setFlexGrow(0).setWidth("140px");
        grid.addColumn(d -> d.number() != null ? d.number() : "—").setHeader(i18n.translate("common.number")).setFlexGrow(0).setWidth("120px");
        grid.addColumn(TaxDocumentResponse::accountName).setHeader(i18n.translate("common.customer")).setSortable(true);
        grid.addColumn(d -> d.currency() + " " + d.grossTotal()).setHeader(i18n.translate("common.total"));
        grid.addComponentColumn(this::statusBadge).setHeader(i18n.translate("common.status")).setFlexGrow(0).setWidth("110px");
        grid.addComponentColumn(doc -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);
            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(doc));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            actions.add(edit);
            if (doc.status() == DocumentStatus.DRAFT) {
                Button issue = new Button(VaadinIcon.CHECK.create(), e -> issue(doc));
                issue.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                issue.getElement().setAttribute("title", i18n.translate("view.taxDocuments.issue"));
                actions.add(issue);
            }
            return actions;
        }).setHeader(i18n.translate("common.actions")).setFlexGrow(0).setWidth("120px");
    }

    private Span statusBadge(TaxDocumentResponse doc) {
        Span badge = new Span(i18n.translateEnum(doc.status()));
        String theme = switch (doc.status()) {
            case DRAFT -> "badge contrast";
            case ISSUED -> "badge success";
            case CANCELLED -> "badge error";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    private void refreshGrid() {
        grid.setItems(draftService.findAll(PageRequest.of(0, 200)).getContent());
    }

    private void issue(TaxDocumentResponse doc) {
        try {
            issueService.issue(doc.id());
            refreshGrid();
            notify(i18n.translate("view.taxDocuments.notification.issued"), false);
        } catch (Exception ex) {
            notify(ex.getMessage(), true);
        }
    }

    private void openDialog(TaxDocumentResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.taxDocuments.new")
                : i18n.translate("view.taxDocuments.edit"));
        dialog.setWidth("760px");

        ComboBox<DocumentType> documentType = new ComboBox<>(i18n.translate("common.type"));
        documentType.setItems(DocumentType.values());
        documentType.setItemLabelGenerator(i18n::translateEnum);
        documentType.setValue(DocumentType.TAX_INVOICE_RECEIPT);

        List<CustomerQuickCreateResponse> customers = customerQuickCreateService.search(null);
        ComboBox<CustomerQuickCreateResponse> customer = new ComboBox<>(i18n.translate("common.customer"));
        customer.setItems(customers);
        customer.setItemLabelGenerator(CustomerQuickCreateResponse::name);

        DatePicker documentDate = new DatePicker(i18n.translate("common.date"));
        documentDate.setValue(java.time.LocalDate.now());
        TextField currency = new TextField(i18n.translate("common.currency"));
        currency.setValue("ILS");

        ComboBox<VatType> vatType = new ComboBox<>(i18n.translate("common.vatType"));
        vatType.setItems(VatType.values());
        vatType.setItemLabelGenerator(i18n::translateEnum);
        vatType.setValue(VatType.STANDARD);

        ComboBox<DiscountType> discountType = new ComboBox<>(i18n.translate("common.discountType"));
        discountType.setItems(DiscountType.values());
        discountType.setItemLabelGenerator(i18n::translateEnum);
        discountType.setClearButtonVisible(true);
        NumberField discountValue = new NumberField(i18n.translate("common.discountValue"));
        discountValue.setValue(0.0);

        ComboBox<RoundingMode> roundingMode = new ComboBox<>(i18n.translate("common.roundingMode"));
        roundingMode.setItems(RoundingMode.values());
        roundingMode.setItemLabelGenerator(i18n::translateEnum);
        roundingMode.setValue(RoundingMode.NONE);

        TextArea freeText = new TextArea(i18n.translate("common.notes"));

        Span totalsSummary = new Span();

        pendingLineItems.clear();
        boolean editable = existing == null || existing.status() == DocumentStatus.DRAFT;
        if (existing != null) {
            documentType.setValue(existing.documentType());
            customers.stream().filter(c -> c.id().equals(existing.accountId())).findFirst().ifPresent(customer::setValue);
            documentDate.setValue(existing.documentDate());
            currency.setValue(existing.currency());
            vatType.setValue(existing.vatType());
            if (existing.discountType() != null) discountType.setValue(existing.discountType());
            discountValue.setValue(existing.discountValue() != null ? existing.discountValue().doubleValue() : 0.0);
            roundingMode.setValue(existing.roundingMode());
            freeText.setValue(existing.freeText() != null ? existing.freeText() : "");
            for (LineItemResponse item : existing.lineItems()) {
                pendingLineItems.add(new LineItemRequest(item.id(), item.productOrService(), item.description(),
                        item.quantity(), item.unitPrice(), item.sortOrder()));
            }
            totalsSummary.setText(i18n.translate("common.total") + ": " + existing.currency() + " " + existing.grossTotal()
                    + " (" + i18n.translate("common.net") + ": " + existing.netTotal()
                    + ", " + i18n.translate("common.vat") + ": " + existing.vatAmount() + ")");
        }
        documentType.setReadOnly(!editable);
        customer.setReadOnly(!editable);

        configureLineItemGrid();
        lineItemGrid.setItems(pendingLineItems);
        Button addItem = new Button(i18n.translate("common.addLineItem"), VaadinIcon.PLUS.create(), e -> openLineItemDialog());
        addItem.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addItem.setEnabled(editable);

        FormLayout form = new FormLayout(documentType, customer, documentDate, currency, vatType,
                discountType, discountValue, roundingMode, freeText);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));
        form.setColspan(freeText, 3);

        VerticalLayout content = new VerticalLayout(form, new H4(i18n.translate("common.lineItems")),
                addItem, lineItemGrid, totalsSummary);
        content.setPadding(false);
        if (existing != null) {
            content.add(buildPaymentsSection(existing));
        }
        dialog.add(content);

        Button saveDraft = new Button(i18n.translate("common.saveDraft"), e -> {
            TaxDocumentCreateRequest request = buildRequest(documentType, customer, currency, documentDate,
                    freeText, vatType, discountType, discountValue, roundingMode);
            if (request == null) return;
            try {
                if (existing == null) draftService.create(request);
                else draftService.update(existing.id(), request);
                refreshGrid();
                dialog.close();
                notify(i18n.translate("view.taxDocuments.notification.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        saveDraft.setEnabled(editable);
        saveDraft.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button(i18n.translate("common.cancel"), e -> dialog.close()), saveDraft);
        dialog.open();
    }

    private TaxDocumentCreateRequest buildRequest(ComboBox<DocumentType> documentType, ComboBox<CustomerQuickCreateResponse> customer,
                                                   TextField currency, DatePicker documentDate, TextArea freeText,
                                                   ComboBox<VatType> vatType, ComboBox<DiscountType> discountType,
                                                   NumberField discountValue, ComboBox<RoundingMode> roundingMode) {
        if (customer.getValue() == null) {
            customer.setInvalid(true);
            return null;
        }
        if (pendingLineItems.isEmpty()) {
            notify(i18n.translate("view.paymentRequests.error.noLineItems"), true);
            return null;
        }
        return new TaxDocumentCreateRequest(documentType.getValue(), customer.getValue().id(), currency.getValue(),
                documentDate.getValue(), freeText.getValue(), vatType.getValue(), discountType.getValue(),
                discountValue.getValue() != null ? BigDecimal.valueOf(discountValue.getValue()) : BigDecimal.ZERO,
                roundingMode.getValue(), new ArrayList<>(pendingLineItems));
    }

    private VerticalLayout buildPaymentsSection(TaxDocumentResponse doc) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.add(new H4(i18n.translate("view.taxDocuments.paymentsReceived")));

        Grid<com.crm.billing.dto.DocumentPaymentResponse> paymentsGrid = new Grid<>();
        paymentsGrid.addColumn(p -> i18n.translateEnum(p.paymentMethod())).setHeader(i18n.translate("common.paymentMethod"));
        paymentsGrid.addColumn(com.crm.billing.dto.DocumentPaymentResponse::amount).setHeader(i18n.translate("common.amount"));
        paymentsGrid.addColumn(p -> p.receivedAt() != null ? p.receivedAt().toString() : "").setHeader(i18n.translate("common.date"));
        paymentsGrid.setItems(doc.payments());
        paymentsGrid.setHeight("140px");

        ComboBox<PaymentMethod> method = new ComboBox<>(i18n.translate("common.paymentMethod"));
        method.setItems(PaymentMethod.values());
        method.setItemLabelGenerator(i18n::translateEnum);
        NumberField amount = new NumberField(i18n.translate("common.amount"));
        Button addPayment = new Button(i18n.translate("common.add"), e -> {
            if (method.getValue() == null || amount.getValue() == null) return;
            draftService.addPayment(doc.id(), new DocumentPaymentRequest(method.getValue(),
                    BigDecimal.valueOf(amount.getValue()), java.time.LocalDate.now()));
            paymentsGrid.setItems(draftService.findById(doc.id()).payments());
        });

        section.add(paymentsGrid, new HorizontalLayout(method, amount, addPayment));
        return section;
    }

    private void configureLineItemGrid() {
        lineItemGrid.removeAllColumns();
        lineItemGrid.addColumn(LineItemRequest::productOrService).setHeader(i18n.translate("common.product")).setFlexGrow(2);
        lineItemGrid.addColumn(LineItemRequest::quantity).setHeader(i18n.translate("common.qty"));
        lineItemGrid.addColumn(LineItemRequest::unitPrice).setHeader(i18n.translate("common.unitPrice"));
        lineItemGrid.addColumn(i -> i.quantity().multiply(i.unitPrice())).setHeader(i18n.translate("common.total"));
        lineItemGrid.addComponentColumn(item -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);
            Button duplicate = new Button(VaadinIcon.COPY.create(), e -> {
                pendingLineItems.add(new LineItemRequest(null, item.productOrService(), item.description(),
                        item.quantity(), item.unitPrice(), pendingLineItems.size()));
                lineItemGrid.getDataProvider().refreshAll();
            });
            duplicate.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button delete = new Button(VaadinIcon.TRASH.create(), e -> {
                pendingLineItems.remove(item);
                lineItemGrid.getDataProvider().refreshAll();
            });
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            actions.add(duplicate, delete);
            return actions;
        }).setHeader("").setFlexGrow(0).setWidth("100px");
        lineItemGrid.setHeight("200px");
    }

    private void openLineItemDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(i18n.translate("common.addLineItem"));
        dialog.setWidth("440px");

        TextField product = new TextField(i18n.translate("common.productOrService"));
        TextArea description = new TextArea(i18n.translate("common.description"));
        NumberField qty = new NumberField(i18n.translate("common.quantity"));
        qty.setValue(1.0);
        NumberField unitPrice = new NumberField(i18n.translate("common.unitPrice"));
        unitPrice.setValue(0.0);

        FormLayout form = new FormLayout(product, description, qty, unitPrice);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(product, 2);
        form.setColspan(description, 2);
        dialog.add(form);

        Button add = new Button(i18n.translate("common.add"), e -> {
            if (product.getValue().isBlank()) {
                product.setInvalid(true);
                return;
            }
            pendingLineItems.add(new LineItemRequest(null, product.getValue(), description.getValue(),
                    BigDecimal.valueOf(qty.getValue() != null ? qty.getValue() : 1.0),
                    BigDecimal.valueOf(unitPrice.getValue() != null ? unitPrice.getValue() : 0.0),
                    pendingLineItems.size()));
            lineItemGrid.getDataProvider().refreshAll();
            dialog.close();
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button(i18n.translate("common.cancel"), e -> dialog.close()), add);
        dialog.open();
    }

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}
