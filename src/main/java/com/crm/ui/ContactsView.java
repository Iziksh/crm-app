package com.crm.ui;

import com.crm.domain.enums.ContactStatus;
import com.crm.dto.request.ContactRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.service.AccountService;
import com.crm.service.ContactService;
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
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Route(value = "contacts", layout = MainLayout.class)
@PageTitle("Contacts | CRM")
@PermitAll
public class ContactsView extends VerticalLayout {

    private final ContactService contactService;
    private final AccountService accountService;
    private final Grid<ContactResponse> grid = new Grid<>(ContactResponse.class, false);
    private final TextField searchField = new TextField();

    public ContactsView(ContactService contactService, AccountService accountService) {
        this.contactService = contactService;
        this.accountService = accountService;
        setSizeFull();
        setPadding(true);

        configureGrid();

        HorizontalLayout toolbar = buildToolbar();
        add(new H2("Contacts"), toolbar, grid);
        setFlexGrow(1, grid);

        grid.setItems(DataProvider.fromCallbacks(
            query -> {
                int page = query.getLimit() > 0 ? query.getOffset() / query.getLimit() : 0;
                String search = searchField.getValue();
                if (search != null && !search.isBlank()) {
                    return contactService.search(search, PageRequest.of(page, query.getLimit())).getContent().stream();
                }
                return contactService.findAll(PageRequest.of(page, query.getLimit())).getContent().stream();
            },
            query -> (int) contactService.count(searchField.getValue())
        ));
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(ContactResponse::firstName).setHeader("First Name").setSortable(true);
        grid.addColumn(ContactResponse::lastName).setHeader("Last Name").setSortable(true);
        grid.addColumn(ContactResponse::email).setHeader("Email");
        grid.addColumn(ContactResponse::phone).setHeader("Phone");
        grid.addColumn(ContactResponse::jobTitle).setHeader("Job Title");
        grid.addColumn(ContactResponse::accountName).setHeader("Account").setSortable(true);
        grid.addColumn(ContactResponse::status).setHeader("Status").setSortable(true);
        grid.addComponentColumn(contact -> {
            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(contact));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(contact));
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

        Button addBtn = new Button("Add Contact", VaadinIcon.PLUS.create(), e -> openDialog(null));
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

    private void openDialog(ContactResponse existing) {
        List<AccountResponse> accounts = accountService.findAll(PageRequest.of(0, 1000)).getContent();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Contact" : "Edit Contact");
        dialog.setWidth("480px");

        TextField firstName = new TextField("First Name");
        TextField lastName = new TextField("Last Name");
        EmailField email = new EmailField("Email");
        TextField phone = new TextField("Phone");
        TextField jobTitle = new TextField("Job Title");
        TextField department = new TextField("Department");
        ComboBox<ContactStatus> status = new ComboBox<>("Status");
        status.setItems(ContactStatus.values());
        ComboBox<AccountResponse> account = new ComboBox<>("Account");
        account.setItems(accounts);
        account.setItemLabelGenerator(AccountResponse::name);
        account.setClearButtonVisible(true);
        TextField notes = new TextField("Notes");

        if (existing != null) {
            firstName.setValue(nvl(existing.firstName()));
            lastName.setValue(nvl(existing.lastName()));
            email.setValue(nvl(existing.email()));
            phone.setValue(nvl(existing.phone()));
            jobTitle.setValue(nvl(existing.jobTitle()));
            department.setValue(nvl(existing.department()));
            status.setValue(existing.status());
            notes.setValue(nvl(existing.notes()));
            if (existing.accountId() != null) {
                accounts.stream().filter(a -> a.id().equals(existing.accountId()))
                        .findFirst().ifPresent(account::setValue);
            }
        }

        FormLayout form = new FormLayout(firstName, lastName, email, phone, jobTitle, department, status, account, notes);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(notes, 2);
        dialog.add(form);

        Button save = new Button("Save", e -> {
            if (firstName.getValue().isBlank() || lastName.getValue().isBlank() || email.getValue().isBlank()) {
                notify("First name, last name and email are required", true);
                return;
            }
            AccountResponse selectedAccount = account.getValue();
            ContactRequest req = new ContactRequest(
                    firstName.getValue(), lastName.getValue(), email.getValue(),
                    phone.getValue(), jobTitle.getValue(), department.getValue(),
                    status.getValue(), notes.getValue(),
                    selectedAccount != null ? selectedAccount.id() : null);
            try {
                if (existing == null) contactService.create(req);
                else contactService.update(existing.id(), req);
                refreshGrid();
                dialog.close();
                notify("Contact saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(ContactResponse contact) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Contact");
        confirm.setText("Delete \"" + contact.firstName() + " " + contact.lastName() + "\"?");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            contactService.delete(contact.id());
            refreshGrid();
            notify("Contact deleted", false);
        });
        confirm.open();
    }

    private static void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
