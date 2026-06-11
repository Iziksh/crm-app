package com.crm.ui;

import com.crm.domain.enums.ContactStatus;
import com.crm.dto.request.ContactRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.dto.response.ImportResultResponse;
import com.crm.service.AccountService;
import com.crm.service.AttachmentService;
import com.crm.service.ContactService;
import com.crm.service.ImportService;
import com.crm.service.TranslationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Route(value = "contacts", layout = MainLayout.class)
@PermitAll
public class ContactsView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final ContactService contactService;
    private final AccountService accountService;
    private final ImportService importService;
    private final AttachmentService attachmentService;
    private final SecurityService securityService;
    private final Grid<ContactResponse> grid = new Grid<>(ContactResponse.class, false);
    private final TextField searchField = new TextField();

    public ContactsView(TranslationService i18n, ContactService contactService, AccountService accountService,
                        ImportService importService, AttachmentService attachmentService,
                        SecurityService securityService) {
        this.i18n = i18n;
        this.contactService = contactService;
        this.accountService = accountService;
        this.importService = importService;
        this.attachmentService = attachmentService;
        this.securityService = securityService;
        setSizeFull();
        setPadding(true);

        configureGrid();

        HorizontalLayout toolbar = buildToolbar();
        add(new H2(i18n.translate("view.contacts.title")), toolbar, grid);
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

    @Override
    public String getPageTitle() {
        return i18n.translate("pageTitle.contacts");
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(ContactResponse::firstName).setHeader(i18n.translate("view.contacts.column.firstName")).setSortable(true);
        grid.addColumn(ContactResponse::lastName).setHeader(i18n.translate("view.contacts.column.lastName")).setSortable(true);
        grid.addColumn(ContactResponse::email).setHeader(i18n.translate("common.column.email"));
        grid.addColumn(ContactResponse::phone).setHeader(i18n.translate("common.column.phone"));
        grid.addColumn(ContactResponse::jobTitle).setHeader(i18n.translate("view.contacts.column.jobTitle"));
        grid.addColumn(ContactResponse::accountName).setHeader(i18n.translate("common.column.account")).setSortable(true);
        grid.addColumn(c -> i18n.translateEnum(c.status())).setHeader(i18n.translate("common.column.status")).setSortable(true);
        grid.addComponentColumn(contact -> {
            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(contact));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(contact));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            HorizontalLayout actions = new HorizontalLayout(edit, delete);
            actions.setSpacing(false);
            return actions;
        }).setHeader(i18n.translate("common.column.actions")).setFlexGrow(0).setWidth("120px");
    }

    private HorizontalLayout buildToolbar() {
        searchField.setPlaceholder(i18n.translate("view.contacts.search.placeholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button(i18n.translate("view.contacts.button.add"), VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button exportBtn = new Button(i18n.translate("common.button.exportCsv"), VaadinIcon.DOWNLOAD.create());
        exportBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        exportBtn.addClickListener(e -> {
            StreamResource resource = new StreamResource("contacts.csv", () -> {
                String search = searchField.getValue();
                String[] headers = {"id","first_name","last_name","email","phone","job_title","department","status","account"};
                java.util.List<String[]> rows = contactService.findAllForExport(search).stream().map(c -> new String[]{
                        c.id() != null ? c.id().toString() : "", c.firstName(), c.lastName(), c.email(),
                        c.phone() != null ? c.phone() : "", c.jobTitle() != null ? c.jobTitle() : "",
                        c.department() != null ? c.department() : "", c.status() != null ? c.status().name() : "",
                        c.accountName() != null ? c.accountName() : ""
                }).toList();
                return com.crm.util.CsvExporter.build(headers, rows);
            });
            Anchor anchor = new Anchor(resource, "");
            anchor.getElement().setAttribute("download", true);
            anchor.getStyle().set("display", "none");
            add(anchor);
            anchor.getElement().executeJs("this.click(); setTimeout(() => this.remove(), 1000)");
        });

        Button importBtn = new Button(i18n.translate("common.button.importCsv"), VaadinIcon.UPLOAD.create());
        importBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        importBtn.addClickListener(e -> openImportDialog());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, exportBtn, importBtn, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        toolbar.setWidthFull();
        toolbar.expand(searchField);
        return toolbar;
    }

    private void openImportDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(i18n.translate("view.contacts.import.title"));
        dialog.setWidth("480px");

        Span hint = new Span(i18n.translate("view.contacts.import.hint"));
        hint.getStyle().set("font-size", "0.85em").set("color", "var(--lumo-secondary-text-color)");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("text/csv", ".csv");
        upload.setMaxFiles(1);

        VerticalLayout resultArea = new VerticalLayout();
        resultArea.setPadding(false);

        upload.addSucceededListener(event -> {
            try {
                ImportResultResponse result = importService.importContacts(buffer.getInputStream());
                resultArea.removeAll();
                resultArea.add(new Span(i18n.translate("view.accounts.import.result",
                        result.imported(), result.skipped())));
                if (!result.errors().isEmpty()) {
                    result.errors().stream().limit(5).forEach(err ->
                            resultArea.add(new Span(i18n.translate("common.import.errorLine", err))));
                }
                refreshGrid();
            } catch (Exception ex) {
                resultArea.removeAll();
                resultArea.add(new Span(i18n.translate("view.accounts.import.error", ex.getMessage())));
            }
        });

        dialog.add(hint, upload, resultArea);
        dialog.getFooter().add(new Button(i18n.translate("dialog.close"), e2 -> dialog.close()));
        dialog.open();
    }

    private void refreshGrid() {
        grid.getDataProvider().refreshAll();
    }

    private void openDialog(ContactResponse existing) {
        List<AccountResponse> accounts = accountService.findAll(PageRequest.of(0, 1000)).getContent();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.contacts.dialog.new")
                : i18n.translate("view.contacts.dialog.edit"));
        dialog.setWidth("480px");

        TextField firstName = new TextField(i18n.translate("view.contacts.column.firstName"));
        TextField lastName = new TextField(i18n.translate("view.contacts.column.lastName"));
        EmailField email = new EmailField(i18n.translate("common.column.email"));
        TextField phone = new TextField(i18n.translate("common.column.phone"));
        TextField jobTitle = new TextField(i18n.translate("view.contacts.column.jobTitle"));
        TextField department = new TextField(i18n.translate("view.contacts.field.department"));
        ComboBox<ContactStatus> status = new ComboBox<>(i18n.translate("common.column.status"));
        status.setItems(ContactStatus.values());
        status.setItemLabelGenerator(i18n::translateEnum);
        ComboBox<AccountResponse> account = new ComboBox<>(i18n.translate("common.column.account"));
        account.setItems(accounts);
        account.setItemLabelGenerator(AccountResponse::name);
        account.setClearButtonVisible(true);
        TextField notes = new TextField(i18n.translate("common.column.notes"));

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

        AttachmentPanel attachments = new AttachmentPanel(i18n, attachmentService, "CONTACT",
                existing != null ? existing.id() : null, securityService.getUsername());

        VerticalLayout body = new VerticalLayout(form, attachments);
        body.setPadding(false);
        dialog.add(body);

        Button save = new Button(i18n.translate("dialog.save"), e -> {
            if (firstName.getValue().isBlank() || lastName.getValue().isBlank() || email.getValue().isBlank()) {
                notify(i18n.translate("view.contacts.validation.required"), true);
                return;
            }
            AccountResponse selectedAccount = account.getValue();
            ContactRequest req = new ContactRequest(
                    firstName.getValue(), lastName.getValue(), email.getValue(),
                    phone.getValue(), jobTitle.getValue(), department.getValue(),
                    status.getValue(), notes.getValue(),
                    selectedAccount != null ? selectedAccount.id() : null);
            try {
                ContactResponse saved;
                if (existing == null) saved = contactService.create(req);
                else { contactService.update(existing.id(), req); saved = contactService.findById(existing.id()); }
                attachments.setEntityId(saved.id());
                refreshGrid();
                dialog.close();
                notify(i18n.translate("notification.contact.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button(i18n.translate("dialog.cancel"), e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(ContactResponse contact) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(i18n.translate("view.contacts.delete.header"));
        confirm.setText(i18n.translate("view.contacts.delete.text",
                contact.firstName() + " " + contact.lastName()));
        confirm.setConfirmText(i18n.translate("dialog.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            contactService.delete(contact.id());
            refreshGrid();
            notify(i18n.translate("notification.contact.deleted"), false);
        });
        confirm.open();
    }

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
