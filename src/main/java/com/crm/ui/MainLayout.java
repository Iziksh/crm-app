package com.crm.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class MainLayout extends AppLayout {

    private final SecurityService securityService;

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 title = new H1("CRM");
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        String username = securityService.getUsername();
        Span userSpan = new Span("Logged in as: " + username);
        userSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        Button logout = new Button("Logout", VaadinIcon.SIGN_OUT.create(), e -> securityService.logout());
        logout.addClassNames(LumoUtility.Margin.Left.AUTO);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, userSpan, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
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
