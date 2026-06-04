package com.crm.ui;

import com.crm.service.DeviceTrustService;
import com.crm.service.EmailService;
import com.crm.service.OtpService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.servlet.http.Cookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Route("verify-otp")
@PageTitle("Verify Code | CRM")
@AnonymousAllowed
public class OtpVerificationView extends VerticalLayout implements BeforeEnterObserver {

    private final OtpService otpService;
    private final EmailService emailService;
    private final DeviceTrustService deviceTrustService;
    private final UserDetailsService userDetailsService;

    private final Span errorMsg = new Span();
    private String pendingUsername;
    private String pendingEmail;

    public OtpVerificationView(OtpService otpService,
                               EmailService emailService,
                               DeviceTrustService deviceTrustService,
                               UserDetailsService userDetailsService) {
        this.otpService = otpService;
        this.emailService = emailService;
        this.deviceTrustService = deviceTrustService;
        this.userDetailsService = userDetailsService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        VaadinSession session = VaadinSession.getCurrent();
        pendingUsername = (String) session.getAttribute("2fa_username");
        pendingEmail    = (String) session.getAttribute("2fa_email");

        if (pendingUsername == null) {
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

        H2 title = new H2("Verify your identity");
        title.addClassNames(LumoUtility.Margin.NONE);
        title.getStyle().set("color", "#1565c0");

        String maskedEmail = maskEmail(pendingEmail);
        Paragraph hint = new Paragraph("A 6-digit code was sent to " + maskedEmail + ". Enter it below.");
        hint.getStyle().set("color", "#555").set("font-size", "14px").set("margin", "0");

        errorMsg.getStyle().set("color", "#d32f2f").set("font-size", "14px");
        errorMsg.setVisible(false);

        TextField codeField = new TextField("Verification code");
        codeField.setWidthFull();
        codeField.setMaxLength(6);
        codeField.setAutofocus(true);
        codeField.getStyle().set("letter-spacing", "4px").set("font-size", "20px");

        Checkbox trustCheckbox = new Checkbox("Remember this device for " + deviceTrustService.getTrustDays() + " days");

        Button verifyBtn = new Button("Verify", e -> handleVerify(codeField.getValue(), trustCheckbox.getValue()));
        verifyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        verifyBtn.setWidthFull();

        codeField.addKeyDownListener(com.vaadin.flow.component.Key.ENTER,
                ev -> handleVerify(codeField.getValue(), trustCheckbox.getValue()));

        Button resendBtn = new Button("Resend code", e -> handleResend());
        resendBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        Button backBtn = new Button("Back to login", e -> UI.getCurrent().navigate(LoginView.class));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        HorizontalLayout links = new HorizontalLayout(resendBtn, backBtn);
        links.setWidthFull();
        links.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        card.add(title, hint, errorMsg, codeField, trustCheckbox, verifyBtn, links);
        add(card);
    }

    private void handleVerify(String code, boolean trustDevice) {
        errorMsg.setVisible(false);
        if (code == null || code.isBlank()) {
            showError("Please enter the verification code.");
            return;
        }

        if (!otpService.validate(pendingEmail, code.trim())) {
            showError("Invalid or expired code. Request a new one.");
            return;
        }

        // OTP valid — load user and complete authentication
        UserDetails userDetails = userDetailsService.loadUserByUsername(pendingUsername);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        if (trustDevice) {
            String rawToken = deviceTrustService.createTrustToken(pendingEmail);
            writeCookie(deviceTrustService.getCookieName(), rawToken,
                    deviceTrustService.getTrustDays() * 86400);
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        VaadinSession.getCurrent().getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        VaadinSession.getCurrent().setAttribute("2fa_username", null);
        VaadinSession.getCurrent().setAttribute("2fa_email", null);

        UI.getCurrent().getPage().setLocation("/");
    }

    private void handleResend() {
        if (pendingEmail != null) {
            String newOtp = otpService.generateAndStore(pendingEmail);
            emailService.sendOtp(pendingEmail, newOtp);
            errorMsg.setText("A new code has been sent.");
            errorMsg.getStyle().set("color", "#2e7d32");
            errorMsg.setVisible(true);
        }
    }

    private void writeCookie(String name, String value, int maxAgeSeconds) {
        VaadinResponse response = VaadinService.getCurrentResponse();
        if (response instanceof VaadinServletResponse servletResponse) {
            Cookie cookie = new Cookie(name, value);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(maxAgeSeconds);
            // cookie.setSecure(true); // enable when using HTTPS in production
            servletResponse.getHttpServletResponse().addCookie(cookie);
        }
    }

    private void showError(String message) {
        errorMsg.getStyle().set("color", "#d32f2f");
        errorMsg.setText(message);
        errorMsg.setVisible(true);
    }

    private String maskEmail(String email) {
        if (email == null) return "your email";
        int at = email.indexOf('@');
        if (at <= 1) return email;
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return local.charAt(0) + "***" + domain;
        return local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1) + domain;
    }
}
