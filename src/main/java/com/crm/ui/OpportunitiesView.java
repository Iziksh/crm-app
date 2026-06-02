package com.crm.ui;

import com.crm.domain.enums.OpportunityStage;
import com.crm.dto.request.OpportunityRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.dto.response.OpportunityResponse;
import com.crm.service.AccountService;
import com.crm.service.ContactService;
import com.crm.service.OpportunityService;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "opportunities", layout = MainLayout.class)
@PageTitle("Opportunities | CRM")
@RolesAllowed({"SALES", "ADMIN"})
public class OpportunitiesView extends VerticalLayout {

    private final OpportunityService opportunityService;
    private final AccountService accountService;
    private final ContactService contactService;

    private final Grid<OpportunityResponse> grid = new Grid<>(OpportunityResponse.class, false);
    private final ComboBox<OpportunityStage> stageFilter = new ComboBox<>("Stage");
    private final TextField searchField = new TextField();

    public OpportunitiesView(OpportunityService opportunityService,
                             AccountService accountService,
                             ContactService contactService) {
        this.opportunityService = opportunityService;
        this.accountService = accountService;
        this.contactService = contactService;
        setSizeFull();
        setPadding(true);

        configureGrid();
        HorizontalLayout toolbar = buildToolbar();
        add(new H2("Opportunities"), toolbar, grid);
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

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(OpportunityResponse::name).setHeader("Name").setSortable(true).setFlexGrow(2);
        grid.addColumn(OpportunityResponse::accountName).setHeader("Account").setSortable(true);
        grid.addComponentColumn(o -> stageBadge(o.stage())).setHeader("Stage").setFlexGrow(0).setWidth("130px");
        grid.addColumn(o -> o.amount() != null ? o.currency() + " " + o.amount() : "")
                .setHeader("Amount").setSortable(true);
        grid.addColumn(o -> o.probability() != null ? o.probability() + "%" : "")
                .setHeader("Probability");
        grid.addColumn(o -> o.weightedAmount() != null
                ? o.currency() + " " + String.format("%.2f", o.weightedAmount()) : "")
                .setHeader("Weighted");
        grid.addColumn(o -> o.closeDate() != null ? o.closeDate().toString() : "")
                .setHeader("Close Date").setSortable(true);
        grid.addColumn(OpportunityResponse::assignedToName).setHeader("Assigned To");
        grid.addComponentColumn(opp -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            Button edit = new Button(VaadinIcon.EDIT.create(), e -> openDialog(opp));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(opp));
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            actions.add(edit, delete);
            return actions;
        }).setHeader("Actions").setFlexGrow(0).setWidth("110px");
    }

    private HorizontalLayout buildToolbar() {
        stageFilter.setItems(OpportunityStage.values());
        stageFilter.setPlaceholder("All Stages");
        stageFilter.setClearButtonVisible(true);
        stageFilter.setWidth("160px");
        stageFilter.addValueChangeListener(e -> refreshGrid());

        searchField.setPlaceholder("Search name…");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button("New Opportunity", VaadinIcon.PLUS.create(), e -> openDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(stageFilter, searchField, addBtn);
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
        dialog.setHeaderTitle(existing == null ? "New Opportunity" : "Edit Opportunity");
        dialog.setWidth("600px");

        TextField name = new TextField("Name");

        ComboBox<OpportunityStage> stage = new ComboBox<>("Stage");
        stage.setItems(OpportunityStage.values());
        stage.setValue(OpportunityStage.PROSPECTING);

        NumberField amount = new NumberField("Amount");
        TextField currency = new TextField("Currency");
        currency.setValue("USD");

        IntegerField probability = new IntegerField("Probability (%)");
        probability.setMin(0);
        probability.setMax(100);
        probability.setValue(10);

        DatePicker closeDate = new DatePicker("Close Date");

        List<AccountResponse> accounts = accountService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<AccountResponse> account = new ComboBox<>("Account");
        account.setItems(accounts);
        account.setItemLabelGenerator(AccountResponse::name);
        account.setClearButtonVisible(true);

        List<ContactResponse> contacts = contactService.findAll(PageRequest.of(0, 500)).getContent();
        ComboBox<ContactResponse> contact = new ComboBox<>("Contact");
        contact.setItems(contacts);
        contact.setItemLabelGenerator(c -> c.firstName() + " " + c.lastName());
        contact.setClearButtonVisible(true);

        TextArea notes = new TextArea("Notes");
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

        Button save = new Button("Save", e -> {
            if (name.getValue().isBlank()) {
                name.setInvalid(true);
                name.setErrorMessage("Name is required");
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
                notify("Opportunity saved", false);
            } catch (Exception ex) {
                notify(ex.getMessage(), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDelete(OpportunityResponse opp) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete Opportunity");
        confirm.setText("Delete \"" + opp.name() + "\"?");
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.addConfirmListener(e -> {
            opportunityService.delete(opp.id());
            refreshGrid();
            notify("Opportunity deleted", false);
        });
        confirm.open();
    }

    private Span stageBadge(OpportunityStage stage) {
        if (stage == null) return new Span();
        Span badge = new Span(stage.name());
        String theme = switch (stage) {
            case WON -> "badge success";
            case LOST -> "badge error";
            case NEGOTIATION -> "badge primary";
            default -> "badge contrast";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    private static void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
