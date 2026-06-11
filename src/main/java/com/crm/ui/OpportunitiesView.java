package com.crm.ui;

import com.crm.domain.enums.OpportunityStage;
import com.crm.dto.request.OpportunityRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.dto.response.OpportunityResponse;
import com.crm.service.AccountService;
import com.crm.service.ContactService;
import com.crm.service.OpportunityService;
import com.crm.service.TranslationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "opportunities", layout = MainLayout.class)
@RolesAllowed({"SALES", "ADMIN"})
public class OpportunitiesView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final OpportunityService opportunityService;
    private final AccountService accountService;
    private final ContactService contactService;

    private final Grid<OpportunityResponse> grid = new Grid<>(OpportunityResponse.class, false);
    private final ComboBox<OpportunityStage> stageFilter = new ComboBox<>();
    private final TextField searchField = new TextField();

    public OpportunitiesView(OpportunityService opportunityService,
                             AccountService accountService,
                             ContactService contactService,
                             TranslationService i18n) {
        this.opportunityService = opportunityService;
        this.accountService = accountService;
        this.contactService = contactService;
        this.i18n = i18n;
        setSizeFull();
        setPadding(true);

        configureGrid();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2(i18n.translate("view.opportunities.title")), toolbar, grid);
        setFlexGrow(1, grid);

        grid.setItems(DataProvider.fromCallbacks(
            query -> {
                int page = query.getLimit() > 0 ? query.getOffset() / query.getLimit() : 0;
                return opportunityService.findAll(
                    PageRequest.of(page, query.getLimit()),
                    stageFilter.getValue(), searchField.getValue()
                ).getContent().stream();
            },
            query -> (int) opportunityService.count(stageFilter.getValue(), searchField.getValue())
        ));
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("page.opportunities");
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(OpportunityResponse::name).setHeader(i18n.translate("common.name")).setSortable(true).setFlexGrow(2);
        grid.addColumn(OpportunityResponse::accountName).setHeader(i18n.translate("common.account")).setSortable(true);
        grid.addComponentColumn(o -> stageBadge(o.stage())).setHeader(i18n.translate("common.stage")).setFlexGrow(0).setWidth("130px");
        grid.addColumn(o -> o.amount() != null ? o.currency() + " " + o.amount() : "")
                .setHeader(i18n.translate("common.amount")).setSortable(true);
        grid.addColumn(o -> o.probability() != null ? o.probability() + "%" : "")
                .setHeader(i18n.translate("common.probability"));
        grid.addColumn(o -> o.weightedAmount() != null
                ? o.currency() + " " + String.format("%.2f", o.weightedAmount()) : "")
                .setHeader(i18n.translate("common.weighted"));
        grid.addColumn(o -> o.closeDate() != null ? o.closeDate().toString() : "")
                .setHeader(i18n.translate("common.closeDate")).setSortable(true);
        grid.addColumn(OpportunityResponse::assignedToName).setHeader(i18n.translate("common.assignedTo"));
        grid.addComponentColumn(opp -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(opp));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(opp));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(edit, delete);
            return actions;
        }).setHeader(i18n.translate("common.actions")).setFlexGrow(0).setWidth("110px");
    }

    private HorizontalLayout buildToolbar() {
        stageFilter.setLabel(i18n.translate("common.stage"));
        stageFilter.setItems(OpportunityStage.values());
        stageFilter.setItemLabelGenerator(i18n::translateEnum);
        stageFilter.setPlaceholder(i18n.translate("view.opportunities.allStages"));
        stageFilter.setClearButtonVisible(true);
        stageFilter.setWidth("160px");
        stageFilter.addValueChangeListener(e -> refreshGrid());

        searchField.setPlaceholder(i18n.translate("view.opportunities.searchPlaceholder"));
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button(i18n.translate("view.opportunities.newOpportunity"), VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button exportBtn = new Button(i18n.translate("common.exportCsv"), VaadinIcon.DOWNLOAD.create());
        exportBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        exportBtn.addClickListener(e -> {
            com.vaadin.flow.server.StreamResource resource = new com.vaadin.flow.server.StreamResource("opportunities.csv", () -> {
                String[] headers = {"id","name","stage","amount","currency","probability","close_date","account","assigned_to"};
                java.util.List<String[]> rows = opportunityService.findAllForExport(stageFilter.getValue(), searchField.getValue()).stream().map(o -> new String[]{
                        o.id() != null ? o.id().toString() : "", o.name(), o.stage() != null ? o.stage().name() : "",
                        o.amount() != null ? o.amount().toString() : "", o.currency() != null ? o.currency() : "",
                        o.probability() != null ? o.probability().toString() : "", o.closeDate() != null ? o.closeDate().toString() : "",
                        o.accountName() != null ? o.accountName() : "", o.assignedToName() != null ? o.assignedToName() : ""
                }).toList();
                return com.crm.util.CsvExporter.build(headers, rows);
            });
            com.vaadin.flow.component.html.Anchor anchor = new com.vaadin.flow.component.html.Anchor(resource, "");
            anchor.getElement().setAttribute("download", true);
            anchor.getStyle().set("display", "none");
            add(anchor);
            anchor.getElement().executeJs("this.click(); setTimeout(() => this.remove(), 1000)");
        });

        HorizontalLayout toolbar = new HorizontalLayout(stageFilter, searchField, exportBtn, addBtn);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.END);
        toolbar.setWidthFull();
        toolbar.expand(searchField);
        return toolbar;
    }

    private void refreshGrid() {
        grid.getDataProvider().refreshAll();
    }

    private void openDialog(OpportunityResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.opportunities.newOpportunity")
                : i18n.translate("view.opportunities.editOpportunity"));
        dialog.setWidth("600px");

        TextField name = new TextField(i18n.translate("common.name"));

        ComboBox<OpportunityStage> stage = new ComboBox<>(i18n.translate("common.stage"));
        stage.setItems(OpportunityStage.values());
        stage.setItemLabelGenerator(i18n::translateEnum);
        stage.setValue(OpportunityStage.PROSPECTING);

        NumberField amount = new NumberField(i18n.translate("common.amount"));
        TextField currency = new TextField(i18n.translate("common.currency"));
        currency.setValue("USD");

        IntegerField probability = new IntegerField(i18n.translate("view.opportunities.probabilityPercent"));
        probability.setMin(0);
        probability.setMax(100);
        probability.setValue(10);

        DatePicker closeDate = new DatePicker(i18n.translate("common.closeDate"));

        List<AccountResponse> accounts = accountService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<AccountResponse> account = new ComboBox<>(i18n.translate("common.account"));
        account.setItems(accounts);
        account.setItemLabelGenerator(AccountResponse::name);
        account.setClearButtonVisible(true);

        List<ContactResponse> contacts = contactService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<ContactResponse> contact = new ComboBox<>(i18n.translate("common.contact"));
        contact.setItems(contacts);
        contact.setItemLabelGenerator(c -> c.firstName() + " " + c.lastName());
        contact.setClearButtonVisible(true);

        TextArea notes = new TextArea(i18n.translate("common.notes"));
        notes.setMinHeight("80px");

        if (existing != null) {
            name.setValue(nvl(existing.name()));
            if (existing.stage() != null) stage.setValue(existing.stage());
            if (existing.amount() != null) amount.setValue(existing.amount().doubleValue());
            if (existing.currency() != null) currency.setValue(existing.currency());
            if (existing.probability() != null) probability.setValue(existing.probability());
            if (existing.closeDate() != null) closeDate.setValue(existing.closeDate());
            if (existing.accountId() != null) {
                accounts.stream().filter(a -> a.id().equals(existing.accountId()))
                        .findFirst().ifPresent(account::setValue);
            }
            if (existing.contactId() != null) {
                contacts.stream().filter(c -> c.id().equals(existing.contactId()))
                        .findFirst().ifPresent(contact::setValue);
            }
        }

        FormLayout form = new FormLayout(name, stage, amount, currency, probability, closeDate,
                account, contact, notes);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(name, 2);
        form.setColspan(notes, 2);
        dialog.add(form);

        Button save = new Button(i18n.translate("common.save"), e -> {
            if (name.getValue().isBlank()) {
                name.setInvalid(true);
                name.setErrorMessage(i18n.translate("view.opportunities.validation.nameRequired"));
                return;
            }
            BigDecimal amt = amount.getValue() != null ? BigDecimal.valueOf(amount.getValue()) : null;
            OpportunityRequest req = new OpportunityRequest(
                    name.getValue(), stage.getValue(), amt, currency.getValue(),
                    probability.getValue(), closeDate.getValue(), notes.getValue(),
                    null,
                    account.getValue() != null ? account.getValue().id() : null,
                    contact.getValue() != null ? contact.getValue().id() : null,
                    null);
            try {
                if (existing == null) opportunityService.create(req, "admin");
                else opportunityService.update(existing.id(), req);
                refreshGrid();
                dialog.close();
                notify(i18n.translate("view.opportunities.notification.saved"), false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button(i18n.translate("common.cancel"), e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(OpportunityResponse opp) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader(i18n.translate("view.opportunities.deleteOpportunity"));
        confirm.setText(i18n.translate("dialog.deleteConfirm", opp.name()));
        confirm.setConfirmText(i18n.translate("common.delete"));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            opportunityService.delete(opp.id());
            refreshGrid();
            notify(i18n.translate("view.opportunities.notification.deleted"), false);
        });
        confirm.open();
    }

    private Span stageBadge(OpportunityStage stage) {
        if (stage == null) return new Span();
        Span badge = new Span(i18n.translateEnum(stage));
        String theme = switch (stage) {
            case WON -> "badge success";
            case LOST -> "badge error";
            case NEGOTIATION -> "badge primary";
            default -> "badge contrast";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
