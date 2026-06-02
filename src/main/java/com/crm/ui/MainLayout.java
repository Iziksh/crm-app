package com.crm.ui;

import com.crm.dto.response.NotificationResponse;
import com.crm.service.NotificationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

public class MainLayout extends AppLayout {

    private final SecurityService securityService;
    private final NotificationService notificationService;
    private final Span bellBadge = new Span();
    private Button bellBtn;

    public MainLayout(SecurityService securityService, NotificationService notificationService) {
        this.securityService = securityService;
        this.notificationService = notificationService;
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
        long count = notificationService.countUnread(securityService.getUsername());
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
        dialog.setHeaderTitle("Notifications");
        dialog.setWidth("480px");

        List<NotificationResponse> notifications = notificationService.getUnread(securityService.getUsername());

        Button markAllBtn = new Button("Mark all read", e -> {
            notificationService.markAllRead(securityService.getUsername());
            refreshBellCount();
            dialog.close();
        });
        markAllBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        markAllBtn.setEnabled(!notifications.isEmpty());

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);

        if (notifications.isEmpty()) {
            list.add(new Span("No unread notifications."));
        } else {
            for (NotificationResponse n : notifications) {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
                row.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)").set("padding", "6px 0");

                Div content = new Div();
                Span msg = new Span(n.message());
                Span time = new Span(n.createdAt() != null ? n.createdAt().toLocalDate().toString() : "");
                time.getStyle().set("font-size", "0.8em").set("color", "var(--lumo-secondary-text-color)");
                content.add(msg, new Div(time));
                content.getStyle().set("flex", "1");

                Button readBtn = new Button("✓", e -> {
                    notificationService.markRead(n.id());
                    row.setVisible(false);
                    refreshBellCount();
                });
                readBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);

                row.add(content, readBtn);
                list.add(row);
            }
        }

        VerticalLayout body = new VerticalLayout(markAllBtn, list);
        body.setPadding(false);
        dialog.add(body);
        dialog.getFooter().add(new Button("Close", e -> dialog.close()));
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
        if (securityService.hasRole("ADMIN")) {
            settings.addItem(new SideNavItem("Users", UsersView.class, VaadinIcon.USERS.create()));
        }
        nav.addItem(settings);

        addToDrawer(nav);
    }
}
