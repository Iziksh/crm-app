package com.crm.ui;

import com.crm.domain.enums.AddressType;
import com.crm.dto.request.AddressRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.AddressResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.service.AccountService;
import com.crm.service.AddressService;
import com.crm.service.ContactService;
import com.crm.service.TranslationService;
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
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Route(value = "addresses", layout = MainLayout.class)
@PermitAll
public class AddressesView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final AddressService addressService;
    private final AccountService accountService;
    private final ContactService contactService;
    private final Grid<AddressResponse> grid = new Grid<>(AddressResponse.class, false);

    private Long activeAccountId = null;
    private Long activeContactId = null;

    public AddressesView(TranslationService i18n, AddressService addressService,
                         AccountService accountService,
                         ContactService contactService) {
        this.i18n = i18n;
        this.addressService = addressService;
        this.accountService = accountService;
        this.contactService = contactService;
        setSizeFull();
        setPadding(true);

        configureGrid();

        HorizontalLayout toolbar = buildToolbar();
        add(new H2(i18n.translate("view.addresses.title")), toolbar, grid);
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

    @Override
    public String getPageTitle() {
        return i18n.translate("pageTitle.addresses");
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(a -> i18n.translateEnum(a.type())).setHeader(i18n.translate("common.column.type")).setSortable(true);
        grid.addColumn(AddressResponse::street).setHeader(i18n.translate("view.addresses.column.street")).setFlexGrow(2);
        grid.addColumn(AddressResponse::city).setHeader(i18n.translate("view.addresses.column.city")).setSortable(true);
        grid.addColumn(AddressResponse::state).setHeader(i18n.translate("view.addresses.column.state")).setSortable(true);
        grid.addColumn(AddressResponse::country).setHeader(i18n.translate("view.addresses.column.country")).setSortable(true);
        grid.addColumn(AddressResponse::accountName).setHeader(i18n.translate("common.column.account"));
        grid.addColumn(AddressResponse::contactName).setHeader(i18n.translate("common.column.contact"));
        grid.addComponentColumn(address -> {
            Span badge = new Span(address.enabled()
                    ? i18n.translate("view.addresses.status.active")
                    : i18n.translate("view.addresses.status.disabled"));
            badge.getElement().getThemeList().add(address.enabled() ? "badge success" : "badge error");
            return badge;
        }).setHeader(i18n.translate("common.column.status")).setFlexGrow(0).setWidth("100px");
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
        }).setHeader(i18n.translate("common.column.actions")).setFlexGrow(0).setWidth("160px");
    }

    private HorizontalLayout buildToolbar() {
        List<AccountResponse> accounts = accountService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<AccountResponse> accountFilter = new ComboBox<>(i18n.translate("view.addresses.filter.account"));
        accountFilter.setItems(accounts);
        accountFilter.setItemLabelGenerator(AccountResponse::name);
        accountFilter.setClearButtonVisible(true);
        accountFilter.setWidth("200px");

        List<ContactResponse> contacts = contactService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<ContactResponse> contactFilter = new ComboBox<>(i18n.translate("view.addresses.filter.contact"));
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

        Button addBtn = new Button(i18n.translate("view.addresses.button.new"), VaadinIcon.PLUS.create(), e -> openDialog(null));
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
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.addresses.dialog.new")
                : i18n.translate("view.addresses.dialog.edit"));
        dialog.setWidth("520px");

        ComboBox<AddressType> type = new ComboBox<>(i18n.translate("common.column.type"));
        type.setItems(AddressType.values());
        type.setItemLabelGenerator(i18n::translateEnum);

        TextField street = new TextField(i18n.translate("view.addresses.column.street"));
        TextField city = new TextField(i18n.translate("view.addresses.column.city"));
        TextField state = new TextField(i18n.translate("view.addresses.column.state"));
        TextField postalCode = new TextField(i18n.translate("view.addresses.field.postalCode"));
        TextField country = new TextField(i18n.translate("view.addresses.column.country"));

        List<AccountResponse> accounts = accountService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<AccountResponse> accountBox = new ComboBox<>(i18n.translate("common.column.account"));
        accountBox.setItems(accounts);
        accountBox.setItemLabelGenerator(AccountResponse::name);
        accountBox.setClearButtonVisible(true);

        List<ContactResponse> contacts = contactService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<ContactResponse> contactBox = new ComboBox<>(i18n.translate("common.column.contact"));
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

        Button save = new Button(i18n.translate("dialog.save"), e -> {
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
                notify(i18n.translate("notification.address.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button(i18n.translate("dialog.cancel"), e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(AddressResponse address) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(i18n.translate("view.addresses.delete.header"));
        confirm.setText(i18n.translate("view.addresses.delete.text"));
        confirm.setConfirmText(i18n.translate("dialog.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            addressService.delete(address.id());
            refreshGrid(null, null);
            notify(i18n.translate("notification.address.deleted"), false);
        });
        confirm.open();
    }

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
