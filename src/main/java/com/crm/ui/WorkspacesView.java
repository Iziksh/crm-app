package com.crm.ui;

import com.crm.dto.request.WorkspaceRequest;
import com.crm.dto.response.WorkspaceResponse;
import com.crm.service.TranslationService;
import com.crm.service.UserService;
import com.crm.service.WorkspaceService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "workspaces", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class WorkspacesView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final WorkspaceService workspaceService;
    private final UserService userService;

    private final Grid<WorkspaceResponse> grid = new Grid<>(WorkspaceResponse.class, false);
    private final VerticalLayout memberPanel = new VerticalLayout();
    private WorkspaceResponse selectedWorkspace = null;

    public WorkspacesView(WorkspaceService workspaceService, UserService userService,
                          TranslationService i18n) {
        this.workspaceService = workspaceService;
        this.userService = userService;
        this.i18n = i18n;
        setSizeFull();
        setPadding(true);

        configureGrid();
        memberPanel.setPadding(false);
        memberPanel.setVisible(false);

        Button addBtn = new Button(i18n.translate("view.workspaces.button.newWorkspace"),
                VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new H2(i18n.translate("view.workspaces.title")), addBtn, grid, memberPanel);
        setFlexGrow(1, grid);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("page.workspaces");
    }

    private void configureGrid() {
        grid.setHeight("400px");
        grid.addColumn(WorkspaceResponse::name).setHeader(i18n.translate("common.name"))
                .setSortable(true).setFlexGrow(2);
        grid.addColumn(WorkspaceResponse::memberCount).setHeader(i18n.translate("common.members"));
        grid.addColumn(w -> w.createdAt() != null ? w.createdAt().toLocalDate().toString() : "")
                .setHeader(i18n.translate("common.created"));
        grid.addComponentColumn(ws -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(ws));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(ws));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(edit, delete);
            return actions;
        }).setHeader(i18n.translate("common.actions")).setFlexGrow(0).setWidth("110px");

        grid.asSingleSelect().addValueChangeListener(e -> {
            selectedWorkspace = e.getValue();
            refreshMemberPanel();
        });
    }

    private void refreshMemberPanel() {
        memberPanel.removeAll();
        if (selectedWorkspace == null) { memberPanel.setVisible(false); return; }
        memberPanel.setVisible(true);

        WorkspaceResponse fresh = workspaceService.findById(selectedWorkspace.id());

        H4 title = new H4(i18n.translate("view.workspaces.membersOf", fresh.name()));

        Grid<String> memberGrid = new Grid<>();
        memberGrid.addColumn(s -> s).setHeader(i18n.translate("common.username"));
        memberGrid.addComponentColumn(username -> {
            Button remove = new Button(VaadinIcon.MINUS.create(), e -> {
                userService.findAll().stream()
                        .filter(u -> u.username().equals(username))
                        .findFirst()
                        .ifPresent(u -> {
                            workspaceService.removeMember(fresh.id(), u.id());
                            selectedWorkspace = workspaceService.findById(fresh.id());
                            refreshMemberPanel();
                            refreshGrid();
                        });
            });
            remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return remove;
        }).setHeader("").setWidth("60px").setFlexGrow(0);
        memberGrid.setItems(fresh.memberNames());
        memberGrid.setAllRowsVisible(true);

        TextField addMember = new TextField();
        addMember.setPlaceholder(i18n.translate("view.workspaces.placeholder.addUsername"));
        Button addBtn = new Button(i18n.translate("common.add"), e -> {
            String username = addMember.getValue().trim();
            if (username.isBlank()) return;
            userService.findAll().stream()
                    .filter(u -> u.username().equals(username))
                    .findFirst()
                    .ifPresentOrElse(
                            u -> {
                                workspaceService.addMember(fresh.id(), u.id());
                                selectedWorkspace = workspaceService.findById(fresh.id());
                                refreshMemberPanel();
                                refreshGrid();
                                addMember.clear();
                                notify(i18n.translate("view.workspaces.notification.memberAdded"), false);
                            },
                            () -> notify(i18n.translate("view.workspaces.notification.userNotFound", username), true)
                    );
        });
        addBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout addRow = new HorizontalLayout(addMember, addBtn);
        addRow.setDefaultVerticalComponentAlignment(Alignment.END);

        memberPanel.add(title, memberGrid, addRow);
    }

    private void refreshGrid() {
        grid.setItems(workspaceService.findAll());
    }

    private void openDialog(WorkspaceResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.workspaces.dialog.new")
                : i18n.translate("view.workspaces.dialog.edit"));
        dialog.setWidth("440px");

        TextField name = new TextField(i18n.translate("common.name"));
        TextArea description = new TextArea(i18n.translate("common.description"));
        description.setMinHeight("80px");

        if (existing != null) {
            name.setValue(nvl(existing.name()));
            description.setValue(nvl(existing.description()));
        }

        FormLayout form = new FormLayout(name, description);
        form.setColspan(description, 2);
        dialog.add(form);

        Button save = new Button(i18n.translate("common.save"), e -> {
            if (name.getValue().isBlank()) { name.setInvalid(true); return; }
            WorkspaceRequest req = new WorkspaceRequest(name.getValue(), description.getValue());
            try {
                if (existing == null) workspaceService.create(req, "admin");
                refreshGrid();
                dialog.close();
                notify(i18n.translate("view.workspaces.notification.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(
                new Button(i18n.translate("common.cancel"), e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(WorkspaceResponse ws) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(i18n.translate("view.workspaces.dialog.deleteHeader"));
        confirm.setText(i18n.translate("dialog.deleteConfirm", ws.name()));
        confirm.setConfirmText(i18n.translate("common.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            workspaceService.delete(ws.id());
            selectedWorkspace = null;
            refreshGrid();
            refreshMemberPanel();
            notify(i18n.translate("view.workspaces.notification.deleted"), false);
        });
        confirm.open();
    }

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
