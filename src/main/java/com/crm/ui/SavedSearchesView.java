package com.crm.ui;

import com.crm.domain.enums.SavedSearchScope;
import com.crm.dto.request.SavedSearchRequest;
import com.crm.dto.response.SavedSearchResponse;
import com.crm.service.SavedSearchService;
import com.crm.service.TranslationService;
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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "saved-searches", layout = MainLayout.class)
@PermitAll
public class SavedSearchesView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final SavedSearchService savedSearchService;
    private final Grid<SavedSearchResponse> grid = new Grid<>(SavedSearchResponse.class, false);

    public SavedSearchesView(SavedSearchService savedSearchService, TranslationService i18n) {
        this.savedSearchService = savedSearchService;
        this.i18n = i18n;
        setSizeFull();
        setPadding(true);

        configureGrid();

        Button addBtn = new Button(i18n.translate("view.savedSearches.button.newSearch"),
                VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new H2(i18n.translate("view.savedSearches.title")), addBtn, grid);
        setFlexGrow(1, grid);
        refreshGrid();
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("page.savedSearches");
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(SavedSearchResponse::name).setHeader(i18n.translate("common.name"))
                .setSortable(true).setFlexGrow(2);
        grid.addColumn(r -> r.scope() != null ? i18n.translateEnum(r.scope()) : "")
                .setHeader(i18n.translate("common.scope")).setSortable(true);
        grid.addColumn(r -> r.createdAt() != null ? r.createdAt().toLocalDate().toString() : "")
                .setHeader(i18n.translate("common.created"));
        grid.addComponentColumn(search -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            Button run = new Button(i18n.translate("common.run"), VaadinIcon.PLAY.create(),
                    e -> runSearch(search));
            run.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(search));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(search));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(run, edit, delete);
            return actions;
        }).setHeader(i18n.translate("common.actions")).setFlexGrow(0).setWidth("200px");
    }

    private void runSearch(SavedSearchResponse search) {
        try {
            long count = savedSearchService.execute(search.id());
            notify(i18n.translate("view.savedSearches.notification.searchResults", count), false);
        } catch (Exception e) {
            notify(i18n.translate("notification.errorPrefix", e.getMessage()), true);
        }
    }

    private void refreshGrid() {
        grid.setItems(savedSearchService.findAll());
    }

    private void openDialog(SavedSearchResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.savedSearches.dialog.new")
                : i18n.translate("view.savedSearches.dialog.edit"));
        dialog.setWidth("500px");

        TextField name = new TextField(i18n.translate("common.name"));

        ComboBox<SavedSearchScope> scope = new ComboBox<>(i18n.translate("common.scope"));
        scope.setItems(SavedSearchScope.values());
        scope.setItemLabelGenerator(i18n::translateEnum);

        TextArea filterJson = new TextArea(i18n.translate("common.filterJson"));
        filterJson.setPlaceholder(i18n.translate("view.savedSearches.filterJson.placeholder"));
        filterJson.setMinHeight("120px");
        filterJson.setHelperText(i18n.translate("view.savedSearches.filterJson.helper"));

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

        Button save = new Button(i18n.translate("common.save"), e -> {
            if (name.getValue().isBlank()) { name.setInvalid(true); return; }
            SavedSearchRequest req = new SavedSearchRequest(
                    name.getValue(), scope.getValue(), filterJson.getValue());
            try {
                if (existing == null) savedSearchService.create(req, "admin");
                else savedSearchService.update(existing.id(), req);
                refreshGrid();
                dialog.close();
                notify(i18n.translate("view.savedSearches.notification.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(
                new Button(i18n.translate("common.cancel"), e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(SavedSearchResponse search) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(i18n.translate("view.savedSearches.dialog.deleteHeader"));
        confirm.setText(i18n.translate("dialog.deleteConfirm", search.name()));
        confirm.setConfirmText(i18n.translate("common.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            savedSearchService.delete(search.id());
            refreshGrid();
            notify(i18n.translate("notification.genericDeleted"), false);
        });
        confirm.open();
    }

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}
