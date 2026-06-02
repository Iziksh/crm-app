package com.crm.ui;

import com.crm.dto.request.AccountGroupRequest;
import com.crm.dto.response.AccountGroupResponse;
import com.crm.dto.response.AccountResponse;
import com.crm.service.AccountGroupService;
import com.crm.service.AccountService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Route(value = "account-groups", layout = MainLayout.class)
@PageTitle("Account Groups | CRM")
@PermitAll
public class AccountGroupsView extends VerticalLayout {

    private final AccountGroupService groupService;
    private final AccountService accountService;
    private final Grid<AccountGroupResponse> grid = new Grid<>(AccountGroupResponse.class, false);
    private final TextField searchField = new TextField();

    public AccountGroupsView(AccountGroupService groupService, AccountService accountService) {
        this.groupService = groupService;
        this.accountService = accountService;
        setSizeFull();
        setPadding(true);

        configureGrid();

        HorizontalLayout toolbar = buildToolbar();
        add(new H2("Account Groups"), toolbar, grid);
        setFlexGrow(1, grid);

        grid.setItems(DataProvider.fromCallbacks(
            query -> {
                int page = query.getLimit() > 0 ? query.getOffset() / query.getLimit() : 0;
                String search = searchField.getValue();
                return groupService.findAll(PageRequest.of(page, query.getLimit()), search).getContent().stream();
            },
            query -> (int) groupService.count(searchField.getValue())
        ));
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(AccountGroupResponse::name).setHeader("Name").setSortable(true).setFlexGrow(2);
        grid.addColumn(AccountGroupResponse::parentName).setHeader("Parent Group").setSortable(true);
        grid.addColumn(AccountGroupResponse::memberCount).setHeader("Members").setSortable(true);
        grid.addColumn(r -> r.createdAt() != null ? r.createdAt().toLocalDate().toString() : "")
                .setHeader("Created").setSortable(true);
        grid.addComponentColumn(group -> {
            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(group));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(group));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            HorizontalLayout actions = new HorizontalLayout(edit, delete);
            actions.setSpacing(false);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setWidth("120px");
    }

    private HorizontalLayout buildToolbar() {
        searchField.setPlaceholder("Search by name…");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button("New Group", VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.expand(searchField);
        return toolbar;
    }

    private void refreshGrid() {
        grid.getDataProvider().refreshAll();
    }

    private void openDialog(AccountGroupResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Group" : "Edit Group");
        dialog.setWidth("480px");

        TextField name = new TextField("Name");
        TextField description = new TextField("Description");

        List<AccountGroupResponse> allGroups = groupService.findAll(PageRequest.of(0, 200)).getContent();
        ComboBox<AccountGroupResponse> parentGroup = new ComboBox<>("Parent Group");
        parentGroup.setItems(allGroups);
        parentGroup.setItemLabelGenerator(AccountGroupResponse::name);
        parentGroup.setClearButtonVisible(true);

        if (existing != null) {
            name.setValue(nvl(existing.name()));
            description.setValue(nvl(existing.description()));
            if (existing.parentId() != null) {
                allGroups.stream()
                        .filter(g -> g.id().equals(existing.parentId()))
                        .findFirst()
                        .ifPresent(parentGroup::setValue);
            }
        }

        FormLayout form = new FormLayout(name, description, parentGroup);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        dialog.add(form);

        Button save = new Button("Save", e -> {
            if (name.getValue().isBlank()) {
                name.setInvalid(true);
                name.setErrorMessage("Name is required");
                return;
            }
            Long parentId = parentGroup.getValue() != null ? parentGroup.getValue().id() : null;
            AccountGroupRequest req = new AccountGroupRequest(name.getValue(), description.getValue(), parentId);
            try {
                if (existing == null) groupService.create(req);
                else groupService.update(existing.id(), req);
                refreshGrid();
                dialog.close();
                notify("Group saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(AccountGroupResponse group) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Group");
        confirm.setText("Delete \"" + group.name() + "\"?");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            groupService.delete(group.id());
            refreshGrid();
            notify("Group deleted", false);
        });
        confirm.open();
    }

    private static void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
