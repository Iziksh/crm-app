package com.crm.ui;

import com.crm.domain.entity.User;
import com.crm.domain.enums.UserStatus;
import com.crm.dto.request.AdminInviteRequest;
import com.crm.exception.LastAdminException;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import com.crm.service.AdminUserManagementService;
import com.crm.service.TranslationService;
import com.crm.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "users", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "COMPANY_ADMIN", "SUPER_ADMIN"})
public class UsersView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final AdminUserManagementService adminService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final SecurityService securityService;
    private final Grid<User> grid = new Grid<>(User.class, false);
    private final TextField searchField = new TextField();
    private User actingUser;

    public UsersView(AdminUserManagementService adminService,
                     UserService userService,
                     UserRepository userRepository,
                     WorkspaceRepository workspaceRepository,
                     SecurityService securityService,
                     TranslationService i18n) {
        this.adminService = adminService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.securityService = securityService;
        this.i18n = i18n;
        setSizeFull();
        setPadding(true);

        actingUser = userRepository.findByUsername(securityService.getUsername())
                .orElseThrow(() -> new IllegalStateException("Current user not found"));

        configureGrid();
        add(new H2(i18n.translate("view.users.title")), buildToolbar(), grid);
        setFlexGrow(1, grid);
        refresh();
    }

    @Override
    public String getPageTitle() { return i18n.translate("page.users"); }

    // If workspace_id is null (account created before workspace scoping),
    // look it up via workspace membership and persist it so future calls are fast.
    private void resolveWorkspaceIfMissing() {
        if (actingUser.getWorkspaceId() != null || adminService.isSuperAdmin(actingUser)) return;
        workspaceRepository.findByMembers_Id(actingUser.getId()).stream().findFirst().ifPresent(ws -> {
            actingUser.setWorkspaceId(ws.getId());
            userRepository.save(actingUser);
        });
    }

    private void refresh() {
        resolveWorkspaceIfMissing();
        List<User> users;
        if (adminService.isSuperAdmin(actingUser)) {
            users = userRepository.findAll();
        } else if (actingUser.getWorkspaceId() == null) {
            users = List.of();
        } else {
            users = adminService.listWorkspaceUsers(actingUser.getWorkspaceId(), actingUser);
        }
        String filter = searchField.getValue().toLowerCase();
        if (!filter.isBlank()) {
            users = users.stream()
                    .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(filter))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(filter)))
                    .toList();
        }
        grid.setItems(users);
    }

    private void configureGrid() {
        grid.setSizeFull();

        grid.addColumn(User::getUsername)
                .setHeader(i18n.translate("common.username")).setSortable(true).setFlexGrow(1);
        grid.addColumn(User::getEmail)
                .setHeader(i18n.translate("common.email")).setSortable(true).setFlexGrow(2);

        grid.addComponentColumn(u -> {
            HorizontalLayout badges = new HorizontalLayout();
            badges.setSpacing(true);
            if (u.getRoles() != null) {
                u.getRoles().forEach(role -> {
                    String key = role.replace("ROLE_", "");
                    Span badge = new Span(i18n.translate("common.role." + key));
                    badge.getElement().getThemeList().add(switch (role) {
                        case "ROLE_ADMIN", "ROLE_SUPER_ADMIN" -> "badge error";
                        case "ROLE_COMPANY_ADMIN" -> "badge primary";
                        case "ROLE_SALES" -> "badge success";
                        case "ROLE_SUPPORT" -> "badge";
                        default -> "badge contrast";
                    });
                    badges.add(badge);
                });
            }
            return badges;
        }).setHeader(i18n.translate("common.roles")).setFlexGrow(1);

        grid.addComponentColumn(u -> {
            UserStatus st = u.getStatus() != null ? u.getStatus()
                    : (u.isEnabled() ? UserStatus.ACTIVE : UserStatus.DISABLED);
            String label = switch (st) {
                case INVITED  -> i18n.translate("common.status.INVITED");
                case ACTIVE   -> i18n.translate("common.active");
                case DISABLED -> i18n.translate("common.disabled");
            };
            String theme = switch (st) {
                case INVITED  -> "badge";
                case ACTIVE   -> "badge success";
                case DISABLED -> "badge contrast";
            };
            Span badge = new Span(label);
            badge.getElement().getThemeList().add(theme);
            return badge;
        }).setHeader(i18n.translate("common.status")).setFlexGrow(0).setWidth("110px");

        grid.addColumn(u -> u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate().toString() : "")
                .setHeader(i18n.translate("common.created")).setFlexGrow(0).setWidth("110px");

        grid.addComponentColumn(u -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);
            boolean isSelf = u.getUsername().equals(actingUser.getUsername());

            UserStatus st = u.getStatus() != null ? u.getStatus()
                    : (u.isEnabled() ? UserStatus.ACTIVE : UserStatus.DISABLED);

            if (st == UserStatus.INVITED) {
                Button resend = new Button(VaadinIcon.ENVELOPE.create(), e -> {
                    try {
                        String role = u.getRoles().stream().findFirst().orElse("ROLE_USER");
                        adminService.inviteUser(
                                new AdminInviteRequest(u.getEmail(), role, u.getWorkspaceId()),
                                actingUser);
                        notify(i18n.translate("view.users.notification.resendInvite", u.getEmail()), false);
                    } catch (Exception ex) {
                        notify(ex.getMessage(), true);
                    }
                });
                resend.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                resend.getElement().setAttribute("title", i18n.translate("view.users.button.resendInvite"));
                actions.add(resend);
            }

            if (!isSelf && st != UserStatus.INVITED) {
                boolean active = st == UserStatus.ACTIVE;
                Button toggle = new Button(active ? VaadinIcon.BAN.create() : VaadinIcon.CHECK.create(), e -> {
                    try {
                        if (active) adminService.disableUser(u.getId(), actingUser);
                        else        adminService.enableUser(u.getId(), actingUser);
                        refresh();
                        notify(active
                                ? i18n.translate("view.users.notification.disabled", u.getUsername())
                                : i18n.translate("view.users.notification.enabled", u.getUsername()), false);
                    } catch (LastAdminException ex) {
                        notify(i18n.translate("view.users.notification.lastAdmin"), true);
                    } catch (Exception ex) {
                        notify(ex.getMessage(), true);
                    }
                });
                toggle.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                toggle.getElement().setAttribute("title",
                        active ? i18n.translate("common.disable") : i18n.translate("common.enable"));
                actions.add(toggle);
            }

            // Edit name/email — available for all users including self
            Button editBtn = new Button(VaadinIcon.PENCIL.create(), e -> openEditDialog(u));
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editBtn.getElement().setAttribute("title", i18n.translate("view.users.button.edit"));
            actions.add(editBtn);

            if (!isSelf) {
                Button roleBtn = new Button(VaadinIcon.EDIT.create(), e -> openChangeRoleDialog(u));
                roleBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                roleBtn.getElement().setAttribute("title", i18n.translate("view.users.button.changeRole"));
                actions.add(roleBtn);

                Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(u));
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                actions.add(deleteBtn);
            }

            return actions;
        }).setHeader(i18n.translate("common.actions")).setFlexGrow(0).setWidth("200px");
    }

    private HorizontalLayout buildToolbar() {
        searchField.setPlaceholder(i18n.translate("view.users.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refresh());

        Button inviteBtn = new Button(i18n.translate("view.users.button.inviteUser"),
                VaadinIcon.PLUS.create(), e -> openInviteDialog());
        inviteBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(searchField, inviteBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        toolbar.setWidthFull();
        toolbar.expand(searchField);
        return toolbar;
    }

    private void openInviteDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(i18n.translate("view.users.dialog.invite"));
        dialog.setWidth("420px");

        EmailField email = new EmailField(i18n.translate("common.email"));
        email.setWidthFull();

        ComboBox<String> roleCombo = new ComboBox<>(i18n.translate("common.roles"));
        roleCombo.setItems("ROLE_COMPANY_ADMIN", "ROLE_USER", "ROLE_SALES", "ROLE_SUPPORT");
        roleCombo.setValue("ROLE_USER");
        roleCombo.setWidthFull();
        roleCombo.setItemLabelGenerator(r -> i18n.translate("common.role." + r.replace("ROLE_", "")));

        FormLayout form = new FormLayout(email, roleCombo);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        dialog.add(form);

        Button send = new Button(i18n.translate("view.users.button.sendInvite"), e -> {
            if (email.getValue().isBlank()) { email.setInvalid(true); return; }
            if (roleCombo.getValue() == null) { roleCombo.setInvalid(true); return; }
            Long wsId = actingUser.getWorkspaceId();
            if (wsId == null) {
                notify(i18n.translate("view.users.notification.noWorkspace"), true);
                return;
            }
            try {
                adminService.inviteUser(
                        new AdminInviteRequest(email.getValue(), roleCombo.getValue(), wsId),
                        actingUser);
                dialog.close();
                refresh();
                notify(i18n.translate("view.users.notification.invited", email.getValue()), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button(i18n.translate("common.cancel"), e -> dialog.close()), send);
        dialog.open();
    }

    private void openEditDialog(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(i18n.translate("view.users.dialog.edit"));
        dialog.setWidth("420px");

        TextField username = new TextField(i18n.translate("common.username"));
        username.setValue(user.getUsername() != null ? user.getUsername() : "");
        username.setWidthFull();

        EmailField email = new EmailField(i18n.translate("common.email"));
        email.setValue(user.getEmail() != null ? user.getEmail() : "");
        email.setWidthFull();

        FormLayout form = new FormLayout(username, email);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        dialog.add(form);

        Button save = new Button(i18n.translate("common.save"), e -> {
            if (username.getValue().isBlank()) { username.setInvalid(true); return; }
            try {
                userService.updateProfile(user.getId(), username.getValue(), email.getValue());
                dialog.close();
                refresh();
                notify(i18n.translate("view.users.notification.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button(i18n.translate("common.cancel"), e -> dialog.close()), save);
        dialog.open();
    }

    private void openChangeRoleDialog(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(i18n.translate("view.users.dialog.changeRole"));
        dialog.setWidth("360px");

        ComboBox<String> roleCombo = new ComboBox<>(i18n.translate("common.roles"));
        roleCombo.setItems("ROLE_COMPANY_ADMIN", "ROLE_USER", "ROLE_SALES", "ROLE_SUPPORT");
        roleCombo.setValue(user.getRoles().stream().findFirst().orElse("ROLE_USER"));
        roleCombo.setWidthFull();
        roleCombo.setItemLabelGenerator(r -> i18n.translate("common.role." + r.replace("ROLE_", "")));

        dialog.add(roleCombo);

        Button save = new Button(i18n.translate("common.save"), e -> {
            if (roleCombo.getValue() == null) { roleCombo.setInvalid(true); return; }
            try {
                adminService.changeRole(user.getId(), roleCombo.getValue(), actingUser);
                dialog.close();
                refresh();
                notify(i18n.translate("view.users.notification.roleChanged"), false);
            } catch (LastAdminException ex) {
                notify(i18n.translate("view.users.notification.lastAdmin"), true);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button(i18n.translate("common.cancel"), e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(User user) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(i18n.translate("view.users.dialog.deleteHeader"));
        confirm.setText(i18n.translate("view.users.dialog.deleteConfirm", user.getUsername()));
        confirm.setConfirmText(i18n.translate("common.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            try {
                adminService.removeUser(user.getId(), actingUser);
                refresh();
                notify(i18n.translate("view.users.notification.deleted"), false);
            } catch (LastAdminException ex) {
                notify(i18n.translate("view.users.notification.lastAdmin"), true);
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
}
