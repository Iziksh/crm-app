package com.crm.ui;

import com.crm.domain.enums.AccountType;
import com.crm.dto.request.AccountRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.ImportResultResponse;
import com.crm.service.AccountService;
import com.crm.service.AttachmentService;
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

@Route(value = "accounts", layout = MainLayout.class)
@PermitAll
public class AccountsView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final AccountService accountService;
    private final ImportService importService;
    private final AttachmentService attachmentService;
    private final SecurityService securityService;
    private final Grid<AccountResponse> grid = new Grid<>(AccountResponse.class, false);
    private final TextField searchField = new TextField();

    public AccountsView(TranslationService i18n, AccountService accountService, ImportService importService,
                        AttachmentService attachmentService, SecurityService securityService) {
        this.i18n = i18n;
        this.accountService = accountService;
        this.importService = importService;
        this.attachmentService = attachmentService;
        this.securityService = securityService;
        setSizeFull();
        setPadding(true);

        configureGrid();

        HorizontalLayout toolbar = buildToolbar();
        add(new H2(i18n.translate("view.accounts.title")), toolbar, grid);
        setFlexGrow(1, grid);

        grid.setItems(DataProvider.fromCallbacks(
            query -> {
                int page = query.getLimit() > 0 ? query.getOffset() / query.getLimit() : 0;
                String search = searchField.getValue();
                return accountService.findAll(PageRequest.of(page, query.getLimit()), search).getContent().stream();
            },
            query -> (int) accountService.count(searchField.getValue())
        ));
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("pageTitle.accounts");
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(AccountResponse::name).setHeader(i18n.translate("common.column.name")).setSortable(true).setFlexGrow(2);
        grid.addColumn(AccountResponse::industry).setHeader(i18n.translate("view.accounts.column.industry")).setSortable(true);
        grid.addColumn(a -> i18n.translateEnum(a.type())).setHeader(i18n.translate("common.column.type")).setSortable(true);
        grid.addColumn(AccountResponse::email).setHeader(i18n.translate("common.column.email"));
        grid.addColumn(AccountResponse::phone).setHeader(i18n.translate("common.column.phone"));
        grid.addComponentColumn(account -> {
            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(account));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(account));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            HorizontalLayout actions = new HorizontalLayout(edit, delete);
            actions.setSpacing(false);
            return actions;
        }).setHeader(i18n.translate("common.column.actions")).setFlexGrow(0).setWidth("120px");
    }

    private HorizontalLayout buildToolbar() {
        searchField.setPlaceholder(i18n.translate("view.accounts.search.placeholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button(i18n.translate("view.accounts.button.add"), VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button exportBtn = new Button(i18n.translate("common.button.exportCsv"), VaadinIcon.DOWNLOAD.create());
        exportBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        exportBtn.addClickListener(e -> {
            StreamResource resource = new StreamResource("accounts.csv", () -> {
                String search = searchField.getValue();
                String[] headers = {"id","name","industry","website","phone","email","address","type","notes"};
                java.util.List<String[]> rows = accountService.findAllForExport(search).stream().map(a -> new String[]{
                        a.id() != null ? a.id().toString() : "", a.name() != null ? a.name() : "",
                        a.industry() != null ? a.industry() : "", a.website() != null ? a.website() : "",
                        a.phone() != null ? a.phone() : "", a.email() != null ? a.email() : "",
                        a.address() != null ? a.address() : "", a.type() != null ? a.type().name() : "",
                        a.notes() != null ? a.notes() : ""
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
        dialog.setHeaderTitle(i18n.translate("view.accounts.import.title"));
        dialog.setWidth("480px");

        Span hint = new Span(i18n.translate("view.accounts.import.hint"));
        hint.getStyle().set("font-size", "0.85em").set("color", "var(--lumo-secondary-text-color)");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("text/csv", ".csv");
        upload.setMaxFiles(1);

        VerticalLayout resultArea = new VerticalLayout();
        resultArea.setPadding(false);

        upload.addSucceededListener(event -> {
            try {
                ImportResultResponse result = importService.importAccounts(buffer.getInputStream());
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

    private void openDialog(AccountResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.accounts.dialog.new")
                : i18n.translate("view.accounts.dialog.edit"));
        dialog.setWidth("480px");

        TextField name = new TextField(i18n.translate("common.column.name"));
        TextField industry = new TextField(i18n.translate("view.accounts.column.industry"));
        TextField website = new TextField(i18n.translate("view.accounts.field.website"));
        TextField phone = new TextField(i18n.translate("common.column.phone"));
        EmailField email = new EmailField(i18n.translate("common.column.email"));
        TextField address = new TextField(i18n.translate("view.accounts.field.address"));
        ComboBox<AccountType> type = new ComboBox<>(i18n.translate("common.column.type"));
        type.setItems(AccountType.values());
        type.setItemLabelGenerator(i18n::translateEnum);
        TextField notes = new TextField(i18n.translate("common.column.notes"));

        if (existing != null) {
            name.setValue(nvl(existing.name()));
            industry.setValue(nvl(existing.industry()));
            website.setValue(nvl(existing.website()));
            phone.setValue(nvl(existing.phone()));
            email.setValue(nvl(existing.email()));
            address.setValue(nvl(existing.address()));
            type.setValue(existing.type());
            notes.setValue(nvl(existing.notes()));
        }

        FormLayout form = new FormLayout(name, industry, website, phone, email, address, type, notes);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(address, 2);
        form.setColspan(notes, 2);

        AttachmentPanel attachments = new AttachmentPanel(i18n, attachmentService, "ACCOUNT",
                existing != null ? existing.id() : null, securityService.getUsername());

        VerticalLayout body = new VerticalLayout(form, attachments);
        body.setPadding(false);
        dialog.add(body);

        Button save = new Button(i18n.translate("dialog.save"), e -> {
            if (name.getValue().isBlank()) {
                name.setInvalid(true);
                name.setErrorMessage(i18n.translate("common.validation.nameRequired"));
                return;
            }
            AccountRequest req = new AccountRequest(
                    name.getValue(), industry.getValue(), website.getValue(),
                    phone.getValue(), email.getValue(), address.getValue(),
                    type.getValue(), notes.getValue());
            try {
                AccountResponse saved;
                if (existing == null) saved = accountService.create(req);
                else { accountService.update(existing.id(), req); saved = accountService.findById(existing.id()); }
                attachments.setEntityId(saved.id());
                refreshGrid();
                dialog.close();
                notify(i18n.translate("notification.account.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button(i18n.translate("dialog.cancel"), e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(AccountResponse account) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(i18n.translate("view.accounts.delete.header"));
        confirm.setText(i18n.translate("view.accounts.delete.text", account.name()));
        confirm.setConfirmText(i18n.translate("dialog.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            accountService.delete(account.id());
            refreshGrid();
            notify(i18n.translate("notification.account.deleted"), false);
        });
        confirm.open();
    }

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
