package com.crm.ui;

import com.crm.repository.UserRepository;
import com.crm.service.TranslationService;
import com.crm.service.WorkspaceContext;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Route(value = "my-profile", layout = MainLayout.class)
@PermitAll
public class MyProfileView extends VerticalLayout implements HasDynamicTitle {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private static final Map<String, String> ROLE_LABELS = Map.of(
            "ROLE_SUPER_ADMIN",   "Super Admin",
            "ROLE_COMPANY_ADMIN", "Company Admin",
            "ROLE_ADMIN",         "Admin",
            "ROLE_SALES",         "Sales",
            "ROLE_SUPPORT",       "Support",
            "ROLE_USER",          "User"
    );

    private static final Map<String, String> ROLE_BADGE_THEME = Map.of(
            "ROLE_SUPER_ADMIN",   "badge error",
            "ROLE_COMPANY_ADMIN", "badge contrast",
            "ROLE_ADMIN",         "badge",
            "ROLE_SALES",         "badge success",
            "ROLE_SUPPORT",       "badge",
            "ROLE_USER",          "badge contrast"
    );

    private final TranslationService i18n;

    public MyProfileView(UserRepository userRepository,
                         WorkspaceContext workspaceContext,
                         TranslationService i18n) {
        this.i18n = i18n;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setPadding(true);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        userRepository.findByUsername(username).ifPresent(user -> {
            // ── Card wrapper ─────────────────────────────────────────────
            Div card = new Div();
            card.getStyle()
                    .set("background", "var(--lumo-base-color)")
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("border-radius", "12px")
                    .set("padding", "32px 40px")
                    .set("max-width", "560px")
                    .set("width", "100%")
                    .set("box-shadow", "0 2px 8px rgba(0,0,0,.06)");

            // ── Avatar + username ────────────────────────────────────────
            Div avatar = new Div();
            avatar.getStyle()
                    .set("width", "72px").set("height", "72px")
                    .set("border-radius", "50%")
                    .set("background", "#1565c0")
                    .set("display", "flex").set("align-items", "center").set("justify-content", "center")
                    .set("color", "white").set("font-size", "28px").set("font-weight", "700")
                    .set("flex-shrink", "0");
            avatar.setText(username.substring(0, 1).toUpperCase());

            H2 usernameLabel = new H2(user.getUsername());
            usernameLabel.getStyle().set("margin", "0").set("font-size", "22px");

            Span statusBadge = statusBadge(user.getStatus() != null ? user.getStatus().name() : "ACTIVE");

            VerticalLayout nameCol = new VerticalLayout(usernameLabel, statusBadge);
            nameCol.setPadding(false);
            nameCol.setSpacing(false);
            nameCol.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

            HorizontalLayout header = new HorizontalLayout(avatar, nameCol);
            header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            header.setSpacing(true);
            header.setWidthFull();

            // ── Divider ──────────────────────────────────────────────────
            Div divider = new Div();
            divider.getStyle()
                    .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                    .set("margin", "20px 0");

            // ── Details section ──────────────────────────────────────────
            VerticalLayout details = new VerticalLayout();
            details.setPadding(false);
            details.setSpacing(false);
            details.getStyle().set("gap", "12px");

            details.add(infoRow(VaadinIcon.ENVELOPE, "Email", user.getEmail()));

            workspaceContext.currentUserPrimaryWorkspace().ifPresent(ws ->
                    details.add(infoRow(VaadinIcon.BUILDING, "Workspace", ws.getName() + " (" + ws.getSlug() + ")"))
            );

            if (user.getCreatedAt() != null) {
                details.add(infoRow(VaadinIcon.CALENDAR, "Member Since", user.getCreatedAt().format(DATE_FMT)));
            }

            // ── Roles section ────────────────────────────────────────────
            Div rolesSection = new Div();
            rolesSection.getStyle().set("margin-top", "20px");

            H4 rolesTitle = new H4("Roles");
            rolesTitle.addClassNames(LumoUtility.Margin.NONE, LumoUtility.Margin.Bottom.SMALL);
            rolesTitle.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "12px")
                    .set("text-transform", "uppercase").set("letter-spacing", "0.08em");

            HorizontalLayout rolesRow = new HorizontalLayout();
            rolesRow.setSpacing(true);
            rolesRow.getStyle().set("flex-wrap", "wrap").set("gap", "6px");

            user.getRoles().stream()
                    .sorted()
                    .forEach(role -> rolesRow.add(roleBadge(role)));

            rolesSection.add(rolesTitle, rolesRow);

            card.add(header, divider, details, rolesSection);
            add(card);
        });
    }

    private HorizontalLayout infoRow(VaadinIcon icon, String label, String value) {
        Span iconSpan = new Span(icon.create());
        iconSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("width", "18px");

        Span labelSpan = new Span(label + ": ");
        labelSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "14px")
                .set("min-width", "110px");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.getStyle().set("font-size", "14px").set("font-weight", "500");

        HorizontalLayout row = new HorizontalLayout(iconSpan, labelSpan, valueSpan);
        row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        row.setPadding(false);
        row.setSpacing(false);
        row.getStyle().set("gap", "10px");
        return row;
    }

    private Span roleBadge(String role) {
        String label = ROLE_LABELS.getOrDefault(role, role.replace("ROLE_", "").replace("_", " "));
        Span badge = new Span(label);
        badge.getElement().getThemeList().add(ROLE_BADGE_THEME.getOrDefault(role, "badge"));
        return badge;
    }

    private Span statusBadge(String status) {
        Span badge = new Span(status);
        String theme = switch (status) {
            case "ACTIVE"   -> "badge success small";
            case "DISABLED" -> "badge error small";
            default         -> "badge contrast small";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    @Override
    public String getPageTitle() {
        return "My Profile";
    }
}
