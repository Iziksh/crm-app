package com.crm.ui;

import com.crm.service.DeviceTrustService;
import com.crm.service.EmailService;
import com.crm.service.LocaleService;
import com.crm.service.OtpService;
import com.crm.service.TranslationService;
import com.crm.service.UserService;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.servlet.http.Cookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@CssImport("./i18n.css")
@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final DeviceTrustService deviceTrustService;
    private final LocaleService localeService;
    private final TranslationService i18n;

    private final Span errorMsg = new Span();

    public LoginView(AuthenticationManager authenticationManager,
                     UserService userService,
                     OtpService otpService,
                     EmailService emailService,
                     DeviceTrustService deviceTrustService,
                     LocaleService localeService,
                     TranslationService i18n) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.deviceTrustService = deviceTrustService;
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

        H2 title = new H2(i18n.translate("app.name"));
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

        PasswordField passwordField = new PasswordField(i18n.translate("auth.password"));
        passwordField.setWidthFull();

        Button loginBtn = new Button(i18n.translate("auth.login"),
                e -> handleLogin(usernameField.getValue(), passwordField.getValue()));
        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginBtn.setWidthFull();

        passwordField.addKeyDownListener(com.vaadin.flow.component.Key.ENTER,
                ev -> handleLogin(usernameField.getValue(), passwordField.getValue()));

        card.add(langRow, title, errorMsg, usernameField, passwordField, loginBtn);
        add(card);
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("page.login");
    }

    private void handleLogin(String username, String password) {
        errorMsg.setVisible(false);
        if (username.isBlank() || password.isBlank()) {
            showError(i18n.translate("auth.usernamePasswordRequired"));
            return;
        }

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username.trim(), password));
        } catch (BadCredentialsException e) {
            showError(i18n.translate("auth.incorrectCredentials"));
            return;
        } catch (Exception e) {
            showError(i18n.translate("auth.loginFailed"));
            return;
        }

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String email = userService.findDeliverableEmailByUsername(userDetails.getUsername()).orElse(null);

        if (email == null) {
            showError(i18n.translate("auth.noEmail"));
            return;
        }

        // Check for a trusted device cookie
        String cookieValue = readCookie(deviceTrustService.getCookieName());
        if (cookieValue != null && deviceTrustService.isTokenValid(cookieValue, email)) {
            completeAuthentication(auth);
            return;
        }

        // No trusted device — send OTP and proceed to verification
        String otp = otpService.generateAndStore(email);
        emailService.sendOtp(email, otp, localeService.getCurrentLocale());

        VaadinSession.getCurrent().setAttribute("2fa_username", userDetails.getUsername());
        VaadinSession.getCurrent().setAttribute("2fa_email", email);

        getUI().ifPresent(ui -> ui.navigate("verify-otp"));
    }

    private void completeAuthentication(Authentication auth) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        VaadinSession.getCurrent().getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        getUI().ifPresent(ui -> ui.getPage().setLocation("/"));
    }

    private String readCookie(String name) {
        VaadinRequest request = VaadinService.getCurrentRequest();
        if (request instanceof VaadinServletRequest vsr) {
            Cookie[] cookies = vsr.getHttpServletRequest().getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if (name.equals(c.getName())) return c.getValue();
                }
            }
        }
        return null;
    }

    private void showError(String message) {
        errorMsg.setText(message);
        errorMsg.setVisible(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Clear any stale 2FA session data if navigating back to login
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute("2fa_username", null);
            session.setAttribute("2fa_email", null);
        }
    }
}
