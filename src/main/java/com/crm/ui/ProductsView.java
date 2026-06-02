package com.crm.ui;

import com.crm.domain.enums.ProductCategory;
import com.crm.dto.request.ProductRequest;
import com.crm.dto.response.ProductResponse;
import com.crm.service.ProductService;
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
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

@Route(value = "products", layout = MainLayout.class)
@PageTitle("Products | CRM")
@PermitAll
public class ProductsView extends VerticalLayout {

    private final ProductService productService;
    private final TextField searchField = new TextField();
    private final Grid<ProductResponse> grid = new Grid<>(ProductResponse.class, false);

    public ProductsView(ProductService productService) {
        this.productService = productService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2("Products"), toolbar, grid);
        setFlexGrow(1, grid);

        grid.setItems(DataProvider.fromCallbacks(
            query -> {
                int page = query.getLimit() > 0 ? query.getOffset() / query.getLimit() : 0;
                return productService.findAll(PageRequest.of(page, query.getLimit()), searchField.getValue(), false)
                        .getContent().stream();
            },
            query -> (int) productService.countAll(searchField.getValue())
        ));
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(ProductResponse::sku).setHeader("SKU").setFlexGrow(0).setWidth("120px");
        grid.addColumn(ProductResponse::name).setHeader("Name").setSortable(true).setFlexGrow(2);
        grid.addColumn(p -> p.category() != null ? p.category().name() : "").setHeader("Category");
        grid.addColumn(p -> p.currency() + " " + p.unitPrice()).setHeader("Unit Price").setSortable(true);
        grid.addComponentColumn(p -> {
            Span badge = new Span(p.active() ? "Active" : "Inactive");
            badge.getElement().getThemeList().add(p.active() ? "badge success" : "badge contrast");
            return badge;
        }).setHeader("Status").setFlexGrow(0).setWidth("100px");
        grid.addComponentColumn(product -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            Button toggle = new Button(product.active() ? VaadinIcon.EYE_SLASH.create() : VaadinIcon.EYE.create(), e -> {
                productService.toggleActive(product.id());
                grid.getDataProvider().refreshAll();
                notify(product.name() + (product.active() ? " deactivated" : " activated"), false);
            });
            toggle.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            toggle.getElement().setAttribute("title", product.active() ? "Deactivate" : "Activate");

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(product));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(product));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(toggle, edit, delete);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setWidth("160px");
    }

    private HorizontalLayout buildToolbar() {
        searchField.setPlaceholder("Search products…");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        Button addBtn = new Button("New Product", VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        toolbar.setWidthFull();
        toolbar.expand(searchField);
        return toolbar;
    }

    private void openDialog(ProductResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Product" : "Edit Product");
        dialog.setWidth("500px");

        TextField sku = new TextField("SKU");
        TextField name = new TextField("Name");
        TextArea description = new TextArea("Description");
        description.setMinHeight("70px");

        ComboBox<ProductCategory> category = new ComboBox<>("Category");
        category.setItems(ProductCategory.values());

        NumberField unitPrice = new NumberField("Unit Price");
        unitPrice.setMin(0);
        TextField currency = new TextField("Currency");
        currency.setValue("USD");
        currency.setMaxLength(3);
        currency.setWidth("80px");

        if (existing != null) {
            sku.setValue(nvl(existing.sku()));
            name.setValue(nvl(existing.name()));
            description.setValue(nvl(existing.description()));
            if (existing.category() != null) category.setValue(existing.category());
            if (existing.unitPrice() != null) unitPrice.setValue(existing.unitPrice().doubleValue());
            if (existing.currency() != null) currency.setValue(existing.currency());
        }

        FormLayout form = new FormLayout(sku, name, category, unitPrice, currency, description);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(name, 2);
        form.setColspan(description, 2);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            if (sku.getValue().isBlank()) { sku.setInvalid(true); return; }
            if (name.getValue().isBlank()) { name.setInvalid(true); return; }
            if (unitPrice.getValue() == null) { unitPrice.setInvalid(true); return; }
            ProductRequest req = new ProductRequest(
                    sku.getValue(), name.getValue(), description.getValue(),
                    category.getValue(),
                    BigDecimal.valueOf(unitPrice.getValue()),
                    currency.getValue());
            try {
                if (existing == null) productService.create(req);
                else productService.update(existing.id(), req);
                grid.getDataProvider().refreshAll();
                dialog.close();
                notify("Product saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), save);
        dialog.open();
    }

    private void confirmDelete(ProductResponse product) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Product");
        confirm.setText("Delete \"" + product.name() + "\"?");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            productService.delete(product.id());
            grid.getDataProvider().refreshAll();
            notify("Product deleted", false);
        });
        confirm.open();
    }

    private static void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
