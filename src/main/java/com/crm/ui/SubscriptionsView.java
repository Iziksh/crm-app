package com.crm.ui;

import com.crm.domain.enums.SubscriptionEventType;
import com.crm.dto.request.SubscriptionRequest;
import com.crm.dto.response.SubscriptionResponse;
import com.crm.dto.response.TopicResponse;
import com.crm.service.SubscriptionService;
import com.crm.service.TopicService;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@Route(value = "subscriptions", layout = MainLayout.class)
@PageTitle("Subscriptions | CRM")
@PermitAll
public class SubscriptionsView extends VerticalLayout {

    private final SubscriptionService subscriptionService;
    private final TopicService topicService;
    private final SecurityService securityService;

    private final Grid<SubscriptionResponse> grid = new Grid<>(SubscriptionResponse.class, false);

    public SubscriptionsView(SubscriptionService subscriptionService,
                              TopicService topicService,
                              SecurityService securityService) {
        this.subscriptionService = subscriptionService;
        this.topicService = topicService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);

        configureGrid();

        Button newBtn = new Button("New Subscription", e -> openDialog(null));
        newBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new HorizontalLayout(newBtn), grid);
        refresh();
    }

    private void configureGrid() {
        grid.addColumn(SubscriptionResponse::name).setHeader("Name").setFlexGrow(2);
        grid.addColumn(SubscriptionResponse::topicName).setHeader("Topic");
        grid.addColumn(SubscriptionResponse::entityType).setHeader("Entity Type");
        grid.addComponentColumn(s -> {
            Span badge = new Span(s.active() ? "Active" : "Inactive");
            badge.getElement().getThemeList().add(s.active() ? "badge success" : "badge contrast");
            return badge;
        }).setHeader("Status");
        grid.addComponentColumn(s -> {
            Button edit = new Button("Edit", e -> openDialog(s));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button delete = new Button("Delete", e -> {
                subscriptionService.delete(s.id());
                refresh();
                Notification.show("Subscription deleted", 2000, Notification.Position.BOTTOM_CENTER);
            });
            delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions");
        grid.setWidthFull();
    }

    private void openDialog(SubscriptionResponse existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Subscription" : "Edit Subscription");
        dialog.setWidth("600px");

        TextField nameField = new TextField("Name");
        nameField.setWidthFull();
        if (existing != null) nameField.setValue(existing.name() != null ? existing.name() : "");

        TextArea descField = new TextArea("Description");
        descField.setWidthFull();
        if (existing != null && existing.description() != null) descField.setValue(existing.description());

        Checkbox activeCheck = new Checkbox("Active", existing == null || existing.active());

        List<TopicResponse> topics = topicService.findAll();
        ComboBox<TopicResponse> topicBox = new ComboBox<>("Topic");
        topicBox.setItems(topics);
        topicBox.setItemLabelGenerator(t -> t.name() + " (" + t.entityType() + ")");
        topicBox.setWidthFull();
        if (existing != null) topics.stream().filter(t -> t.id().equals(existing.topicId())).findFirst()
                .ifPresent(topicBox::setValue);

        MultiSelectComboBox<SubscriptionEventType> eventTypesBox = new MultiSelectComboBox<>("Event Types");
        eventTypesBox.setItems(SubscriptionEventType.values());
        eventTypesBox.setHelperText("Empty = all event types");
        eventTypesBox.setWidthFull();
        if (existing != null && existing.eventTypes() != null) eventTypesBox.setValue(existing.eventTypes());

        // 5 filter slots
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
            filterNames[i] = new TextField("Filter " + i + " Field");
            filterValues[i] = new TextField("Filter " + i + " Value (comma-sep, ! = negate)");
            if (existingNames[i] != null) filterNames[i].setValue(existingNames[i]);
            if (existingValues[i] != null) filterValues[i].setValue(existingValues[i]);
            filterLayout.add(filterNames[i], filterValues[i]);
        }

        VerticalLayout body = new VerticalLayout(nameField, descField, activeCheck, topicBox, eventTypesBox, filterLayout);
        body.setPadding(false);
        dialog.add(body);

        Button save = new Button("Save", e -> {
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
                Notification.show("Subscription saved", 2000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", ev -> dialog.close()), save);
        dialog.open();
    }

    private void refresh() {
        grid.setItems(subscriptionService.findAllForUser(securityService.getUsername()));
    }
}
