package com.crm.ui;

import com.crm.dto.request.UserRequest;
import com.crm.dto.response.UserResponse;
import com.crm.service.TranslationService;
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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.util.Set;

@Route(value = "users", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class UsersView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final UserService userService;
    private final SecurityService securityService;
    private final TextField searchField = new TextField();
    private final Grid<UserResponse> grid = new Grid<>(UserResponse.class, false);

    public UsersView(UserService userService, SecurityService securityService, TranslationService i18n) {
        this.userService = userService;
        this.securityService = securityService;
        this.i18n = i18n;
        setSizeFull();
        setPadding(true);

        configureGrid();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2(i18n.translate("view.users.title")), toolbar, grid);
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

    @Override
    public String getPageTitle() {
        return i18n.translate("page.users");
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(UserResponse::username).setHeader(i18n.translate("common.username"))
                .setSortable(true).setFlexGrow(1);
        grid.addColumn(UserResponse::email).setHeader(i18n.translate("common.email"))
                .setSortable(true).setFlexGrow(2);
        grid.addComponentColumn(u -> {
            HorizontalLayout badges = new HorizontalLayout();
            badges.setSpacing(true);
            if (u.roles() != null) {
                u.roles().forEach(role -> {
                    String roleKey = role.replace("ROLE_", "");
                    Span badge = new Span(i18n.translate("common.role." + roleKey));
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
        }).setHeader(i18n.translate("common.roles")).setFlexGrow(1);
        grid.addComponentColumn(u -> {
            Span badge = new Span(u.enabled()
                    ? i18n.translate("common.active")
                    : i18n.translate("common.disabled"));
            badge.getElement().getThemeList().add(u.enabled() ? "badge success" : "badge contrast");
            return badge;
        }).setHeader(i18n.translate("common.status")).setFlexGrow(0).setWidth("100px");
        grid.addColumn(u -> u.createdAt() != null ? u.createdAt().toLocalDate().toString() : "")
                .setHeader(i18n.translate("common.created")).setFlexGrow(0).setWidth("110px");
        grid.addComponentColumn(user -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);
            String currentUser = securityService.getUsername();

            Button toggle = new Button(user.enabled() ? VaadinIcon.BAN.create() : VaadinIcon.CHECK.create(), e -> {
                if (user.username().equals(currentUser)) {
                    notify(i18n.translate("view.users.notification.cannotDisableSelf"), true);
                    return;
                }
                userService.toggleEnabled(user.id());
                grid.getDataProvider().refreshAll();
                notify(user.enabled()
                        ? i18n.translate("view.users.notification.disabled", user.username())
                        : i18n.translate("view.users.notification.enabled", user.username()), false);
            });
            toggle.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            toggle.getElement().setAttribute("title", user.enabled()
                    ? i18n.translate("common.disable")
                    : i18n.translate("common.enable"));

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(user));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(user));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            delete.setEnabled(!user.username().equals(currentUser));

            actions.add(toggle, edit, delete);
            return actions;
        }).setHeader(i18n.translate("common.actions")).setFlexGrow(0).setWidth("160px");
    }

    private HorizontalLayout buildToolbar() {
        searchField.setPlaceholder(i18n.translate("view.users.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        Button addBtn = new Button(i18n.translate("view.users.button.newUser"),
                VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        toolbar.setWidthFull();
        toolbar.expand(searchField);
        return toolbar;
    }

    private void openDialog(UserResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.users.dialog.new")
                : i18n.translate("view.users.dialog.edit"));
        dialog.setWidth("480px");

        TextField username = new TextField(i18n.translate("common.username"));
        username.setEnabled(existing == null);
        TextField email = new TextField(i18n.translate("common.email"));
        PasswordField password = new PasswordField(i18n.translate("common.password"));
        if (existing != null) password.setPlaceholder(i18n.translate("view.users.password.placeholder"));

        CheckboxGroup<String> roles = new CheckboxGroup<>(i18n.translate("common.roles"));
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

        Button save = new Button(i18n.translate("common.save"), e -> {
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
                notify(i18n.translate("view.users.notification.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(
                new Button(i18n.translate("common.cancel"), e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(UserResponse user) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(i18n.translate("view.users.dialog.deleteHeader"));
        confirm.setText(i18n.translate("view.users.dialog.deleteConfirm", user.username()));
        confirm.setConfirmText(i18n.translate("common.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            try {
                userService.delete(user.id());
                grid.getDataProvider().refreshAll();
                notify(i18n.translate("view.users.notification.deleted"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        confirm.open();
    }

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
