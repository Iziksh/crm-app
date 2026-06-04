package com.crm.ui;

import com.crm.repository.UserRepository;
import com.crm.timetracking.entity.Attendance;
import com.crm.timetracking.entity.Holiday;
import com.crm.timetracking.repository.HolidayRepository;
import com.crm.timetracking.service.AttendanceService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "time-clock", layout = MainLayout.class)
@PageTitle("Time Clock | CRM")
@PermitAll
public class TimeClockView extends VerticalLayout {

    private static final ZoneId IL_ZONE = ZoneId.of("Asia/Jerusalem");
    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final AttendanceService attendanceService;
    private final HolidayRepository holidayRepository;
    private final Long              currentUserId;

    private YearMonth currentMonth = YearMonth.now();

    private Div    statusBadge;
    private Span   statusText;
    private TextField noteField;
    private Button clockInBtn;
    private Button clockOutBtn;

    private H3               monthLabel;
    private Grid<Attendance> recordGrid;
    private Span             totalSpan;
    private VerticalLayout   holidayList;

    public TimeClockView(AttendanceService attendanceService,
                         UserRepository userRepository,
                         HolidayRepository holidayRepository,
                         SecurityService securityService) {
        this.attendanceService = attendanceService;
        this.holidayRepository = holidayRepository;

        String username = securityService.getUsername();
        this.currentUserId = userRepository.findByUsername(username)
                .map(u -> u.getId()).orElse(null);

        setSpacing(true);
        setPadding(true);
        add(new H2("Time Clock"));

        if (currentUserId == null) {
            add(new Span("Unable to resolve current user. Please log out and log in again."));
            return;
        }

        buildStatusCard();
        buildMonthlySection();
        buildHolidaysSection();
        refreshAll();
    }

    // ── Clock In / Out card ───────────────────────────────────────────────────

    private void buildStatusCard() {
        Div card = new Div();
        card.getStyle()
                .set("background", "#fff")
                .set("border-radius", "10px")
                .set("padding", "24px 28px")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,.10)")
                .set("max-width", "520px");

        statusBadge = new Div();
        statusBadge.getStyle()
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("padding", "6px 16px")
                .set("border-radius", "20px")
                .set("font-weight", "700")
                .set("font-size", "14px")
                .set("margin-bottom", "18px");
        statusText = new Span();
        statusBadge.add(statusText);

        noteField = new TextField("Note (optional)");
        noteField.setWidthFull();
        noteField.setPlaceholder("Task or project description…");
        noteField.getStyle().set("margin-bottom", "16px");

        clockInBtn = new Button("Clock In", e -> doPunchIn());
        clockInBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        clockInBtn.getStyle().set("min-width", "130px");

        clockOutBtn = new Button("Clock Out", e -> doPunchOut());
        clockOutBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        clockOutBtn.getStyle().set("min-width", "130px");

        HorizontalLayout btnRow = new HorizontalLayout(clockInBtn, clockOutBtn);
        btnRow.setSpacing(true);

        card.add(statusBadge, noteField, btnRow);
        add(card);
    }

    // ── Monthly records ───────────────────────────────────────────────────────

    private void buildMonthlySection() {
        monthLabel = new H3();
        monthLabel.getStyle().set("margin", "0");

        Button prevBtn = new Button("‹ Prev", e -> { currentMonth = currentMonth.minusMonths(1); refreshMonthly(); });
        Button nextBtn = new Button("Next ›", e -> { currentMonth = currentMonth.plusMonths(1); refreshMonthly(); });
        prevBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout navRow = new HorizontalLayout(prevBtn, monthLabel, nextBtn);
        navRow.setAlignItems(Alignment.CENTER);
        navRow.getStyle().set("margin-top", "24px");

        recordGrid = new Grid<>(Attendance.class, false);
        recordGrid.addColumn(a -> a.getStartTime().atZoneSameInstant(IL_ZONE).format(DATE_FMT))
                .setHeader("Date").setWidth("110px").setFlexGrow(0);
        recordGrid.addColumn(a -> a.getStartTime().atZoneSameInstant(IL_ZONE).format(TIME_FMT))
                .setHeader("Clock In").setWidth("100px").setFlexGrow(0);
        recordGrid.addColumn(a -> a.getEndTime() != null
                        ? a.getEndTime().atZoneSameInstant(IL_ZONE).format(TIME_FMT) : "OPEN")
                .setHeader("Clock Out").setWidth("100px").setFlexGrow(0);
        recordGrid.addColumn(a -> {
            if (a.getDurationSeconds() == null) return "—";
            return String.format("%d:%02d", a.getDurationSeconds() / 3600, (a.getDurationSeconds() % 3600) / 60);
        }).setHeader("Duration").setWidth("90px").setFlexGrow(0);
        recordGrid.addColumn(a -> a.getNote() != null ? a.getNote() : "")
                .setHeader("Note").setFlexGrow(1);
        recordGrid.setHeight("280px");
        recordGrid.setWidthFull();

        totalSpan = new Span();
        totalSpan.getStyle().set("font-weight", "700").set("font-size", "14px").set("color", "#1565c0");

        add(navRow, recordGrid, totalSpan);
    }

    // ── Upcoming holidays ─────────────────────────────────────────────────────

    private void buildHolidaysSection() {
        H3 title = new H3("Upcoming Holidays (next 90 days)");
        title.getStyle().set("margin-top", "28px");
        holidayList = new VerticalLayout();
        holidayList.setSpacing(false);
        holidayList.setPadding(false);
        add(title, holidayList);
    }

    // ── Refresh helpers ───────────────────────────────────────────────────────

    private void refreshAll() {
        refreshStatus();
        refreshMonthly();
        refreshHolidays();
    }

    private void refreshStatus() {
        attendanceService.findActiveSession(currentUserId).ifPresentOrElse(session -> {
            String since = session.getStartTime().atZoneSameInstant(IL_ZONE).format(TIME_FMT);
            statusText.setText("● Clocked in since " + since);
            statusBadge.getStyle().set("background", "#e8f5e9").set("color", "#2e7d32");
            clockInBtn.setEnabled(false);
            clockOutBtn.setEnabled(true);
        }, () -> {
            statusText.setText("● Not clocked in");
            statusBadge.getStyle().set("background", "#f5f5f5").set("color", "#757575");
            clockInBtn.setEnabled(true);
            clockOutBtn.setEnabled(false);
        });
    }

    private void refreshMonthly() {
        monthLabel.setText(currentMonth.format(MONTH_FMT));
        List<Attendance> records = attendanceService.getMonthlyRecords(
                currentUserId, currentMonth.getYear(), currentMonth.getMonthValue());
        recordGrid.setItems(records);

        long totalSec = records.stream()
                .filter(a -> a.getDurationSeconds() != null)
                .mapToLong(Attendance::getDurationSeconds).sum();
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        totalSpan.setText(String.format("Total: %dh %dm across %d sessions", h, m, records.size()));
    }

    private void refreshHolidays() {
        holidayList.removeAll();
        LocalDate today = LocalDate.now();
        List<Holiday> holidays = holidayRepository.findByDateBetween(today, today.plusDays(90))
                .stream().limit(8).toList();

        if (holidays.isEmpty()) {
            holidayList.add(new Span("No upcoming holidays in the next 90 days."));
            return;
        }
        for (Holiday h : holidays) {
            String creditStr = h.getCreditHours().stripTrailingZeros().toPlainString() + "h";

            Span dateSpan = new Span(h.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            dateSpan.getStyle().set("min-width", "110px").set("color", "#888").set("font-size", "13px");

            Span nameSpan = new Span(h.getName());
            nameSpan.getStyle().set("flex", "1").set("font-weight", "500");

            Span creditSpan = new Span(creditStr);
            creditSpan.getStyle().set("color", "#2e7d32").set("font-size", "13px").set("font-weight", "700").set("min-width", "36px").set("text-align", "right");

            Div row = new Div(dateSpan, nameSpan, creditSpan);
            row.getStyle()
                    .set("display", "flex").set("gap", "12px").set("align-items", "center")
                    .set("padding", "7px 4px").set("border-bottom", "1px solid #f0f0f0");
            holidayList.add(row);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void doPunchIn() {
        try {
            String note = noteField.getValue().isBlank() ? null : noteField.getValue();
            attendanceService.punchIn(currentUserId, note, "MANUAL");
            noteField.clear();
            refreshAll();
            Notification n = Notification.show("Clocked in successfully", 3000, Notification.Position.BOTTOM_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification n = Notification.show(e.getMessage(), 4000, Notification.Position.BOTTOM_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void doPunchOut() {
        try {
            attendanceService.punchOut(currentUserId);
            refreshAll();
            Notification n = Notification.show("Clocked out successfully", 3000, Notification.Position.BOTTOM_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification n = Notification.show(e.getMessage(), 4000, Notification.Position.BOTTOM_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
