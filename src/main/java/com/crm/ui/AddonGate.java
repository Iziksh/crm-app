package com.crm.ui;

import com.crm.service.AddonService;
import com.crm.service.TranslationService;
import com.crm.service.UserService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import org.springframework.stereotype.Component;

/** Shared per-tenant enable/disable check for the Billing & Documents addon, following the exact
 * same {@code Addon}/{@code AddonService} pattern {@link MainLayout} and {@code TimeClockView}
 * already use for "Time Clock" — an {@code Addon} row named "Billing & Documents" attached to the
 * current user's linked Account. */
@Component
public class AddonGate {

    static final String ADDON_NAME = "Billing & Documents";

    private final SecurityService securityService;
    private final UserService userService;
    private final AddonService addonService;

    public AddonGate(SecurityService securityService, UserService userService, AddonService addonService) {
        this.securityService = securityService;
        this.userService = userService;
        this.addonService = addonService;
    }

    public boolean currentUserHasBillingDocuments() {
        boolean isAdminUser = securityService.hasRole("SUPER_ADMIN") || securityService.hasRole("ADMIN");
        if (isAdminUser) {
            return true;
        }
        Long accountId = userService.getAccountIdByUsername(securityService.getUsername()).orElse(null);
        return addonService.accountHasActiveAddon(accountId, ADDON_NAME);
    }

    public Div disabledNotice(TranslationService i18n) {
        Div notice = new Div();
        Span msg = new Span(i18n.translate("view.billing.error.noAddon"));
        msg.getStyle().set("color", "var(--lumo-error-text-color)");
        notice.add(msg);
        return notice;
    }
}
