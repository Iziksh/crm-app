package com.crm.ui;

import com.crm.domain.entity.User;
import com.crm.exception.InvitationInvalidException;
import com.crm.service.AdminUserManagementService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.List;

@Route("verify-invite")
@AnonymousAllowed
public class VerifyInviteView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

    private final AdminUserManagementService adminService;
    private final UserDetailsService userDetailsService;
    private final RoleHierarchy roleHierarchy;

    private final Span errorMsg = new Span();
    private String emailFromQuery = "";

    public VerifyInviteView(AdminUserManagementService adminService,
                             UserDetailsService userDetailsService,
                             RoleHierarchy roleHierarchy) {
        this.adminService = adminService;
        this.userDetailsService = userDetailsService;
        this.roleHierarchy = roleHierarchy;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public String getPageTitle() {
        return "Accept Invitation | CRM";
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        List<String> emails = event.getLocation().getQueryParameters()
                .getParameters().getOrDefault("email", List.of());
        emailFromQuery = emails.isEmpty() ? "" : emails.get(0);
        buildUi();
    }

    private void buildUi() {
        removeAll();
        Div card = buildCard();

        H2 title = new H2("Accept Your Invitation");
        title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextAlignment.CENTER);
        title.getStyle().set("color", "#1565c0");

        Paragraph hint = new Paragraph(
                "Enter the verification code from your invitation email and choose a password for your account.");
        hint.getStyle().set("color", "#666").set("font-size", "14px").set("margin", "0");

        errorMsg.getStyle().set("color", "#d32f2f").set("font-size", "14px");
        errorMsg.setVisible(false);

        TextField emailField = new TextField("Email");
        emailField.setWidthFull();
        emailField.setValue(emailFromQuery);
        emailField.setAutofocus(emailFromQuery.isBlank());

        TextField otpField = new TextField("Verification Code");
        otpField.setWidthFull();
        otpField.setMaxLength(6);
        otpField.getStyle().set("letter-spacing", "4px").set("font-size", "20px");
        if (!emailFromQuery.isBlank()) otpField.setAutofocus(true);

        PasswordField passwordField = new PasswordField("Choose a Password");
        passwordField.setWidthFull();
        passwordField.setHelperText("Minimum 8 characters");

        PasswordField confirmField = new PasswordField("Confirm Password");
        confirmField.setWidthFull();

        Button activateBtn = new Button("Activate Account", e -> handleVerify(
                emailField.getValue().trim(),
                otpField.getValue().trim(),
                passwordField.getValue(),
                confirmField.getValue()));
        activateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        activateBtn.setWidthFull();

        confirmField.addKeyDownListener(Key.ENTER, ev -> handleVerify(
                emailField.getValue().trim(),
                otpField.getValue().trim(),
                passwordField.getValue(),
                confirmField.getValue()));

        card.add(title, hint, errorMsg, emailField, otpField, passwordField, confirmField, activateBtn);
        add(card);
    }

    private void handleVerify(String email, String otp, String password, String confirm) {
        errorMsg.setVisible(false);

        if (email.isBlank() || otp.isBlank() || password.isBlank()) {
            showError("Email, verification code and password are required.");
            return;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        try {
            User activated = adminService.verifyInviteOtp(email, otp, password);
            signInAndRedirect(activated);
        } catch (InvitationInvalidException ex) {
            showError("Invalid or expired code. Contact your administrator for a new invitation.");
        } catch (Exception ex) {
            showError("Something went wrong. Please try again.");
        }
    }

    private void signInAndRedirect(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null,
                roleHierarchy.getReachableGrantedAuthorities(userDetails.getAuthorities()));
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
        VaadinSession.getCurrent().getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
        UI.getCurrent().getPage().setLocation("/");
    }

    private void showError(String message) {
        errorMsg.setText(message);
        errorMsg.setVisible(true);
    }

    private static Div buildCard() {
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
        return card;
    }
}
