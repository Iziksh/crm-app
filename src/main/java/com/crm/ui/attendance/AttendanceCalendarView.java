package com.crm.ui.attendance;

import com.crm.dto.response.UserSummaryResponse;
import com.crm.repository.UserRepository;
import com.crm.service.UserService;
import com.crm.timetracking.dto.AttendanceReportResponse;
import com.crm.timetracking.dto.DayCalendarEntry;
import com.crm.timetracking.dto.MonthlyCalendarResponse;
import com.crm.timetracking.enums.AttendanceReportType;
import com.crm.timetracking.service.AttendanceReportService;
import com.crm.timetracking.util.DurationCalculator;
import com.crm.ui.MainLayout;
import com.crm.ui.SecurityService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;

@CssImport("./attendance-calendar.css")
@Route(value = "attendance-calendar", layout = MainLayout.class)
@PageTitle("עדכון נוכחות | CRM")
@PermitAll
public class AttendanceCalendarView extends VerticalLayout {

    private final AttendanceReportService reportService;
    private final UserService             userService;
    private final SecurityService         securityService;
    private final UserRepository          userRepository;

    private YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Jerusalem"));
    private Long      selectedUserId;

    private final Button prevBtn    = new Button(VaadinIcon.ANGLE_LEFT.create());
    private final Button nextBtn    = new Button(VaadinIcon.ANGLE_RIGHT.create());
    private final Span   monthLabel = new Span();
    private final ComboBox<UserSummaryResponse> userCombo = new ComboBox<>("עובד");

    private final Span profileName = new Span();
    private final Span profileId   = new Span();
    private final Div  calGrid     = new Div();
    private final Span totalLine   = new Span();

    public AttendanceCalendarView(AttendanceReportService reportService,
                                  UserService userService,
                                  SecurityService securityService,
                                  UserRepository userRepository) {
        this.reportService   = reportService;
        this.userService     = userService;
        this.securityService = securityService;
        this.userRepository  = userRepository;

        getElement().setAttribute("dir", "rtl");
        setPadding(false);
        setSpacing(false);
        addClassName("attendance-calendar-page");
        setWidth("100%");

        buildProfileSection();
        buildMonthNav();
        calGrid.addClassName("cal-grid");
        add(calGrid);
        totalLine.addClassName("total-line");
        add(totalLine);

        initUserContext();
    }

    // ── Profile header ────────────────────────────────────────────────────────

    private void buildProfileSection() {
        Icon userIcon = VaadinIcon.USER.create();
        userIcon.getStyle().set("width", "28px").set("height", "28px").set("color", "#aaa");
        Div avatar = new Div(userIcon);
        avatar.addClassName("profile-avatar");

        profileName.addClassName("profile-name");
        profileId.addClassName("profile-id");

        Div info = new Div(profileName, profileId);
        info.addClassName("profile-info");

        Div section = new Div(info, avatar);
        section.addClassName("profile-section");
        add(section);
    }

    // ── Month navigation ──────────────────────────────────────────────────────

    private void buildMonthNav() {
        prevBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        nextBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        prevBtn.addClassName("nav-arrow-btn");
        nextBtn.addClassName("nav-arrow-btn");
        prevBtn.addClickListener(e -> navigate(-1));
        nextBtn.addClickListener(e -> navigate(+1));

        monthLabel.addClassName("month-label");

        Button menuBtn = new Button(VaadinIcon.ELLIPSIS_DOTS_V.create());
        menuBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        menuBtn.addClassName("menu-btn");

        userCombo.setItemLabelGenerator(UserSummaryResponse::username);
        userCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                selectedUserId = e.getValue().id();
                refresh();
            }
        });
        userCombo.setWidth("100%");
        userCombo.addClassName("admin-user-picker");

        // RTL layout: DOM order [nextBtn, monthLabel, prevBtn, menuBtn]
        // renders visually left-to-right as: [menu] [prev] [month] [next]
        HorizontalLayout nav = new HorizontalLayout(nextBtn, monthLabel, prevBtn, menuBtn);
        nav.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        nav.addClassName("month-nav-bar");
        nav.setWidthFull();

        add(nav, userCombo);
    }

    // ── User context ──────────────────────────────────────────────────────────

    private void initUserContext() {
        if (securityService.hasRole("ADMIN")) {
            userCombo.setItems(userService.findAll());
        } else {
            userCombo.setVisible(false);
            selectedUserId = userRepository
                    .findByUsername(securityService.getUsername())
                    .map(u -> u.getId())
                    .orElse(null);
            refresh();
        }
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    void refresh() {
        if (selectedUserId == null) return;
        Locale he = Locale.forLanguageTag("he");
        monthLabel.setText(
                currentMonth.getMonth().getDisplayName(TextStyle.FULL, he)
                        + " " + currentMonth.getYear());

        MonthlyCalendarResponse cal = reportService.getMonthlyCalendar(
                selectedUserId, currentMonth.getYear(), currentMonth.getMonthValue());

        profileName.setText(cal.username());
        profileId.setText("# " + cal.userId());

        buildCalendarGrid(cal);

        totalLine.setText(String.format(
                "סה\"כ חודש: %s / תקן: %s  (דלטא: %s)",
                DurationCalculator.formatMinutes(cal.totalWorkedMinutes()),
                DurationCalculator.formatMinutes(cal.totalStandardMinutes()),
                DurationCalculator.formatMinutes(Math.abs(cal.totalDeltaMinutes()))));
    }

    private void navigate(int delta) {
        currentMonth = currentMonth.plusMonths(delta);
        refresh();
    }

    // ── Calendar grid ─────────────────────────────────────────────────────────

    private void buildCalendarGrid(MonthlyCalendarResponse cal) {
        calGrid.removeAll();

        // Single-letter Hebrew day headers — RTL: Sun on right, Sat on left
        String[] headers   = {"א", "ב", "ג", "ד", "ה", "ו", "ש"};
        boolean[] isSabbat = {false, false, false, false, false, false, true};
        for (int i = 0; i < headers.length; i++) {
            Div h = new Div(new Span(headers[i]));
            h.addClassName("cal-header-cell");
            if (isSabbat[i]) h.addClassName("cal-header-shabbat");
            calGrid.add(h);
        }

        // Blank leading cells before the 1st
        LocalDate today  = LocalDate.now(ZoneId.of("Asia/Jerusalem"));
        LocalDate first  = YearMonth.of(cal.year(), cal.month()).atDay(1);
        int       offset = israeliDayIndex(first.getDayOfWeek());
        for (int i = 0; i < offset; i++) {
            Div empty = new Div();
            empty.addClassName("cal-empty-cell");
            calGrid.add(empty);
        }

        for (DayCalendarEntry entry : cal.days()) {
            calGrid.add(buildDayCell(entry, today));
        }
    }

    private static int israeliDayIndex(DayOfWeek dow) {
        return switch (dow) {
            case SUNDAY    -> 0;
            case MONDAY    -> 1;
            case TUESDAY   -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY  -> 4;
            case FRIDAY    -> 5;
            case SATURDAY  -> 6;
        };
    }

    // ── Single day cell ───────────────────────────────────────────────────────

    private Div buildDayCell(DayCalendarEntry entry, LocalDate today) {
        Div cell = new Div();
        cell.addClassName("cal-day-cell");

        boolean isToday  = entry.date().equals(today);
        boolean isSabbat = entry.date().getDayOfWeek() == DayOfWeek.SATURDAY;
        boolean isAbsent = entry.reports().stream()
                .anyMatch(r -> r.reportType() == AttendanceReportType.ABSENCE);

        if (isToday)           cell.addClassName("cal-today");
        if (isSabbat)          cell.addClassName("cal-shabbat");
        if (entry.isHoliday()) cell.addClassName("cal-holiday");

        if (isAbsent) {
            // Red X in the time slot, date number also red
            Span x = new Span("✕");
            x.addClassName("absence-mark");
            cell.add(x);
            // empty spacer to keep exit-time row consistent
            Span spacer = new Span(" ");
            spacer.addClassName("exit-time");
            cell.add(spacer);
        } else {
            // Find the PRESENCE report for entry/exit times
            AttendanceReportResponse presence = entry.reports().stream()
                    .filter(r -> r.reportType() == AttendanceReportType.PRESENCE)
                    .findFirst()
                    .orElse(null);

            LocalTime entryTime = presence != null ? presence.entryTime() : null;
            LocalTime exitTime  = presence != null ? presence.exitTime()  : null;

            Span entryLbl = new Span(entryTime != null
                    ? String.format("%02d:%02d", entryTime.getHour(), entryTime.getMinute())
                    : " ");
            entryLbl.addClassName("entry-time");
            cell.add(entryLbl);

            Span exitLbl = new Span(exitTime != null
                    ? String.format("%02d:%02d", exitTime.getHour(), exitTime.getMinute())
                    : " ");
            exitLbl.addClassName("exit-time");
            cell.add(exitLbl);
        }

        // Date number
        Span dayNum = new Span(String.valueOf(entry.date().getDayOfMonth()));
        dayNum.addClassName("day-number");
        if (isAbsent) dayNum.addClassName("absent-num");
        cell.add(dayNum);

        // Daily total + delta — shown on workdays with clocked time
        if (entry.totalWorkedMinutes() > 0 && !isSabbat && !isAbsent) {
            Span total = new Span(DurationCalculator.formatMinutes(entry.totalWorkedMinutes()));
            total.addClassName("day-total");
            if (entry.deltaMinutes() < 0) total.addClassName("day-total-deficit");
            cell.add(total);

            int delta = entry.deltaMinutes();
            String deltaText = (delta >= 0 ? "+" : "-")
                    + DurationCalculator.formatMinutes(Math.abs(delta));
            Span deltaSpan = new Span(deltaText);
            deltaSpan.addClassName("day-delta");
            deltaSpan.addClassName(delta >= 0 ? "day-delta-pos" : "day-delta-neg");
            cell.add(deltaSpan);
        }

        // Holiday name
        if (entry.isHoliday() && entry.holidayName() != null) {
            Span hn = new Span(entry.holidayName());
            hn.addClassName("holiday-name");
            cell.add(hn);
        }

        // Whole cell opens the report editor on click
        cell.addClickListener(e -> {
            AttendanceReportResponse existing =
                    entry.reports().isEmpty() ? null : entry.reports().get(0);
            openEditor(entry.date(), existing);
        });

        return cell;
    }

    private void openEditor(LocalDate date, AttendanceReportResponse existing) {
        new AttendanceReportEditor(reportService, this, selectedUserId, date, existing).open();
    }
}
