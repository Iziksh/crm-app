package com.crm.ui;

import com.crm.billing.dto.CustomerQuickCreateResponse;
import com.crm.billing.dto.LineItemRequest;
import com.crm.billing.dto.LineItemResponse;
import com.crm.billing.dto.PaymentRequestCreateRequest;
import com.crm.billing.dto.PaymentRequestResponse;
import com.crm.billing.enums.PaymentRequestStatus;
import com.crm.billing.service.CustomerQuickCreateService;
import com.crm.billing.service.PaymentRequestService;
import com.crm.service.TranslationService;
import com.crm.service.WorkspaceContext;
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

/** דרישת תשלום editor — customer picker + quick-create, date, currency, free text, shared line-item
 * grid, live total. RTL Hebrew via the existing i18n/messages_he.properties infrastructure. */
@Route(value = "payment-requests", layout = MainLayout.class)
@RolesAllowed({"SALES", "ADMIN"})
public class PaymentRequestEditorView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final PaymentRequestService paymentRequestService;
    private final CustomerQuickCreateService customerQuickCreateService;
    private final AddonGate addonGate;
    private final WorkspaceContext workspaceContext;

    private final Grid<PaymentRequestResponse> grid = new Grid<>(PaymentRequestResponse.class, false);
    private final List<LineItemRequest> pendingLineItems = new ArrayList<>();
    private final Grid<LineItemRequest> lineItemGrid = new Grid<>(LineItemRequest.class, false);

    public PaymentRequestEditorView(PaymentRequestService paymentRequestService,
                                     CustomerQuickCreateService customerQuickCreateService,
                                     AddonGate addonGate,
                                     WorkspaceContext workspaceContext,
                                     TranslationService i18n) {
        this.paymentRequestService = paymentRequestService;
        this.customerQuickCreateService = customerQuickCreateService;
        this.addonGate = addonGate;
        this.workspaceContext = workspaceContext;
        this.i18n = i18n;
        setSizeFull();
        setPadding(true);

        if (!addonGate.currentUserHasBillingDocuments()) {
            add(addonGate.disabledNotice(i18n));
            return;
        }

        configureGrid();

        Button addBtn = new Button(i18n.translate("view.paymentRequests.new"), VaadinIcon.PLUS.create(), e -> openDialog());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new H2(i18n.translate("view.paymentRequests.title")), addBtn, grid);
        setFlexGrow(1, grid);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("view.paymentRequests.title");
    }

    private void configureGrid() {
        grid.setHeight("500px");
        grid.addColumn(PaymentRequestResponse::number).setHeader(i18n.translate("common.number")).setFlexGrow(0).setWidth("110px");
        grid.addColumn(PaymentRequestResponse::accountName).setHeader(i18n.translate("common.customer")).setSortable(true);
        grid.addColumn(pr -> pr.documentDate() != null ? pr.documentDate().toString() : "").setHeader(i18n.translate("common.date"));
        grid.addColumn(pr -> pr.currency() + " " + pr.totalAmount()).setHeader(i18n.translate("common.total"));
        grid.addComponentColumn(this::statusBadge).setHeader(i18n.translate("common.status")).setFlexGrow(0).setWidth("120px");
        grid.addComponentColumn(pr -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            Button view = new Button(VaadinIcon.EYE.create(), e -> navigateToDocument(pr.id()));
            view.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            actions.add(view);

            if (pr.status() == PaymentRequestStatus.OPEN) {
                Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(pr));
                edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                Button cancel = new Button(VaadinIcon.BAN.create(), e -> confirmCancel(pr));
                cancel.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                actions.add(edit, cancel);
            }
            return actions;
        }).setHeader(i18n.translate("common.actions")).setFlexGrow(0).setWidth("160px");
    }

    private void navigateToDocument(Long id) {
        String slug = workspaceContext.currentWorkspaceSlug();
        getUI().ifPresent(ui -> ui.navigate(slug + "/payment-request-document/" + id));
    }

    private void refreshGrid() {
        grid.setItems(paymentRequestService.findAll(PageRequest.of(0, 200)).getContent());
    }

    private Span statusBadge(PaymentRequestResponse pr) {
        Span badge = new Span(i18n.translateEnum(pr.status()));
        String theme = switch (pr.status()) {
            case OPEN -> "badge primary";
            case CONVERTED -> "badge success";
            case CANCELLED -> "badge error";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    private void confirmCancel(PaymentRequestResponse pr) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(i18n.translate("view.paymentRequests.cancelTitle"));
        confirm.setText(i18n.translate("dialog.cancelConfirm", pr.number()));
        confirm.setConfirmText(i18n.translate("common.confirm"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            try {
                paymentRequestService.cancel(pr.id());
                refreshGrid();
                notify(i18n.translate("view.paymentRequests.notification.cancelled"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        confirm.open();
    }

    private void openDialog() {
        openDialog(null);
    }

    private void openDialog(PaymentRequestResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.paymentRequests.new")
                : i18n.translate("view.paymentRequests.edit"));
        dialog.setWidth("700px");

        List<CustomerQuickCreateResponse> customers = customerQuickCreateService.search(null);
        ComboBox<CustomerQuickCreateResponse> customer = new ComboBox<>(i18n.translate("common.customer"));
        customer.setItems(customers);
        customer.setItemLabelGenerator(CustomerQuickCreateResponse::name);
        customer.setWidthFull();
        Button addCustomer = new Button(VaadinIcon.PLUS_CIRCLE.create(), e ->
                new CustomerQuickCreateDialog(customerQuickCreateService, i18n, created -> {
                    customer.setItems(customerQuickCreateService.search(null));
                    customer.setValue(created);
                }).open());
        addCustomer.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        DatePicker documentDate = new DatePicker(i18n.translate("common.date"));
        documentDate.setValue(java.time.LocalDate.now());
        TextField currency = new TextField(i18n.translate("common.currency"));
        currency.setValue("ILS");
        TextArea freeText = new TextArea(i18n.translate("common.notes"));

        pendingLineItems.clear();
        if (existing != null) {
            customers.stream().filter(c -> c.id().equals(existing.accountId())).findFirst().ifPresent(customer::setValue);
            documentDate.setValue(existing.documentDate());
            currency.setValue(existing.currency());
            freeText.setValue(existing.freeText() != null ? existing.freeText() : "");
            for (LineItemResponse item : existing.lineItems()) {
                pendingLineItems.add(new LineItemRequest(item.id(), item.productOrService(), item.description(),
                        item.quantity(), item.unitPrice(), item.sortOrder()));
            }
        }

        configureLineItemGrid();
        lineItemGrid.setItems(pendingLineItems);
        Button addItem = new Button(i18n.translate("common.addLineItem"), VaadinIcon.PLUS.create(),
                e -> openLineItemDialog(lineItemGrid));
        addItem.addThemeVariants(ButtonVariant.LUMO_SMALL);

        FormLayout form = new FormLayout(new HorizontalLayout(customer, addCustomer), documentDate, currency, freeText);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(freeText, 2);

        VerticalLayout content = new VerticalLayout(form, new H4(i18n.translate("common.lineItems")), addItem, lineItemGrid);
        content.setPadding(false);
        dialog.add(content);

        Button save = new Button(i18n.translate("common.save"), e -> {
            if (customer.getValue() == null) {
                customer.setInvalid(true);
                return;
            }
            if (pendingLineItems.isEmpty()) {
                notify(i18n.translate("view.paymentRequests.error.noLineItems"), true);
                return;
            }
            PaymentRequestCreateRequest request = new PaymentRequestCreateRequest(
                    customer.getValue().id(), currency.getValue(), documentDate.getValue(),
                    freeText.getValue(), new ArrayList<>(pendingLineItems));
            try {
                if (existing == null) paymentRequestService.create(request);
                else paymentRequestService.update(existing.id(), request);
                refreshGrid();
                dialog.close();
                notify(i18n.translate("view.paymentRequests.notification.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button(i18n.translate("common.cancel"), e -> dialog.close()), save);
        dialog.open();
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
        lineItemGrid.setHeight("220px");
    }

    private void openLineItemDialog(Grid<LineItemRequest> targetGrid) {
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
            targetGrid.getDataProvider().refreshAll();
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
