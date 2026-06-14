package com.crm.ui;

import com.crm.domain.entity.User;
import com.crm.service.EmailService;
import com.crm.service.LocaleService;
import com.crm.service.OtpService;
import com.crm.service.RegistrationService;
import com.crm.service.TranslationService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@CssImport("./i18n.css")
@Route("verify-registration")
@AnonymousAllowed
public class VerifyRegistrationView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

    private final RegistrationService registrationService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final UserDetailsService userDetailsService;
    private final LocaleService localeService;
    private final TranslationService i18n;

    private String pendingEmail;
    private final Span errorMsg = new Span();

    public VerifyRegistrationView(RegistrationService registrationService,
                                   OtpService otpService,
                                   EmailService emailService,
                                   UserDetailsService userDetailsService,
                                   LocaleService localeService,
                                   TranslationService i18n) {
        this.registrationService = registrationService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.userDetailsService = userDetailsService;
        this.localeService = localeService;
        this.i18n = i18n;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("page.verifyRegistration");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        VaadinSession session = VaadinSession.getCurrent();
        pendingEmail = (String) session.getAttribute("reg_email");
        if (pendingEmail == null) {
            event.rerouteTo(LoginView.class);
            return;
        }
        buildUi();
    }

    private void buildUi() {
        removeAll();

        Div card = new Div();
        card.getStyle()
                .set("background", "#fff")
                .set("padding", "32px")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,.15)")
                .set("min-width", "340px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "12px");

        LanguageSwitcher languageSwitcher = new LanguageSwitcher(localeService, i18n, true);
        HorizontalLayout langRow = new HorizontalLayout(languageSwitcher);
        langRow.setWidthFull();
        langRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        H2 title = new H2(i18n.translate("register.verifyTitle"));
        title.addClassNames(LumoUtility.Margin.NONE);
        title.getStyle().set("color", "#1565c0");

        Paragraph hint = new Paragraph(i18n.translate("auth.otpSent", maskEmail(pendingEmail)));
        hint.getStyle().set("color", "#555").set("font-size", "14px").set("margin", "0");

        errorMsg.getStyle().set("color", "#d32f2f").set("font-size", "14px");
        errorMsg.setVisible(false);

        TextField codeField = new TextField(i18n.translate("auth.verificationCode"));
        codeField.setWidthFull();
        codeField.setMaxLength(6);
        codeField.setAutofocus(true);
        codeField.getStyle().set("letter-spacing", "4px").set("font-size", "20px");

        Button verifyBtn = new Button(i18n.translate("auth.verify"),
                e -> handleVerify(codeField.getValue()));
        verifyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        verifyBtn.setWidthFull();

        codeField.addKeyDownListener(Key.ENTER, ev -> handleVerify(codeField.getValue()));

        Button resendBtn = new Button(i18n.translate("auth.resendCode"), e -> handleResend());
        resendBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        Button backBtn = new Button(i18n.translate("auth.backToLogin"),
                e -> UI.getCurrent().navigate(LoginView.class));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        HorizontalLayout links = new HorizontalLayout(resendBtn, backBtn);
        links.setWidthFull();
        links.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        card.add(langRow, title, hint, errorMsg, codeField, verifyBtn, links);
        add(card);
    }

    private void handleVerify(String code) {
        errorMsg.setVisible(false);
        if (code == null || code.isBlank()) {
            showError(i18n.translate("auth.codeRequired"));
            return;
        }

        User user = registrationService.completeRegistration(pendingEmail, code.trim());
        if (user == null) {
            showError(i18n.translate("auth.invalidCode"));
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        VaadinSession.getCurrent().getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        localeService.persistLocaleForAuthenticatedUser(localeService.getCurrentLocale());
        VaadinSession.getCurrent().setAttribute("reg_email", null);

        UI.getCurrent().getPage().setLocation("/");
    }

    private void handleResend() {
        if (pendingEmail != null) {
            String newOtp = otpService.generateAndStore(pendingEmail);
            emailService.sendOtp(pendingEmail, newOtp, localeService.getCurrentLocale());
            errorMsg.getStyle().set("color", "#2e7d32");
            errorMsg.setText(i18n.translate("auth.newCodeSent"));
            errorMsg.setVisible(true);
        }
    }

    private void showError(String message) {
        errorMsg.getStyle().set("color", "#d32f2f");
        errorMsg.setText(message);
        errorMsg.setVisible(true);
    }

    private String maskEmail(String email) {
        if (email == null) return i18n.translate("auth.yourEmail");
        int at = email.indexOf('@');
        if (at <= 1) return email;
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return local.charAt(0) + "***" + domain;
        return local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1) + domain;
    }
}