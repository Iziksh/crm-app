package com.crm.ui;

import com.crm.domain.enums.AddressType;
import com.crm.dto.request.AddressRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.AddressResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.service.AccountService;
import com.crm.service.AddressService;
import com.crm.service.ContactService;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.data.provider.DataProvider;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Route(value = "addresses", layout = MainLayout.class)
@PageTitle("Addresses | CRM")
@PermitAll
public class AddressesView extends VerticalLayout {

    private final AddressService addressService;
    private final AccountService accountService;
    private final ContactService contactService;
    private final Grid<AddressResponse> grid = new Grid<>(AddressResponse.class, false);

    private Long activeAccountId = null;
    private Long activeContactId = null;

    public AddressesView(AddressService addressService,
                         AccountService accountService,
                         ContactService contactService) {
        this.addressService = addressService;
        this.accountService = accountService;
        this.contactService = contactService;
        setSizeFull();
        setPadding(true);

        configureGrid();

        HorizontalLayout toolbar = buildToolbar();
        add(new H2("Addresses"), toolbar, grid);
        setFlexGrow(1, grid);

        grid.setItems(DataProvider.fromCallbacks(
            query -> {
                if (activeAccountId != null) return addressService.findByAccount(activeAccountId).stream();
                if (activeContactId != null) return addressService.findByContact(activeContactId).stream();
                int page = query.getLimit() > 0 ? query.getOffset() / query.getLimit() : 0;
                return addressService.findAll(PageRequest.of(page, query.getLimit())).getContent().stream();
            },
            query -> {
                if (activeAccountId != null) return addressService.findByAccount(activeAccountId).size();
                if (activeContactId != null) return addressService.findByContact(activeContactId).size();
                return (int) addressService.count();
            }
        ));
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(AddressResponse::type).setHeader("Type").setSortable(true);
        grid.addColumn(AddressResponse::street).setHeader("Street").setFlexGrow(2);
        grid.addColumn(AddressResponse::city).setHeader("City").setSortable(true);
        grid.addColumn(AddressResponse::state).setHeader("State").setSortable(true);
        grid.addColumn(AddressResponse::country).setHeader("Country").setSortable(true);
        grid.addColumn(AddressResponse::accountName).setHeader("Account");
        grid.addColumn(AddressResponse::contactName).setHeader("Contact");
        grid.addComponentColumn(address -> {
            Span badge = new Span(address.enabled() ? "Active" : "Disabled");
            badge.getElement().getThemeList().add(address.enabled() ? "badge success" : "badge error");
            return badge;
        }).setHeader("Status").setFlexGrow(0).setWidth("100px");
        grid.addComponentColumn(address -> {
            Button toggle = new Button(address.enabled() ? VaadinIcon.EYE_SLASH.create() : VaadinIcon.EYE.create(),
                    e -> {
                        addressService.toggleEnabled(address.id());
                        refreshGrid(null, null);
                    });
            toggle.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(address));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(address));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            HorizontalLayout actions = new HorizontalLayout(toggle, edit, delete);
            actions.setSpacing(false);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setWidth("160px");
    }

    private HorizontalLayout buildToolbar() {
        List<AccountResponse> accounts = accountService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<AccountResponse> accountFilter = new ComboBox<>("Filter by Account");
        accountFilter.setItems(accounts);
        accountFilter.setItemLabelGenerator(AccountResponse::name);
        accountFilter.setClearButtonVisible(true);
        accountFilter.setWidth("200px");

        List<ContactResponse> contacts = contactService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<ContactResponse> contactFilter = new ComboBox<>("Filter by Contact");
        contactFilter.setItems(contacts);
        contactFilter.setItemLabelGenerator(c -> c.firstName() + " " + c.lastName());
        contactFilter.setClearButtonVisible(true);
        contactFilter.setWidth("200px");

        accountFilter.addValueChangeListener(e -> {
            contactFilter.clear();
            activeAccountId = e.getValue() != null ? e.getValue().id() : null;
            activeContactId = null;
            refreshGrid(null, null);
        });
        contactFilter.addValueChangeListener(e -> {
            accountFilter.clear();
            activeContactId = e.getValue() != null ? e.getValue().id() : null;
            activeAccountId = null;
            refreshGrid(null, null);
        });

        Button addBtn = new Button("New Address", VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(accountFilter, contactFilter, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        return toolbar;
    }

    private void refreshGrid(Long accountId, Long contactId) {
        grid.getDataProvider().refreshAll();
    }

    private void openDialog(AddressResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Address" : "Edit Address");
        dialog.setWidth("520px");

        ComboBox<AddressType> type = new ComboBox<>("Type");
        type.setItems(AddressType.values());

        TextField street = new TextField("Street");
        TextField city = new TextField("City");
        TextField state = new TextField("State");
        TextField postalCode = new TextField("Postal Code");
        TextField country = new TextField("Country");

        List<AccountResponse> accounts = accountService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<AccountResponse> accountBox = new ComboBox<>("Account");
        accountBox.setItems(accounts);
        accountBox.setItemLabelGenerator(AccountResponse::name);
        accountBox.setClearButtonVisible(true);

        List<ContactResponse> contacts = contactService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<ContactResponse> contactBox = new ComboBox<>("Contact");
        contactBox.setItems(contacts);
        contactBox.setItemLabelGenerator(c -> c.firstName() + " " + c.lastName());
        contactBox.setClearButtonVisible(true);

        accountBox.addValueChangeListener(e -> { if (e.getValue() != null) contactBox.clear(); });
        contactBox.addValueChangeListener(e -> { if (e.getValue() != null) accountBox.clear(); });

        if (existing != null) {
            type.setValue(existing.type());
            street.setValue(nvl(existing.street()));
            city.setValue(nvl(existing.city()));
            state.setValue(nvl(existing.state()));
            postalCode.setValue(nvl(existing.postalCode()));
            country.setValue(nvl(existing.country()));
            if (existing.accountId() != null) {
                accounts.stream().filter(a -> a.id().equals(existing.accountId()))
                        .findFirst().ifPresent(accountBox::setValue);
            }
            if (existing.contactId() != null) {
                contacts.stream().filter(c -> c.id().equals(existing.contactId()))
                        .findFirst().ifPresent(contactBox::setValue);
            }
        }

        FormLayout form = new FormLayout(type, street, city, state, postalCode, country, accountBox, contactBox);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(street, 2);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            Long accountId = accountBox.getValue() != null ? accountBox.getValue().id() : null;
            Long contactId = contactBox.getValue() != null ? contactBox.getValue().id() : null;
            AddressRequest req = new AddressRequest(
                    type.getValue(), street.getValue(), city.getValue(),
                    state.getValue(), postalCode.getValue(), country.getValue(),
                    accountId, contactId);
            try {
                if (existing == null) addressService.create(req);
                else addressService.update(existing.id(), req);
                refreshGrid(null, null);
                dialog.close();
                notify("Address saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(AddressResponse address) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Address");
        confirm.setText("Delete this address?");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            addressService.delete(address.id());
            refreshGrid(null, null);
            notify("Address deleted", false);
        });
        confirm.open();
    }

    private static void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
