package com.crm.ui;



import com.crm.config.performance.StartupPerformanceProfiler;
import com.crm.dto.response.AlertResponse;

import com.crm.dto.response.UserSummaryResponse;

import com.crm.service.AlertService;

import com.crm.service.WorkspaceContext;

import com.crm.service.LocaleService;

import com.crm.service.NotificationService;

import com.crm.service.TranslationService;

import com.crm.service.UserService;

import com.vaadin.flow.router.ParentLayout;
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



@ParentLayout(WorkspaceRouteLayout.class)
@CssImport("./i18n.css")
public class MainLayout extends AppLayout {



    private final SecurityService securityService;

    private final AlertService alertService;

    private final NotificationService notificationService;

    private final UserService userService;

    private final WorkspaceContext workspaceContext;

    private final LocaleService localeService;

    private final TranslationService i18n;

    private final Span bellBadge = new Span();

    private Button bellBtn;



    public MainLayout(SecurityService securityService, AlertService alertService,

                      NotificationService notificationService, UserService userService,

                      WorkspaceContext workspaceContext,

                      LocaleService localeService, TranslationService i18n) {

        this.securityService = securityService;

        this.alertService = alertService;

        this.notificationService = notificationService;

        this.userService = userService;

        this.workspaceContext = workspaceContext;

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

        StartupPerformanceProfiler.time("phase.main-layout.attach", this::refreshBellCount);

    }



    private void refreshBellCount() {

        long count = StartupPerformanceProfiler.time("dashboard.db.alertCountUnread",
                () -> alertService.countUnread(securityService.getUsername()));

        getUI().ifPresent(ui -> ui.access(() -> {

            bellBadge.setText(count > 0 ? String.valueOf(count) : "");

            bellBadge.setVisible(count > 0);

        }));

    }



    private void createHeader() {

        // ── Brand: gradient pill + app name + company ─────────────────────────
        Div logoBox = new Div();
        logoBox.setText("CRM");
        logoBox.getStyle()
                .set("background", "linear-gradient(135deg,#1565c0,#42a5f5)")
                .set("color", "white")
                .set("font-size", "11px")
                .set("font-weight", "800")
                .set("letter-spacing", "1.5px")
                .set("padding", "5px 9px")
                .set("border-radius", "8px")
                .set("flex-shrink", "0");

        Span appName = new Span(i18n.translate("app.name"));
        appName.getStyle()
                .set("font-size", "16px")
                .set("font-weight", "700")
                .set("color", "var(--lumo-body-text-color)")
                .set("line-height", "1.1");

        VerticalLayout brandText = new VerticalLayout(appName);
        brandText.setPadding(false);
        brandText.setSpacing(false);

        String companyName = workspaceContext.currentUserPrimaryWorkspace()
                .map(ws -> ws.getName())
                .orElse(null);
        if (companyName != null) {
            Span co = new Span(companyName.toUpperCase());
            co.getStyle()
                    .set("font-size", "9px")
                    .set("font-weight", "600")
                    .set("color", "#90a4ae")
                    .set("letter-spacing", "1.8px");
            brandText.add(co);
        }

        HorizontalLayout brand = new HorizontalLayout(logoBox, brandText);
        brand.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        brand.setSpacing(false);
        brand.getStyle().set("gap", "10px");

        // ── Spacer pushes right-side controls to the edge ─────────────────────
        Div spacer = new Div();

        // ── User avatar chip ──────────────────────────────────────────────────
        String username = securityService.getUsername();

        Div avatarCircle = new Div();
        avatarCircle.setText(username.substring(0, 1).toUpperCase());
        avatarCircle.getStyle()
                .set("width", "28px").set("height", "28px")
                .set("border-radius", "50%")
                .set("background", "linear-gradient(135deg,#1565c0,#42a5f5)")
                .set("color", "white")
                .set("font-size", "12px")
                .set("font-weight", "700")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("flex-shrink", "0");

        Span usernameSpan = new Span(username);
        usernameSpan.getStyle()
                .set("font-size", "13px")
                .set("font-weight", "500")
                .set("color", "var(--lumo-body-text-color)");

        HorizontalLayout userChip = new HorizontalLayout(avatarCircle, usernameSpan);
        userChip.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        userChip.setSpacing(false);
        userChip.getStyle()
                .set("gap", "7px")
                .set("padding", "4px 12px 4px 5px")
                .set("border-radius", "20px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("cursor", "pointer");

        com.vaadin.flow.component.html.Anchor profileLink =
                new com.vaadin.flow.component.html.Anchor(
                        workspaceContext.currentWorkspaceSlug() + "/my-profile");
        profileLink.add(userChip);
        profileLink.getStyle().set("text-decoration", "none").set("color", "inherit");

        // ── Notification bell ─────────────────────────────────────────────────
        bellBadge.getElement().getThemeList().add("badge error small");
        bellBadge.getStyle().set("position", "absolute").set("top", "-4px").set("inset-inline-end", "-4px");
        bellBadge.setVisible(false);

        Div bellWrapper = new Div();
        bellWrapper.getStyle().set("position", "relative").set("display", "inline-block");
        bellBtn = new Button(VaadinIcon.BELL.create(), e -> openNotificationsDialog());
        bellBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        bellWrapper.add(bellBtn, bellBadge);

        // ── Language switcher ─────────────────────────────────────────────────
        LanguageSwitcher languageSwitcher = new LanguageSwitcher(localeService, i18n, false);

        // ── Separator ─────────────────────────────────────────────────────────
        Div sep = new Div();
        sep.getStyle()
                .set("width", "1px").set("height", "22px")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("align-self", "center");

        // ── Logout (icon-only) ────────────────────────────────────────────────
        Button logout = new Button(VaadinIcon.SIGN_OUT.create(), e -> securityService.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        logout.setTooltipText(i18n.translate("header.logout"));
        logout.getStyle().set("color", "var(--lumo-error-color)");

        // ── Assemble ──────────────────────────────────────────────────────────
        HorizontalLayout header = new HorizontalLayout(
                new DrawerToggle(), brand, spacer,
                profileLink, languageSwitcher, bellWrapper, sep, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(spacer);
        header.setWidthFull();
        header.setSpacing(false);
        header.getStyle()
                .set("padding", "0 12px 0 4px")
                .set("gap", "6px")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,.08)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

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

        String slug = workspaceContext.currentWorkspaceSlug();

        SideNav nav = new SideNav();



        nav.addItem(new SideNavItem(i18n.translate("nav.dashboard"), slug + "/dashboard", VaadinIcon.DASHBOARD.create()));



        SideNavItem contacts = new SideNavItem(i18n.translate("nav.contacts"));

        contacts.setPrefixComponent(VaadinIcon.USERS.create());

        contacts.addItem(new SideNavItem(i18n.translate("nav.accounts"), slug + "/accounts", VaadinIcon.BUILDING.create()));

        contacts.addItem(new SideNavItem(i18n.translate("nav.accountGroups"), slug + "/account-groups", VaadinIcon.FOLDER.create()));

        contacts.addItem(new SideNavItem(i18n.translate("nav.contactsItem"), slug + "/contacts", VaadinIcon.USER.create()));

        contacts.addItem(new SideNavItem(i18n.translate("nav.addresses"), slug + "/addresses", VaadinIcon.MAP_MARKER.create()));

        nav.addItem(contacts);



        SideNavItem support = new SideNavItem(i18n.translate("nav.support"));

        support.setPrefixComponent(VaadinIcon.LIFEBUOY.create());

        support.addItem(new SideNavItem(i18n.translate("nav.activities"), slug + "/activities", VaadinIcon.BUG.create()));

        support.addItem(new SideNavItem(i18n.translate("nav.calendar"), slug + "/calendar", VaadinIcon.CALENDAR.create()));

        nav.addItem(support);



        SideNavItem sales = new SideNavItem(i18n.translate("nav.sales"));

        sales.setPrefixComponent(VaadinIcon.TRENDING_UP.create());

        sales.addItem(new SideNavItem(i18n.translate("nav.leads"), slug + "/leads", VaadinIcon.CONNECT.create()));

        sales.addItem(new SideNavItem(i18n.translate("nav.opportunities"), slug + "/opportunities", VaadinIcon.DOLLAR.create()));

        sales.addItem(new SideNavItem(i18n.translate("nav.quotes"), slug + "/quotes", VaadinIcon.INVOICE.create()));

        sales.addItem(new SideNavItem(i18n.translate("nav.salesOrders"), slug + "/sales-orders", VaadinIcon.PACKAGE.create()));

        sales.addItem(new SideNavItem(i18n.translate("nav.contracts"), slug + "/contracts", VaadinIcon.FILE_TEXT.create()));

        sales.addItem(new SideNavItem(i18n.translate("nav.forecast"), slug + "/forecast", VaadinIcon.CHART.create()));

        sales.addItem(new SideNavItem(i18n.translate("nav.products"), slug + "/products", VaadinIcon.CART.create()));

        nav.addItem(sales);



        SideNavItem settings = new SideNavItem(i18n.translate("nav.settings"));

        settings.setPrefixComponent(VaadinIcon.COG.create());

        settings.addItem(new SideNavItem(i18n.translate("nav.myProfile"), slug + "/my-profile", VaadinIcon.USER_CARD.create()));

        settings.addItem(new SideNavItem(i18n.translate("nav.workspaces"), slug + "/workspaces", VaadinIcon.GROUP.create()));

        settings.addItem(new SideNavItem(i18n.translate("nav.savedSearches"), slug + "/saved-searches", VaadinIcon.SEARCH.create()));

        settings.addItem(new SideNavItem(i18n.translate("nav.subscriptions"), slug + "/subscriptions", VaadinIcon.BELL.create()));

        if (securityService.hasRole("ADMIN") || securityService.hasRole("COMPANY_ADMIN") || securityService.hasRole("SUPER_ADMIN")) {

            settings.addItem(new SideNavItem(i18n.translate("nav.users"), slug + "/users", VaadinIcon.USERS.create()));

        }

        if (securityService.hasRole("ADMIN")) {

            settings.addItem(new SideNavItem(i18n.translate("nav.taskQueue"), slug + "/scheduled-tasks", VaadinIcon.TIMER.create()));

        }

        nav.addItem(settings);



        SideNavItem hr = new SideNavItem(i18n.translate("nav.hr"));

        hr.setPrefixComponent(VaadinIcon.CLOCK.create());

        hr.addItem(new SideNavItem(i18n.translate("nav.timeClock"), slug + "/time-clock", VaadinIcon.CLOCK.create()));

        hr.addItem(new SideNavItem(i18n.translate("nav.attendanceCalendar"), slug + "/attendance-calendar", VaadinIcon.CALENDAR.create()));

        if (securityService.hasRole("ADMIN")) {

            hr.addItem(new SideNavItem(i18n.translate("nav.corrections"), slug + "/attendance-corrections", VaadinIcon.CHECK_CIRCLE.create()));

        }

        nav.addItem(hr);



        addToDrawer(nav);

    }

}


