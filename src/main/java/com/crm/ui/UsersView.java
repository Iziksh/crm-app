package com.crm.ui;

import com.crm.dto.request.UserRequest;
import com.crm.dto.response.UserResponse;
import com.crm.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.util.Set;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Users | CRM")
@RolesAllowed("ADMIN")
public class UsersView extends VerticalLayout {

    private final UserService userService;
    private final SecurityService securityService;
    private final TextField searchField = new TextField();
    private final Grid<UserResponse> grid = new Grid<>(UserResponse.class, false);

    public UsersView(UserService userService, SecurityService securityService) {
        this.userService = userService;
        this.securityService = securityService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2("Users"), toolbar, grid);
        setFlexGrow(1, grid);

        grid.setItems(DataProvider.fromCallbacks(
            query -> {
                int page = query.getLimit() > 0 ? query.getOffset() / query.getLimit() : 0;
                return userService.findAll(PageRequest.of(page, query.getLimit()), searchField.getValue())
                        .getContent().stream();
            },
            query -> (int) userService.count(searchField.getValue())
        ));
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(UserResponse::username).setHeader("Username").setSortable(true).setFlexGrow(1);
        grid.addColumn(UserResponse::email).setHeader("Email").setSortable(true).setFlexGrow(2);
        grid.addComponentColumn(u -> {
            HorizontalLayout badges = new HorizontalLayout();
            badges.setSpacing(true);
            if (u.roles() != null) {
                u.roles().forEach(role -> {
                    Span badge = new Span(role.replace("ROLE_", ""));
                    String theme = switch (role) {
                        case "ROLE_ADMIN" -> "badge error";
                        case "ROLE_SALES" -> "badge success";
                        case "ROLE_SUPPORT" -> "badge primary";
                        default -> "badge contrast";
                    };
                    badge.getElement().getThemeList().add(theme);
                    badges.add(badge);
                });
            }
            return badges;
        }).setHeader("Roles").setFlexGrow(1);
        grid.addComponentColumn(u -> {
            Span badge = new Span(u.enabled() ? "Active" : "Disabled");
            badge.getElement().getThemeList().add(u.enabled() ? "badge success" : "badge contrast");
            return badge;
        }).setHeader("Status").setFlexGrow(0).setWidth("100px");
        grid.addColumn(u -> u.createdAt() != null ? u.createdAt().toLocalDate().toString() : "")
                .setHeader("Created").setFlexGrow(0).setWidth("110px");
        grid.addComponentColumn(user -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);
            String currentUser = securityService.getUsername();

            Button toggle = new Button(user.enabled() ? VaadinIcon.BAN.create() : VaadinIcon.CHECK.create(), e -> {
                if (user.username().equals(currentUser)) { notify("Cannot disable yourself", true); return; }
                userService.toggleEnabled(user.id());
                grid.getDataProvider().refreshAll();
                notify(user.username() + (user.enabled() ? " disabled" : " enabled"), false);
            });
            toggle.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            toggle.getElement().setAttribute("title", user.enabled() ? "Disable" : "Enable");

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(user));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(user));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            delete.setEnabled(!user.username().equals(currentUser));

            actions.add(toggle, edit, delete);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setWidth("160px");
    }

    private HorizontalLayout buildToolbar() {
        searchField.setPlaceholder("Search users…");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        Button addBtn = new Button("New User", VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        toolbar.setWidthFull();
        toolbar.expand(searchField);
        return toolbar;
    }

    private void openDialog(UserResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New User" : "Edit User");
        dialog.setWidth("480px");

        TextField username = new TextField("Username");
        username.setEnabled(existing == null);
        TextField email = new TextField("Email");
        PasswordField password = new PasswordField("Password");
        if (existing != null) password.setPlaceholder("Leave blank to keep current");

        CheckboxGroup<String> roles = new CheckboxGroup<>("Roles");
        roles.setItems("ROLE_USER", "ROLE_ADMIN", "ROLE_SALES", "ROLE_SUPPORT");
        roles.setValue(Set.of("ROLE_USER"));

        if (existing != null) {
            username.setValue(nvl(existing.username()));
            email.setValue(nvl(existing.email()));
            if (existing.roles() != null) roles.setValue(existing.roles());
        }

        FormLayout form = new FormLayout(username, email, password, roles);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(roles, 2);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            if (username.getValue().isBlank()) { username.setInvalid(true); return; }
            UserRequest req = new UserRequest(
                    username.getValue(), email.getValue(),
                    password.getValue().isBlank() ? null : password.getValue(),
                    roles.getValue().isEmpty() ? Set.of("ROLE_USER") : roles.getValue());
            try {
                if (existing == null) userService.create(req);
                else userService.update(existing.id(), req);
                grid.getDataProvider().refreshAll();
                dialog.close();
                notify("User saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(UserResponse user) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete User");
        confirm.setText("Delete user \"" + user.username() + "\"? This cannot be undone.");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            try {
                userService.delete(user.id());
                grid.getDataProvider().refreshAll();
                notify("User deleted", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        confirm.open();
    }

    private static void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
