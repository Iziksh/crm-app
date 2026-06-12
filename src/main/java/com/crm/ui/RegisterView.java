package com.crm.ui;

import com.crm.dto.request.RegisterRequest;
import com.crm.exception.DuplicateEmailException;
import com.crm.service.LocaleService;
import com.crm.service.TranslationService;
import com.crm.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@CssImport("./i18n.css")
@Route("register")
@AnonymousAllowed
public class RegisterView extends VerticalLayout implements HasDynamicTitle {

    private final UserService userService;
    private final LocaleService localeService;
    private final TranslationService i18n;
    private final Span errorMsg = new Span();

    public RegisterView(UserService userService, LocaleService localeService, TranslationService i18n) {
        this.userService = userService;
        this.localeService = localeService;
        this.i18n = i18n;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Div card = new Div();
        card.getStyle()
                .set("background", "#fff")
                .set("padding", "32px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,.15)")
                .set("min-width", "320px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px");

        H2 title = new H2(i18n.translate("auth.register"));
        title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextAlignment.CENTER);
        title.getStyle().set("color", "#1565c0");

        LanguageSwitcher languageSwitcher = new LanguageSwitcher(localeService, i18n, true);
        HorizontalLayout langRow = new HorizontalLayout(languageSwitcher);
        langRow.setWidthFull();
        langRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        errorMsg.getStyle().set("color", "#d32f2f").set("font-size", "14px");
        errorMsg.setVisible(false);

        TextField usernameField = new TextField(i18n.translate("auth.username"));
        usernameField.setWidthFull();
        usernameField.setAutofocus(true);

        EmailField emailField = new EmailField(i18n.translate("common.email"));
        emailField.setWidthFull();

        PasswordField passwordField = new PasswordField(i18n.translate("auth.password"));
        passwordField.setWidthFull();

        Button registerBtn = new Button(i18n.translate("auth.register"),
                e -> handleRegister(
                        usernameField.getValue(),
                        emailField.getValue(),
                        passwordField.getValue()));
        registerBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerBtn.setWidthFull();

        passwordField.addKeyDownListener(com.vaadin.flow.component.Key.ENTER,
                ev -> handleRegister(usernameField.getValue(), emailField.getValue(), passwordField.getValue()));

        RouterLink loginLink = new RouterLink(i18n.translate("auth.backToLogin"), LoginView.class);
        loginLink.getStyle()
                .set("font-size", "13px")
                .set("text-align", "center")
                .set("margin-top", "4px");

        card.add(langRow, title, errorMsg, usernameField, emailField, passwordField, registerBtn, loginLink);
        add(card);
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("page.register");
    }

    private void handleRegister(String username, String email, String password) {
        errorMsg.setVisible(false);
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            showError(i18n.translate("auth.usernamePasswordRequired"));
            return;
        }
        if (password.length() < 8) {
            showError(i18n.translate("auth.password"));
            return;
        }

        try {
            userService.register(new RegisterRequest(username.trim(), email.trim().toLowerCase(), password));
            Notification.show(i18n.translate("auth.register"), 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (DuplicateEmailException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String message) {
        errorMsg.setText(message);
        errorMsg.setVisible(true);
    }
}
