package com.crm.ui;

import com.crm.dto.response.AlertResponse;
import com.crm.dto.response.UserSummaryResponse;
import com.crm.service.AlertService;
import com.crm.service.NotificationService;
import com.crm.service.UserService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

public class MainLayout extends AppLayout {

    private final SecurityService securityService;
    private final AlertService alertService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final Span bellBadge = new Span();
    private Button bellBtn;

    public MainLayout(SecurityService securityService, AlertService alertService,
                      NotificationService notificationService, UserService userService) {
        this.securityService = securityService;
        this.alertService = alertService;
        this.notificationService = notificationService;
        this.userService = userService;
        createHeader();
        createDrawer();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getUI().ifPresent(ui -> {
            ui.setPollInterval(60000);
            ui.addPollListener(e -> refreshBellCount());
        });
        refreshBellCount();
    }

    private void refreshBellCount() {
        long count = alertService.countUnread(securityService.getUsername());
        getUI().ifPresent(ui -> ui.access(() -> {
            bellBadge.setText(count > 0 ? String.valueOf(count) : "");
            bellBadge.setVisible(count > 0);
        }));
    }

    private void createHeader() {
        H1 title = new H1("CRM");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        String username = securityService.getUsername();
        Span userSpan = new Span("Logged in as: " + username);
        userSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        bellBadge.getElement().getThemeList().add("badge error small");
        bellBadge.getStyle().set("position", "absolute").set("top", "-4px").set("right", "-4px");
        bellBadge.setVisible(false);

        Div bellWrapper = new Div();
        bellWrapper.getStyle().set("position", "relative").set("display", "inline-block");
        bellBtn = new Button(VaadinIcon.BELL.create(), e -> openNotificationsDialog());
        bellBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        bellWrapper.add(bellBtn, bellBadge);

        Button logout = new Button("Logout", VaadinIcon.SIGN_OUT.create(), e -> securityService.logout());
        logout.addClassNames(LumoUtility.Margin.Left.AUTO);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, userSpan, bellWrapper, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }

    private void openNotificationsDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Alerts");
        dialog.setWidth("520px");

        List<AlertResponse> alerts = alertService.getForUser(securityService.getUsername(), null);

        Button sendBtn = new Button("Send", VaadinIcon.PLUS.create(), e -> {
            dialog.close();
            openSendNotificationDialog();
        });
        sendBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        Button markAllBtn = new Button("Mark all read", e -> {
            alertService.markAllRead(securityService.getUsername());
            refreshBellCount();
            dialog.close();
        });
        markAllBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        markAllBtn.setEnabled(!alerts.isEmpty());

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);

        if (alerts.isEmpty()) {
            list.add(new Span("No alerts."));
        } else {
            for (AlertResponse a : alerts) {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
                row.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)").set("padding", "6px 0");

                // State badge
                Span stateBadge = new Span(a.alertState().name());
                stateBadge.getElement().getThemeList().add(
                        switch (a.alertState()) {
                            case NEW -> "badge error small";
                            case READ -> "badge contrast small";
                            case ACCEPTED -> "badge success small";
                            default -> "badge small";
                        });
                stateBadge.getStyle().set("margin-right", "6px");

                Div content = new Div();
                Span name = new Span(a.name());
                name.getStyle().set("font-weight", "500");
                Span detail = new Span((a.entityType() != null ? a.entityType() + " — " : "")
                        + (a.createdAt() != null ? a.createdAt().toLocalDate().toString() : ""));
                detail.getStyle().set("font-size", "0.8em").set("color", "var(--lumo-secondary-text-color)");
                content.add(name, new Div(detail));
                content.getStyle().set("flex", "1");

                // 3-state action buttons (OpenCRX: markAsRead / markAsAccepted / markAsNew)
                Button readBtn = new Button("Read", e -> {
                    alertService.markAsRead(a.id());
                    row.setVisible(false);
                    refreshBellCount();
                });
                readBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                readBtn.setEnabled(a.alertState() != com.crm.domain.enums.AlertState.READ);

                Button acceptBtn = new Button("Accept", e -> {
                    alertService.markAsAccepted(a.id());
                    row.setVisible(false);
                    refreshBellCount();
                });
                acceptBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
                acceptBtn.setEnabled(a.alertState() != com.crm.domain.enums.AlertState.ACCEPTED);

                row.add(stateBadge, content, readBtn, acceptBtn);
                list.add(row);
            }
        }

        HorizontalLayout actions = new HorizontalLayout(sendBtn, markAllBtn);
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout body = new VerticalLayout(actions, list);
        body.setPadding(false);
        dialog.add(body);
        dialog.getFooter().add(new Button("Close", e -> dialog.close()));
        dialog.open();
    }

    private void openSendNotificationDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Send Notification");
        dialog.setWidth("420px");

        List<UserSummaryResponse> users = userService.findAll();

        ComboBox<UserSummaryResponse> userPicker = new ComboBox<>("To");
        userPicker.setItems(users);
        userPicker.setItemLabelGenerator(u -> u.username() + (u.email() != null ? " — " + u.email() : ""));
        userPicker.setWidthFull();

        TextArea message = new TextArea("Message");
        message.setWidthFull();
        message.setMinHeight("80px");

        VerticalLayout body = new VerticalLayout(userPicker, message);
        body.setPadding(false);
        dialog.add(body);

        Button send = new Button("Send", e -> {
            if (userPicker.getValue() == null) {
                userPicker.setInvalid(true);
                userPicker.setErrorMessage("Please select a recipient");
                return;
            }
            if (message.getValue().isBlank()) {
                message.setInvalid(true);
                message.setErrorMessage("Message is required");
                return;
            }
            notificationService.notify(userPicker.getValue().id(), message.getValue(), null, null);
            dialog.close();
            Notification.show("Notification sent", 2000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), send);
        dialog.open();
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));

        SideNavItem contacts = new SideNavItem("Contacts");
        contacts.setPrefixComponent(VaadinIcon.USERS.create());
        contacts.addItem(new SideNavItem("Accounts", AccountsView.class, VaadinIcon.BUILDING.create()));
        contacts.addItem(new SideNavItem("Account Groups", AccountGroupsView.class, VaadinIcon.FOLDER.create()));
        contacts.addItem(new SideNavItem("Contacts", ContactsView.class, VaadinIcon.USER.create()));
        contacts.addItem(new SideNavItem("Addresses", AddressesView.class, VaadinIcon.MAP_MARKER.create()));
        nav.addItem(contacts);

        SideNavItem support = new SideNavItem("Support");
        support.setPrefixComponent(VaadinIcon.LIFEBUOY.create());
        support.addItem(new SideNavItem("Activities", ActivitiesView.class, VaadinIcon.BUG.create()));
        support.addItem(new SideNavItem("Calendar", CalendarView.class, VaadinIcon.CALENDAR.create()));
        nav.addItem(support);

        SideNavItem sales = new SideNavItem("Sales");
        sales.setPrefixComponent(VaadinIcon.TRENDING_UP.create());
        sales.addItem(new SideNavItem("Leads", LeadsView.class, VaadinIcon.CONNECT.create()));
        sales.addItem(new SideNavItem("Opportunities", OpportunitiesView.class, VaadinIcon.DOLLAR.create()));
        sales.addItem(new SideNavItem("Quotes", QuotesView.class, VaadinIcon.INVOICE.create()));
        sales.addItem(new SideNavItem("Sales Orders", SalesOrdersView.class, VaadinIcon.PACKAGE.create()));
        sales.addItem(new SideNavItem("Contracts", ContractsView.class, VaadinIcon.FILE_TEXT.create()));
        sales.addItem(new SideNavItem("Forecast", ForecastView.class, VaadinIcon.CHART.create()));
        sales.addItem(new SideNavItem("Products", ProductsView.class, VaadinIcon.CART.create()));
        nav.addItem(sales);

        SideNavItem settings = new SideNavItem("Settings");
        settings.setPrefixComponent(VaadinIcon.COG.create());
        settings.addItem(new SideNavItem("Workspaces", WorkspacesView.class, VaadinIcon.GROUP.create()));
        settings.addItem(new SideNavItem("Saved Searches", SavedSearchesView.class, VaadinIcon.SEARCH.create()));
        settings.addItem(new SideNavItem("Subscriptions", SubscriptionsView.class, VaadinIcon.BELL.create()));
        if (securityService.hasRole("ADMIN")) {
            settings.addItem(new SideNavItem("Users", UsersView.class, VaadinIcon.USERS.create()));
            settings.addItem(new SideNavItem("Task Queue", ScheduledTasksView.class, VaadinIcon.TIMER.create()));
        }
        nav.addItem(settings);

        addToDrawer(nav);
    }
}
