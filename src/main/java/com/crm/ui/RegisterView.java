package com.crm.ui;

import com.crm.exception.DuplicateEmailException;
import com.crm.service.LocaleService;
import com.crm.service.RegistrationService;
import com.crm.service.TranslationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@CssImport("./i18n.css")
@Route("register")
@AnonymousAllowed
public class RegisterView extends VerticalLayout implements HasDynamicTitle {

    private final RegistrationService registrationService;
    private final LocaleService localeService;
    private final TranslationService i18n;

    public RegisterView(RegistrationService registrationService,
                        LocaleService localeService,
                        TranslationService i18n) {
        this.registrationService = registrationService;
        this.localeService = localeService;
        this.i18n = i18n;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        buildUi();
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("page.register");
    }

    private void buildUi() {
        Div card = new Div();
        card.getStyle()
                .set("background", "#fff")
                .set("padding", "32px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,.15)")
                .set("min-width", "340px")
                .set("max-width", "420px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px");

        LanguageSwitcher languageSwitcher = new LanguageSwitcher(localeService, i18n, true);
        HorizontalLayout langRow = new HorizontalLayout(languageSwitcher);
        langRow.setWidthFull();
        langRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        H2 title = new H2(i18n.translate("register.title"));
        title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextAlignment.CENTER);
        title.getStyle().set("color", "#1565c0");

        Span errorMsg = new Span();
        errorMsg.getStyle().set("color", "#d32f2f").set("font-size", "14px");
        errorMsg.setVisible(false);

        TextField usernameField = new TextField(i18n.translate("register.username"));
        usernameField.setWidthFull();
        usernameField.setMinLength(3);
        usernameField.setMaxLength(50);
        usernameField.setAutofocus(true);

        TextField emailField = new TextField(i18n.translate("common.email"));
        emailField.setWidthFull();

        PasswordField passwordField = new PasswordField(i18n.translate("auth.password"));
        passwordField.setWidthFull();
        passwordField.setHelperText(i18n.translate("register.passwordHint"));

        PasswordField confirmField = new PasswordField(i18n.translate("register.confirmPassword"));
        confirmField.setWidthFull();

        TextField companyField = new TextField(i18n.translate("register.companyName"));
        companyField.setWidthFull();

        Button submitBtn = new Button(i18n.translate("register.submit"), e -> {
            errorMsg.setVisible(false);
            String username = usernameField.getValue().trim();
            String email    = emailField.getValue().trim();
            String password = passwordField.getValue();
            String confirm  = confirmField.getValue();
            String company  = companyField.getValue().trim();

            if (username.isBlank() || email.isBlank() || password.isBlank() || company.isBlank()) {
                showError(errorMsg, i18n.translate("register.allFieldsRequired"));
                return;
            }
            if (!email.contains("@") || !email.contains(".")) {
                showError(errorMsg, i18n.translate("register.invalidEmail"));
                return;
            }
            if (password.length() < 8) {
                showError(errorMsg, i18n.translate("register.passwordTooShort"));
                return;
            }
            if (!password.equals(confirm)) {
                showError(errorMsg, i18n.translate("register.passwordMismatch"));
                return;
            }

            try {
                registrationService.startRegistration(
                        username, email, password, company, localeService.getCurrentLocale());
                VaadinSession.getCurrent().setAttribute("reg_email", email.toLowerCase());
                UI.getCurrent().navigate("verify-registration");
            } catch (DuplicateEmailException ex) {
                showError(errorMsg, i18n.translate("register.duplicateEmailOrUsername"));
            } catch (Exception ex) {
                showError(errorMsg, i18n.translate("register.error"));
            }
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitBtn.setWidthFull();

        RouterLink backLink = new RouterLink(i18n.translate("auth.backToLogin"), LoginView.class);
        backLink.getStyle().set("font-size", "13px").set("text-align", "center");

        card.add(langRow, title, errorMsg, usernameField, emailField,
                passwordField, confirmField, companyField, submitBtn, backLink);
        add(card);
    }

    private void showError(Span errorMsg, String message) {
        errorMsg.setText(message);
        errorMsg.setVisible(true);
    }
}