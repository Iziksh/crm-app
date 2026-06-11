package com.crm.ui;

import com.crm.domain.enums.SubscriptionEventType;
import com.crm.dto.request.SubscriptionRequest;
import com.crm.dto.response.SubscriptionResponse;
import com.crm.dto.response.TopicResponse;
import com.crm.service.SubscriptionService;
import com.crm.service.TopicService;
import com.crm.service.TranslationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "subscriptions", layout = MainLayout.class)
@PermitAll
public class SubscriptionsView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final SubscriptionService subscriptionService;
    private final TopicService topicService;
    private final SecurityService securityService;

    private final Grid<SubscriptionResponse> grid = new Grid<>(SubscriptionResponse.class, false);

    public SubscriptionsView(SubscriptionService subscriptionService,
                              TopicService topicService,
                              SecurityService securityService,
                              TranslationService i18n) {
        this.subscriptionService = subscriptionService;
        this.topicService = topicService;
        this.securityService = securityService;
        this.i18n = i18n;

        setSizeFull();
        setPadding(true);

        configureGrid();

        Button newBtn = new Button(i18n.translate("view.subscriptions.button.newSubscription"),
                e -> openDialog(null));
        newBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new HorizontalLayout(newBtn), grid);
        refresh();
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("page.subscriptions");
    }

    private void configureGrid() {
        grid.addColumn(SubscriptionResponse::name).setHeader(i18n.translate("common.name")).setFlexGrow(2);
        grid.addColumn(SubscriptionResponse::topicName).setHeader(i18n.translate("common.topic"));
        grid.addColumn(SubscriptionResponse::entityType).setHeader(i18n.translate("common.entityType"));
        grid.addComponentColumn(s -> {
            Span badge = new Span(s.active()
                    ? i18n.translate("common.active")
                    : i18n.translate("common.inactive"));
            badge.getElement().getThemeList().add(s.active() ? "badge success" : "badge contrast");
            return badge;
        }).setHeader(i18n.translate("common.status"));
        grid.addComponentColumn(s -> {
            Button edit = new Button(i18n.translate("common.edit"), e -> openDialog(s));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button delete = new Button(i18n.translate("common.delete"), e -> {
                subscriptionService.delete(s.id());
                refresh();
                Notification.show(i18n.translate("view.subscriptions.notification.deleted"),
                        2000, Notification.Position.BOTTOM_CENTER);
            });
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            return new HorizontalLayout(edit, delete);
        }).setHeader(i18n.translate("common.actions"));
        grid.setWidthFull();
    }

    private void openDialog(SubscriptionResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? i18n.translate("view.subscriptions.dialog.new")
                : i18n.translate("view.subscriptions.dialog.edit"));
        dialog.setWidth("600px");

        TextField nameField = new TextField(i18n.translate("common.name"));
        nameField.setWidthFull();
        if (existing != null) nameField.setValue(existing.name() != null ? existing.name() : "");

        TextArea descField = new TextArea(i18n.translate("common.description"));
        descField.setWidthFull();
        if (existing != null && existing.description() != null) descField.setValue(existing.description());

        Checkbox activeCheck = new Checkbox(i18n.translate("view.subscriptions.field.active"),
                existing == null || existing.active());

        List<TopicResponse> topics = topicService.findAll();
        ComboBox<TopicResponse> topicBox = new ComboBox<>(i18n.translate("common.topic"));
        topicBox.setItems(topics);
        topicBox.setItemLabelGenerator(t -> t.name() + " (" + t.entityType() + ")");
        topicBox.setWidthFull();
        if (existing != null) topics.stream().filter(t -> t.id().equals(existing.topicId())).findFirst()
                .ifPresent(topicBox::setValue);

        MultiSelectComboBox<SubscriptionEventType> eventTypesBox =
                new MultiSelectComboBox<>(i18n.translate("common.eventTypes"));
        eventTypesBox.setItems(SubscriptionEventType.values());
        eventTypesBox.setItemLabelGenerator(i18n::translateEnum);
        eventTypesBox.setHelperText(i18n.translate("view.subscriptions.eventTypes.helper"));
        eventTypesBox.setWidthFull();
        if (existing != null && existing.eventTypes() != null) eventTypesBox.setValue(existing.eventTypes());

        FormLayout filterLayout = new FormLayout();
        filterLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        TextField[] filterNames = new TextField[5];
        TextField[] filterValues = new TextField[5];
        String[] existingNames = existing == null ? new String[5] :
                new String[]{existing.filterName0(), existing.filterName1(), existing.filterName2(),
                        existing.filterName3(), existing.filterName4()};
        String[] existingValues = existing == null ? new String[5] :
                new String[]{existing.filterValue0(), existing.filterValue1(), existing.filterValue2(),
                        existing.filterValue3(), existing.filterValue4()};
        for (int i = 0; i < 5; i++) {
            filterNames[i] = new TextField(i18n.translate("view.subscriptions.filter.field", i));
            filterValues[i] = new TextField(i18n.translate("view.subscriptions.filter.value", i));
            if (existingNames[i] != null) filterNames[i].setValue(existingNames[i]);
            if (existingValues[i] != null) filterValues[i].setValue(existingValues[i]);
            filterLayout.add(filterNames[i], filterValues[i]);
        }

        VerticalLayout body = new VerticalLayout(nameField, descField, activeCheck, topicBox, eventTypesBox, filterLayout);
        body.setPadding(false);
        dialog.add(body);

        Button save = new Button(i18n.translate("common.save"), e -> {
            if (nameField.getValue().isBlank()) {
                nameField.setInvalid(true);
                return;
            }
            if (topicBox.getValue() == null) {
                topicBox.setInvalid(true);
                return;
            }
            SubscriptionRequest req = new SubscriptionRequest(
                    nameField.getValue(), descField.getValue(), activeCheck.getValue(),
                    topicBox.getValue().id(),
                    new ArrayList<>(eventTypesBox.getValue()),
                    filterNames[0].getValue(), filterValues[0].getValue(),
                    filterNames[1].getValue(), filterValues[1].getValue(),
                    filterNames[2].getValue(), filterValues[2].getValue(),
                    filterNames[3].getValue(), filterValues[3].getValue(),
                    filterNames[4].getValue(), filterValues[4].getValue()
            );
            try {
                if (existing == null) {
                    subscriptionService.create(req, securityService.getUsername());
                } else {
                    subscriptionService.update(existing.id(), req, securityService.getUsername());
                }
                dialog.close();
                refresh();
                Notification.show(i18n.translate("view.subscriptions.notification.saved"),
                        2000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show(i18n.translate("notification.errorPrefix", ex.getMessage()),
                        3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button(i18n.translate("common.cancel"), ev -> dialog.close()), save);
        dialog.open();
    }

    private void refresh() {
        grid.setItems(subscriptionService.findAllForUser(securityService.getUsername()));
    }
}
