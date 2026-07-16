package com.crm.ui;

import com.crm.billing.dto.PaymentRequestResponse;
import com.crm.billing.dto.ShareRequest;
import com.crm.billing.enums.DocumentOwnerType;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.enums.PaymentRequestStatus;
import com.crm.billing.service.ConversionService;
import com.crm.billing.service.DocumentShareService;
import com.crm.billing.service.PaymentRequestService;
import com.crm.domain.entity.Workspace;
import com.crm.domain.enums.WorkspaceTaxStatus;
import com.crm.service.TranslationService;
import com.crm.service.WorkspaceContext;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

/** The rendered דרישת תשלום screen: blue summary header, tax-status-gated action bar
 * (יצירת חשבון/קבלה, יצירת חשבון עסקה, אפשרויות נוספות), left share panel
 * (email/WhatsApp/print/download), RTL preview. */
@Route(value = "payment-request-document", layout = MainLayout.class)
@RolesAllowed({"SALES", "ADMIN"})
public class PaymentRequestDocumentView extends VerticalLayout implements HasUrlParameter<Long>, HasDynamicTitle {

    private final TranslationService i18n;
    private final PaymentRequestService paymentRequestService;
    private final ConversionService conversionService;
    private final DocumentShareService shareService;
    private final WorkspaceContext workspaceContext;
    private final AddonGate addonGate;

    private PaymentRequestResponse current;
    private final Div content = new Div();

    public PaymentRequestDocumentView(PaymentRequestService paymentRequestService,
                                       ConversionService conversionService,
                                       DocumentShareService shareService,
                                       WorkspaceContext workspaceContext,
                                       AddonGate addonGate,
                                       TranslationService i18n) {
        this.paymentRequestService = paymentRequestService;
        this.conversionService = conversionService;
        this.shareService = shareService;
        this.workspaceContext = workspaceContext;
        this.addonGate = addonGate;
        this.i18n = i18n;
        setSizeFull();
        setPadding(true);
        add(content);
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("view.paymentRequestDocument.title");
    }

    @Override
    public void setParameter(BeforeEvent event, Long paymentRequestId) {
        if (!addonGate.currentUserHasBillingDocuments()) {
            content.removeAll();
            content.add(addonGate.disabledNotice(i18n));
            return;
        }
        this.current = paymentRequestService.findById(paymentRequestId);
        render();
    }

    private void render() {
        content.removeAll();
        content.getElement().setAttribute("dir", "rtl");

        Div header = new Div();
        header.getStyle()
                .set("background", "var(--lumo-primary-color)")
                .set("color", "var(--lumo-primary-contrast-color)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-inline-end", "0");
        header.add(new Span(i18n.translate("common.customer") + ": " + current.accountName()));
        header.add(new com.vaadin.flow.component.html.Hr());
        header.add(new Span(i18n.translate("view.paymentRequestDocument.documentAmount") + ": "
                + current.currency() + " " + current.totalAmount()));
        header.add(new com.vaadin.flow.component.html.Hr());
        header.add(new Span(i18n.translate("view.paymentRequestDocument.documentDate") + ": " + current.documentDate()));
        header.add(new com.vaadin.flow.component.html.Hr());
        Span statusBadge = new Span(statusLabel(current.status()));
        statusBadge.getElement().getThemeList().add("badge contrast");
        header.add(statusBadge);

        HorizontalLayout actionBar = buildActionBar();
        HorizontalLayout sharePanel = buildSharePanel();

        VerticalLayout preview = new VerticalLayout(new H3(i18n.translate("view.paymentRequestDocument.previewTitle")));
        current.lineItems().forEach(item -> preview.add(new Span(
                item.productOrService() + " — " + item.quantity() + " x " + item.unitPrice()
                        + " = " + item.lineTotal())));
        preview.add(new com.vaadin.flow.component.html.Hr());
        preview.add(new Span("_________________________"));
        preview.add(new Span(i18n.translate("view.paymentRequestDocument.signature")));
        preview.add(new Span(i18n.translate("view.paymentRequestDocument.employee")));

        HorizontalLayout body = new HorizontalLayout(sharePanel, preview);
        body.setWidthFull();

        content.add(header, actionBar, body);
    }

    private String statusLabel(PaymentRequestStatus status) {
        return switch (status) {
            case OPEN -> i18n.translate("view.paymentRequestDocument.status.open");
            case CONVERTED -> i18n.translate("view.paymentRequestDocument.status.converted");
            case CANCELLED -> i18n.translate("view.paymentRequestDocument.status.cancelled");
        };
    }

    private HorizontalLayout buildActionBar() {
        HorizontalLayout bar = new HorizontalLayout();
        if (current.status() != PaymentRequestStatus.OPEN) {
            return bar;
        }
        Workspace workspace = workspaceContext.currentUserPrimaryWorkspace().orElse(null);
        WorkspaceTaxStatus taxStatus = workspace != null ? workspace.getTaxStatus() : null;
        if (taxStatus == null) {
            bar.add(new Span(i18n.translate("view.paymentRequestDocument.noTaxStatus")));
            return bar;
        }

        DocumentType invoiceReceiptTarget = taxStatus == WorkspaceTaxStatus.EXEMPT_DEALER
                ? DocumentType.RECEIPT : DocumentType.TAX_INVOICE_RECEIPT;
        Button createInvoiceReceipt = new Button(i18n.translate("view.paymentRequestDocument.createInvoiceReceipt"),
                e -> convert(invoiceReceiptTarget));
        createInvoiceReceipt.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        bar.add(createInvoiceReceipt);

        if (taxStatus != WorkspaceTaxStatus.EXEMPT_DEALER) {
            Button createProforma = new Button(i18n.translate("view.paymentRequestDocument.createProforma"),
                    e -> convert(DocumentType.PROFORMA));
            bar.add(createProforma);
        }

        MenuBar moreOptions = new MenuBar();
        MenuItem more = moreOptions.addItem(i18n.translate("view.paymentRequestDocument.moreOptions"));
        more.getSubMenu().addItem(i18n.translate("view.paymentRequests.cancelTitle"), e -> {
            paymentRequestService.cancel(current.id());
            current = paymentRequestService.findById(current.id());
            render();
        });
        bar.add(moreOptions);

        return bar;
    }

    private void convert(DocumentType targetType) {
        try {
            conversionService.convert(current.id(), targetType);
            notify(i18n.translate("view.paymentRequestDocument.converted"), false);
            String slug = workspaceContext.currentWorkspaceSlug();
            getUI().ifPresent(ui -> ui.navigate(slug + "/tax-documents"));
        } catch (Exception ex) {
            notify(ex.getMessage(), true);
        }
    }

    private HorizontalLayout buildSharePanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setWidth("180px");
        panel.setPadding(false);

        Button email = new Button(i18n.translate("view.share.email"), VaadinIcon.ENVELOPE.create(), e -> openEmailDialog());
        Button whatsapp = new Button(i18n.translate("view.share.whatsapp"), VaadinIcon.COMMENT.create(), e -> openWhatsAppDialog());
        Anchor print = pdfAnchor(i18n.translate("view.share.print"), false);
        Anchor download = pdfAnchor(i18n.translate("view.share.download"), true);

        panel.add(email, whatsapp, print, download);
        return new HorizontalLayout(panel);
    }

    private Anchor pdfAnchor(String label, boolean download) {
        StreamResource resource = new StreamResource("payment-request-" + current.id() + ".pdf",
                () -> new java.io.ByteArrayInputStream(shareService.renderPdf(DocumentOwnerType.PAYMENT_REQUEST, current.id())));
        resource.setContentType("application/pdf");
        Anchor anchor = new Anchor(resource, label);
        anchor.setTarget("_blank");
        if (download) {
            anchor.getElement().setAttribute("download", true);
        }
        anchor.getElement().getThemeList().add("button");
        return anchor;
    }

    private void openEmailDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(i18n.translate("view.share.email"));
        TextField emailField = new TextField(i18n.translate("common.email"));
        dialog.add(emailField);
        Button send = new Button(i18n.translate("view.share.send"), e -> {
            try {
                shareService.share(new ShareRequest(DocumentOwnerType.PAYMENT_REQUEST, current.id(),
                        ShareRequest.ShareChannel.EMAIL, emailField.getValue()));
                notify(i18n.translate("view.share.sent"), false);
                dialog.close();
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button(i18n.translate("common.cancel"), e -> dialog.close()), send);
        dialog.open();
    }

    private void openWhatsAppDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(i18n.translate("view.share.whatsapp"));
        TextField phoneField = new TextField(i18n.translate("common.phone"));
        dialog.add(phoneField);
        Button open = new Button(i18n.translate("view.share.open"), e -> {
            String link = shareService.buildWhatsAppLink(DocumentOwnerType.PAYMENT_REQUEST, current.id(), phoneField.getValue());
            getUI().ifPresent(ui -> ui.getPage().open(link, "_blank"));
            dialog.close();
        });
        open.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button(i18n.translate("common.cancel"), e -> dialog.close()), open);
        dialog.open();
    }

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}
