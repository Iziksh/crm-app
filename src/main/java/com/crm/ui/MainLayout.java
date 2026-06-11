package com.crm.ui;



import com.crm.dto.response.AlertResponse;

import com.crm.dto.response.UserSummaryResponse;

import com.crm.service.AlertService;

import com.crm.service.LocaleService;

import com.crm.service.NotificationService;

import com.crm.service.TranslationService;

import com.crm.service.UserService;

import com.crm.ui.attendance.AttendanceCalendarView;

import com.vaadin.flow.component.AttachEvent;

import com.vaadin.flow.component.applayout.AppLayout;

import com.vaadin.flow.component.applayout.DrawerToggle;

import com.vaadin.flow.component.button.Button;

import com.vaadin.flow.component.button.ButtonVariant;

import com.vaadin.flow.component.combobox.ComboBox;

import com.vaadin.flow.component.dependency.CssImport;

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



@CssImport("./i18n.css")

public class MainLayout extends AppLayout {



    private final SecurityService securityService;

    private final AlertService alertService;

    private final NotificationService notificationService;

    private final UserService userService;

    private final LocaleService localeService;

    private final TranslationService i18n;

    private final Span bellBadge = new Span();

    private Button bellBtn;



    public MainLayout(SecurityService securityService, AlertService alertService,

                      NotificationService notificationService, UserService userService,

                      LocaleService localeService, TranslationService i18n) {

        this.securityService = securityService;

        this.alertService = alertService;

        this.notificationService = notificationService;

        this.userService = userService;

        this.localeService = localeService;

        this.i18n = i18n;

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

        H1 title = new H1(i18n.translate("app.name"));

        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);



        String username = securityService.getUsername();

        Span userSpan = new Span(i18n.translate("header.loggedInAs", username));

        userSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);



        bellBadge.getElement().getThemeList().add("badge error small");

        bellBadge.getStyle().set("position", "absolute").set("top", "-4px").set("inset-inline-end", "-4px");

        bellBadge.setVisible(false);



        Div bellWrapper = new Div();

        bellWrapper.getStyle().set("position", "relative").set("display", "inline-block");

        bellBtn = new Button(VaadinIcon.BELL.create(), e -> openNotificationsDialog());

        bellBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        bellWrapper.add(bellBtn, bellBadge);



        LanguageSwitcher languageSwitcher = new LanguageSwitcher(localeService, i18n, false);



        Button logout = new Button(i18n.translate("header.logout"), VaadinIcon.SIGN_OUT.create(),

                e -> securityService.logout());

        logout.getStyle().set("margin-inline-start", "auto");



        HorizontalLayout header = new HorizontalLayout(

                new DrawerToggle(), title, userSpan, languageSwitcher, bellWrapper, logout);

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        header.expand(title);

        header.setWidthFull();

        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);



        addToNavbar(header);

    }



    private void openNotificationsDialog() {

        Dialog dialog = new Dialog();

        dialog.setHeaderTitle(i18n.translate("dialog.alerts"));

        dialog.setWidth("520px");



        List<AlertResponse> alerts = alertService.getForUser(securityService.getUsername(), null);



        Button sendBtn = new Button(i18n.translate("dialog.send"), VaadinIcon.PLUS.create(), e -> {

            dialog.close();

            openSendNotificationDialog();

        });

        sendBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);



        Button markAllBtn = new Button(i18n.translate("dialog.markAllRead"), e -> {

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

            list.add(new Span(i18n.translate("dialog.noAlerts")));

        } else {

            for (AlertResponse a : alerts) {

                HorizontalLayout row = new HorizontalLayout();

                row.setWidthFull();

                row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

                row.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)").set("padding", "6px 0");



                // State badge

                Span stateBadge = new Span(i18n.translateEnum(a.alertState()));

                stateBadge.getElement().getThemeList().add(

                        switch (a.alertState()) {

                            case NEW -> "badge error small";

                            case READ -> "badge contrast small";

                            case ACCEPTED -> "badge success small";

                            default -> "badge small";

                        });

                stateBadge.getStyle().set("margin-inline-end", "6px");



                Div content = new Div();

                Span name = new Span(a.name());

                name.getStyle().set("font-weight", "500");

                String dateStr = a.createdAt() != null ? a.createdAt().toLocalDate().toString() : "";

                Span detail = new Span(a.entityType() != null

                        ? i18n.translate("dialog.alertDetail", a.entityType(), dateStr)

                        : dateStr);

                detail.getStyle().set("font-size", "0.8em").set("color", "var(--lumo-secondary-text-color)");

                content.add(name, new Div(detail));

                content.getStyle().set("flex", "1");



                // 3-state action buttons (OpenCRX: markAsRead / markAsAccepted / markAsNew)

                Button readBtn = new Button(i18n.translate("dialog.read"), e -> {

                    alertService.markAsRead(a.id());

                    row.setVisible(false);

                    refreshBellCount();

                });

                readBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

                readBtn.setEnabled(a.alertState() != com.crm.domain.enums.AlertState.READ);



                Button acceptBtn = new Button(i18n.translate("dialog.accept"), e -> {

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

        dialog.getFooter().add(new Button(i18n.translate("dialog.close"), e -> dialog.close()));

        dialog.open();

    }



    private void openSendNotificationDialog() {

        Dialog dialog = new Dialog();

        dialog.setHeaderTitle(i18n.translate("dialog.sendNotification"));

        dialog.setWidth("420px");



        List<UserSummaryResponse> users = userService.findAll();



        ComboBox<UserSummaryResponse> userPicker = new ComboBox<>(i18n.translate("dialog.to"));

        userPicker.setItems(users);

        userPicker.setItemLabelGenerator(u -> u.username() + (u.email() != null ? " — " + u.email() : ""));

        userPicker.setWidthFull();



        TextArea message = new TextArea(i18n.translate("dialog.message"));

        message.setWidthFull();

        message.setMinHeight("80px");



        VerticalLayout body = new VerticalLayout(userPicker, message);

        body.setPadding(false);

        dialog.add(body);



        Button send = new Button(i18n.translate("dialog.send"), e -> {

            if (userPicker.getValue() == null) {

                userPicker.setInvalid(true);

                userPicker.setErrorMessage(i18n.translate("dialog.selectRecipient"));

                return;

            }

            if (message.getValue().isBlank()) {

                message.setInvalid(true);

                message.setErrorMessage(i18n.translate("dialog.messageRequired"));

                return;

            }

            notificationService.notify(userPicker.getValue().id(), message.getValue(), null, null);

            dialog.close();

            Notification.show(i18n.translate("notification.sent"), 2000, Notification.Position.BOTTOM_CENTER)

                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        });

        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(new Button(i18n.translate("dialog.cancel"), e -> dialog.close()), send);

        dialog.open();

    }



    private void createDrawer() {

        SideNav nav = new SideNav();



        nav.addItem(new SideNavItem(i18n.translate("nav.dashboard"), DashboardView.class, VaadinIcon.DASHBOARD.create()));



        SideNavItem contacts = new SideNavItem(i18n.translate("nav.contacts"));

        contacts.setPrefixComponent(VaadinIcon.USERS.create());

        contacts.addItem(new SideNavItem(i18n.translate("nav.accounts"), AccountsView.class, VaadinIcon.BUILDING.create()));

        contacts.addItem(new SideNavItem(i18n.translate("nav.accountGroups"), AccountGroupsView.class, VaadinIcon.FOLDER.create()));

        contacts.addItem(new SideNavItem(i18n.translate("nav.contactsItem"), ContactsView.class, VaadinIcon.USER.create()));

        contacts.addItem(new SideNavItem(i18n.translate("nav.addresses"), AddressesView.class, VaadinIcon.MAP_MARKER.create()));

        nav.addItem(contacts);



        SideNavItem support = new SideNavItem(i18n.translate("nav.support"));

        support.setPrefixComponent(VaadinIcon.LIFEBUOY.create());

        support.addItem(new SideNavItem(i18n.translate("nav.activities"), ActivitiesView.class, VaadinIcon.BUG.create()));

        support.addItem(new SideNavItem(i18n.translate("nav.calendar"), CalendarView.class, VaadinIcon.CALENDAR.create()));

        nav.addItem(support);



        SideNavItem sales = new SideNavItem(i18n.translate("nav.sales"));

        sales.setPrefixComponent(VaadinIcon.TRENDING_UP.create());

        sales.addItem(new SideNavItem(i18n.translate("nav.leads"), LeadsView.class, VaadinIcon.CONNECT.create()));

        sales.addItem(new SideNavItem(i18n.translate("nav.opportunities"), OpportunitiesView.class, VaadinIcon.DOLLAR.create()));

        sales.addItem(new SideNavItem(i18n.translate("nav.quotes"), QuotesView.class, VaadinIcon.INVOICE.create()));

        sales.addItem(new SideNavItem(i18n.translate("nav.salesOrders"), SalesOrdersView.class, VaadinIcon.PACKAGE.create()));

        sales.addItem(new SideNavItem(i18n.translate("nav.contracts"), ContractsView.class, VaadinIcon.FILE_TEXT.create()));

        sales.addItem(new SideNavItem(i18n.translate("nav.forecast"), ForecastView.class, VaadinIcon.CHART.create()));

        sales.addItem(new SideNavItem(i18n.translate("nav.products"), ProductsView.class, VaadinIcon.CART.create()));

        nav.addItem(sales);



        SideNavItem settings = new SideNavItem(i18n.translate("nav.settings"));

        settings.setPrefixComponent(VaadinIcon.COG.create());

        settings.addItem(new SideNavItem(i18n.translate("nav.workspaces"), WorkspacesView.class, VaadinIcon.GROUP.create()));

        settings.addItem(new SideNavItem(i18n.translate("nav.savedSearches"), SavedSearchesView.class, VaadinIcon.SEARCH.create()));

        settings.addItem(new SideNavItem(i18n.translate("nav.subscriptions"), SubscriptionsView.class, VaadinIcon.BELL.create()));

        if (securityService.hasRole("ADMIN")) {

            settings.addItem(new SideNavItem(i18n.translate("nav.users"), UsersView.class, VaadinIcon.USERS.create()));

            settings.addItem(new SideNavItem(i18n.translate("nav.taskQueue"), ScheduledTasksView.class, VaadinIcon.TIMER.create()));

        }

        nav.addItem(settings);



        SideNavItem hr = new SideNavItem(i18n.translate("nav.hr"));

        hr.setPrefixComponent(VaadinIcon.CLOCK.create());

        hr.addItem(new SideNavItem(i18n.translate("nav.timeClock"), TimeClockView.class, VaadinIcon.CLOCK.create()));

        hr.addItem(new SideNavItem(i18n.translate("nav.attendanceCalendar"), AttendanceCalendarView.class, VaadinIcon.CALENDAR.create()));

        if (securityService.hasRole("ADMIN")) {

            hr.addItem(new SideNavItem(i18n.translate("nav.corrections"), AttendanceCorrectionView.class, VaadinIcon.CHECK_CIRCLE.create()));

        }

        nav.addItem(hr);



        addToDrawer(nav);

    }

}


