package com.crm.ui;

import com.crm.billing.dto.CustomerQuickCreateRequest;
import com.crm.billing.dto.CustomerQuickCreateResponse;
import com.crm.billing.service.CustomerQuickCreateService;
import com.crm.service.TranslationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Consumer;

/** Shared quick-create dialog for the billing addon's customer picker: שם מלא (required), ת"ז/ח"פ,
 * טלפון, מייל, "הצגת פרטים נוספים" toggle for address/industry/website, שמירה. Reuses the existing
 * Account entity (via CustomerQuickCreateService) — no new Customer entity. */
public class CustomerQuickCreateDialog extends Dialog {

    public CustomerQuickCreateDialog(CustomerQuickCreateService customerQuickCreateService,
                                      TranslationService i18n,
                                      Consumer<CustomerQuickCreateResponse> onCreated) {
        setHeaderTitle(i18n.translate("view.customerQuickCreate.title"));
        setWidth("480px");

        TextField name = new TextField(i18n.translate("common.fullName"));
        TextField taxId = new TextField(i18n.translate("common.taxId"));
        taxId.setPlaceholder("123456789");
        TextField phone = new TextField(i18n.translate("common.phone"));
        TextField email = new TextField(i18n.translate("common.email"));

        TextField address = new TextField(i18n.translate("common.address"));
        TextField industry = new TextField(i18n.translate("common.industry"));
        TextField website = new TextField(i18n.translate("common.website"));
        FormLayout moreDetails = new FormLayout(address, industry, website);
        moreDetails.setVisible(false);

        Button toggleMore = new Button(i18n.translate("view.customerQuickCreate.showMoreDetails"),
                e -> moreDetails.setVisible(!moreDetails.isVisible()));
        toggleMore.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        FormLayout form = new FormLayout(name, taxId, phone, email);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        add(form, toggleMore, moreDetails);

        Button save = new Button(i18n.translate("common.save"), e -> {
            if (name.getValue().isBlank()) {
                name.setInvalid(true);
                return;
            }
            CustomerQuickCreateRequest request = new CustomerQuickCreateRequest(
                    name.getValue(),
                    taxId.getValue().isBlank() ? null : taxId.getValue(),
                    phone.getValue().isBlank() ? null : phone.getValue(),
                    email.getValue().isBlank() ? null : email.getValue(),
                    address.getValue().isBlank() ? null : address.getValue(),
                    industry.getValue().isBlank() ? null : industry.getValue(),
                    website.getValue().isBlank() ? null : website.getValue());
            try {
                CustomerQuickCreateResponse created = customerQuickCreateService.create(request);
                onCreated.accept(created);
                close();
            } catch (Exception ex) {
                Notification n = Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(new Button(i18n.translate("common.cancel"), e -> close()), save);
    }
}
