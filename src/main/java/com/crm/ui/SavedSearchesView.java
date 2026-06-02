package com.crm.ui;

import com.crm.domain.enums.SavedSearchScope;
import com.crm.dto.request.SavedSearchRequest;
import com.crm.dto.response.SavedSearchResponse;
import com.crm.service.SavedSearchService;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "saved-searches", layout = MainLayout.class)
@PageTitle("Saved Searches | CRM")
@PermitAll
public class SavedSearchesView extends VerticalLayout {

    private final SavedSearchService savedSearchService;
    private final Grid<SavedSearchResponse> grid = new Grid<>(SavedSearchResponse.class, false);

    public SavedSearchesView(SavedSearchService savedSearchService) {
        this.savedSearchService = savedSearchService;
        setSizeFull();
        setPadding(true);

        configureGrid();

        Button addBtn = new Button("New Search", VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new H2("Saved Searches"), addBtn, grid);
        setFlexGrow(1, grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(SavedSearchResponse::name).setHeader("Name").setSortable(true).setFlexGrow(2);
        grid.addColumn(r -> r.scope() != null ? r.scope().name() : "").setHeader("Scope").setSortable(true);
        grid.addColumn(r -> r.createdAt() != null ? r.createdAt().toLocalDate().toString() : "").setHeader("Created");
        grid.addComponentColumn(search -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            Button run = new Button("Run", VaadinIcon.PLAY.create(), e -> runSearch(search));
            run.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(search));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(search));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(run, edit, delete);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setWidth("200px");
    }

    private void runSearch(SavedSearchResponse search) {
        try {
            long count = savedSearchService.execute(search.id());
            notify("Search returned " + count + " result(s)", false);
        } catch (Exception e) {
            notify("Error: " + e.getMessage(), true);
        }
    }

    private void refreshGrid() {
        grid.setItems(savedSearchService.findAll());
    }

    private void openDialog(SavedSearchResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Saved Search" : "Edit Saved Search");
        dialog.setWidth("500px");

        TextField name = new TextField("Name");

        ComboBox<SavedSearchScope> scope = new ComboBox<>("Scope");
        scope.setItems(SavedSearchScope.values());

        TextArea filterJson = new TextArea("Filter JSON");
        filterJson.setPlaceholder("{\"status\": \"OPEN\"}");
        filterJson.setMinHeight("120px");
        filterJson.setHelperText("JSON object with field: value pairs to filter on");

        if (existing != null) {
            name.setValue(existing.name() != null ? existing.name() : "");
            if (existing.scope() != null) scope.setValue(existing.scope());
            filterJson.setValue(existing.filterJson() != null ? existing.filterJson() : "");
        }

        FormLayout form = new FormLayout(name, scope, filterJson);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(name, 2);
        form.setColspan(filterJson, 2);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            if (name.getValue().isBlank()) { name.setInvalid(true); return; }
            SavedSearchRequest req = new SavedSearchRequest(
                    name.getValue(), scope.getValue(), filterJson.getValue());
            try {
                if (existing == null) savedSearchService.create(req, "admin");
                else savedSearchService.update(existing.id(), req);
                refreshGrid();
                dialog.close();
                notify("Saved search saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(SavedSearchResponse search) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Saved Search");
        confirm.setText("Delete \"" + search.name() + "\"?");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            savedSearchService.delete(search.id());
            refreshGrid();
            notify("Deleted", false);
        });
        confirm.open();
    }

    private static void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}
