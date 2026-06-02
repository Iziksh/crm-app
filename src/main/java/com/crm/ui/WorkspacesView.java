package com.crm.ui;

import com.crm.dto.request.WorkspaceRequest;
import com.crm.dto.response.WorkspaceResponse;
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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "workspaces", layout = MainLayout.class)
@PageTitle("Workspaces | CRM")
@RolesAllowed("ADMIN")
public class WorkspacesView extends VerticalLayout {

    private final WorkspaceService workspaceService;
    private final UserService userService;

    private final Grid<WorkspaceResponse> grid = new Grid<>(WorkspaceResponse.class, false);
    private final VerticalLayout memberPanel = new VerticalLayout();
    private WorkspaceResponse selectedWorkspace = null;

    public WorkspacesView(WorkspaceService workspaceService, UserService userService) {
        this.workspaceService = workspaceService;
        this.userService = userService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        memberPanel.setPadding(false);
        memberPanel.setVisible(false);

        Button addBtn = new Button("New Workspace", VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new H2("Workspaces"), addBtn, grid, memberPanel);
        setFlexGrow(1, grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.setHeight("400px");
        grid.addColumn(WorkspaceResponse::name).setHeader("Name").setSortable(true).setFlexGrow(2);
        grid.addColumn(WorkspaceResponse::memberCount).setHeader("Members");
        grid.addColumn(w -> w.createdAt() != null ? w.createdAt().toLocalDate().toString() : "").setHeader("Created");
        grid.addComponentColumn(ws -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(ws));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(ws));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(edit, delete);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setWidth("110px");

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

        H4 title = new H4("Members of: " + fresh.name());

        Grid<String> memberGrid = new Grid<>();
        memberGrid.addColumn(s -> s).setHeader("Username");
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
        addMember.setPlaceholder("Username to add");
        Button addBtn = new Button("Add", e -> {
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
                                notify("Member added", false);
                            },
                            () -> notify("User not found: " + username, true)
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
        dialog.setHeaderTitle(existing == null ? "New Workspace" : "Edit Workspace");
        dialog.setWidth("440px");

        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");
        description.setMinHeight("80px");

        if (existing != null) {
            name.setValue(existing.name() != null ? existing.name() : "");
            name.setValue(nvl(existing.name()));
            description.setValue(nvl(existing.description()));
        }

        FormLayout form = new FormLayout(name, description);
        form.setColspan(description, 2);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            if (name.getValue().isBlank()) { name.setInvalid(true); return; }
            WorkspaceRequest req = new WorkspaceRequest(name.getValue(), description.getValue());
            try {
                if (existing == null) workspaceService.create(req, "admin");
                refreshGrid();
                dialog.close();
                notify("Workspace saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(WorkspaceResponse ws) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Workspace");
        confirm.setText("Delete \"" + ws.name() + "\"?");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            workspaceService.delete(ws.id());
            selectedWorkspace = null;
            refreshGrid();
            refreshMemberPanel();
            notify("Workspace deleted", false);
        });
        confirm.open();
    }

    private static void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
