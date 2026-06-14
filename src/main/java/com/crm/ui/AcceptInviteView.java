package com.crm.ui;

import com.crm.exception.InvitationInvalidException;
import com.crm.service.InvitationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route("accept-invite")
@AnonymousAllowed
public class AcceptInviteView extends VerticalLayout implements HasUrlParameter<String> {

    private final InvitationService invitationService;

    public AcceptInviteView(InvitationService invitationService) {
        this.invitationService = invitationService;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String token) {
        removeAll();
        if (token == null || token.isBlank()) {
            showNoTokenState();
            return;
        }
        buildForm(token);
    }

    private void buildForm(String token) {
        Div card = buildCard();

        H2 title = new H2("Create Your Account");
        title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextAlignment.CENTER);
        title.getStyle().set("color", "#1565c0");

        Paragraph hint = new Paragraph("Complete your account setup to accept the invitation.");
        hint.getStyle().set("color", "#666").set("font-size", "14px").set("margin", "0");

        TextField usernameField = new TextField("Username");
        usernameField.setWidthFull();
        usernameField.setMinLength(3);
        usernameField.setMaxLength(50);
        usernameField.setAutofocus(true);

        PasswordField passwordField = new PasswordField("Password");
        passwordField.setWidthFull();
        passwordField.setHelperText("Minimum 12 characters");

        PasswordField confirmField = new PasswordField("Confirm Password");
        confirmField.setWidthFull();

        Span errorMsg = new Span();
        errorMsg.getStyle().set("color", "#d32f2f").set("font-size", "14px");
        errorMsg.setVisible(false);

        Button submitBtn = new Button("Accept Invitation", e -> {
            errorMsg.setVisible(false);

            String username = usernameField.getValue().trim();
            String password = passwordField.getValue();
            String confirm = confirmField.getValue();

            if (username.isBlank()) {
                usernameField.setInvalid(true);
                return;
            }
            if (password.length() < 12) {
                errorMsg.setText("Password must be at least 12 characters.");
                errorMsg.setVisible(true);
                return;
            }
            if (!password.equals(confirm)) {
                errorMsg.setText("Passwords do not match.");
                errorMsg.setVisible(true);
                return;
            }

            try {
                invitationService.acceptInvitation(token, username, password);
                removeAll();
                showSuccessState();
            } catch (InvitationInvalidException ex) {
                showInvalidState();
            } catch (Exception ex) {
                errorMsg.setText("Invalid or expired invitation.");
                errorMsg.setVisible(true);
            }
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitBtn.setWidthFull();

        card.add(title, hint, errorMsg, usernameField, passwordField, confirmField, submitBtn);
        add(card);
    }

    private void showSuccessState() {
        Div card = buildCard();
        H2 title = new H2("Account Created");
        title.getStyle().set("color", "#2e7d32");
        title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextAlignment.CENTER);
        Paragraph msg = new Paragraph("Your account has been created successfully. You can now log in.");
        msg.getStyle().set("color", "#555").set("font-size", "14px");
        Button loginBtn = new Button("Go to Login",
                e -> getUI().ifPresent(ui -> ui.navigate("login")));
        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginBtn.setWidthFull();
        card.add(title, msg, loginBtn);
        add(card);
    }

    private void showNoTokenState() {
        Div card = buildCard();
        H2 title = new H2("No Invitation Token");
        title.getStyle().set("color", "#1565c0");
        title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextAlignment.CENTER);
        Paragraph msg = new Paragraph(
                "This page requires a valid invitation link. "
                + "If you'd like to create an account, you can register directly.");
        msg.getStyle().set("color", "#555").set("font-size", "14px");
        RouterLink registerLink = new RouterLink("Create an Account", RegisterView.class);
        registerLink.getStyle().set("font-size", "14px").set("text-align", "center");
        RouterLink backLink = new RouterLink("Back to Login", LoginView.class);
        backLink.getStyle().set("font-size", "13px").set("text-align", "center");
        card.add(title, msg, registerLink, backLink);
        add(card);
    }

    private void showInvalidState() {
        Div card = buildCard();
        H2 title = new H2("Invalid or Expired Invitation");
        title.getStyle().set("color", "#b71c1c");
        title.addClassNames(LumoUtility.Margin.NONE, LumoUtility.TextAlignment.CENTER);
        Paragraph msg = new Paragraph(
                "This invitation link is invalid, has already been used, or has expired. "
                + "Please contact your administrator for a new invitation.");
        msg.getStyle().set("color", "#555").set("font-size", "14px");
        card.add(title, msg);
        add(card);
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