package com.crm.ui;

import com.crm.repository.UserRepository;
import com.crm.timetracking.dto.AttendanceReportRequest;
import com.crm.timetracking.dto.AttendanceReportResponse;
import com.crm.timetracking.entity.Attendance;
import com.crm.timetracking.entity.Holiday;
import com.crm.timetracking.enums.AttendanceApprovalStatus;
import com.crm.timetracking.enums.AttendanceReportType;
import com.crm.timetracking.repository.HolidayRepository;
import com.crm.timetracking.service.AttendanceReportService;
import com.crm.timetracking.service.AttendanceService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Route(value = "time-clock", layout = MainLayout.class)
@PageTitle("שעון נוכחות | CRM")
@PermitAll
public class TimeClockView extends VerticalLayout {

    private static final ZoneId IL_ZONE   = ZoneId.of("Asia/Jerusalem");
    private static final DateTimeFormatter TIME_FMT      = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_DISPLAY  = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy",
                                                                   Locale.forLanguageTag("he"));

    private final AttendanceService       attendanceService;
    private final AttendanceReportService reportService;
    private final HolidayRepository       holidayRepository;
    private final Long                    currentUserId;

    // Status card
    private Div    statusDot;
    private Span   statusText;
    private Span   sinceLabel;
    private Button clockInBtn;
    private Button clockOutBtn;
    private TextField clockNoteField;

    // Today's sessions grid
    private Grid<Attendance>             sessionsGrid;
    private GridListDataView<Attendance> sessionsDataView;
    private Attendance                   pendingNewRow;
    private Span                         todayTotalSpan;
    private TimePicker                   editorStartPicker;
    private TimePicker                   editorEndPicker;

    // Leave reports
    private Grid<AttendanceReportResponse> reportsGrid;
    private VerticalLayout                 addReportPanel;
    private ComboBox<AttendanceReportType> reportTypeSelect;
    private com.vaadin.flow.component.datepicker.DatePicker reportDatePicker;
    private TimePicker                     reportEntryPicker;
    private TimePicker                     reportExitPicker;
    private HorizontalLayout               reportTimesRow;
    private TextField                      reportNoteField;

    // Holidays
    private VerticalLayout holidayList;

    public TimeClockView(AttendanceService attendanceService,
                         AttendanceReportService reportService,
                         UserRepository userRepository,
                         HolidayRepository holidayRepository,
                         SecurityService securityService) {
        this.attendanceService = attendanceService;
        this.reportService     = reportService;
        this.holidayRepository = holidayRepository;

        this.currentUserId = userRepository.findByUsername(securityService.getUsername())
                .map(u -> u.getId()).orElse(null);

        setSpacing(false);
        setPadding(false);
        getStyle().set("background", "#f4f6f9").set("min-height", "100vh");
        setWidth("100%");

        buildPageHeader();

        if (currentUserId == null) {
            Div err = card();
            err.add(new Span("Unable to resolve current user."));
            add(err);
            return;
        }

        buildStatusCard();
        buildTodaySessionsCard();
        buildLeaveReportsCard();
        buildHolidaysSection();
        refreshAll();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PAGE HEADER
    // ══════════════════════════════════════════════════════════════════════════

    private void buildPageHeader() {
        LocalDate today = LocalDate.now(IL_ZONE);
        Span title = new Span("שעון נוכחות");
        title.getStyle().set("font-size", "20px").set("font-weight", "700").set("color", "#1a1a2e");

        Span dateLabel = new Span(today.format(DATE_DISPLAY));
        dateLabel.getStyle().set("font-size", "13px").set("color", "#888").set("margin-top", "2px");

        Div header = new Div(title, dateLabel);
        header.getStyle()
                .set("display", "flex").set("flex-direction", "column")
                .set("padding", "20px 20px 12px").set("direction", "rtl");
        add(header);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATUS CARD
    // ══════════════════════════════════════════════════════════════════════════

    private void buildStatusCard() {
        Div card = card();
        card.getStyle().set("direction", "rtl");

        // Status row
        statusDot = new Div();
        statusDot.getStyle()
                .set("width", "10px").set("height", "10px").set("border-radius", "50%")
                .set("flex-shrink", "0");

        statusText = new Span();
        statusText.getStyle().set("font-size", "15px").set("font-weight", "600").set("color", "#333");

        sinceLabel = new Span();
        sinceLabel.getStyle().set("font-size", "12px").set("color", "#888").set("margin-top", "2px");

        Div statusInfo = new Div(statusDot, statusText);
        statusInfo.getStyle().set("display", "flex").set("align-items", "center").set("gap", "8px");

        Div statusBlock = new Div(statusInfo, sinceLabel);
        statusBlock.getStyle()
                .set("display", "flex").set("flex-direction", "column")
                .set("background", "#f8f9fa").set("border-radius", "10px")
                .set("padding", "12px 16px").set("margin-bottom", "16px");

        // Note field
        clockNoteField = new TextField();
        clockNoteField.setPlaceholder("הערה (משימה / פרויקט)…");
        clockNoteField.setWidthFull();
        clockNoteField.getStyle().set("margin-bottom", "12px");

        // Clock In / Out buttons
        clockInBtn = new Button("כניסה", VaadinIcon.SIGN_IN.create());
        clockInBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS,
                ButtonVariant.LUMO_LARGE);
        clockInBtn.addClickListener(e -> doPunchIn());

        clockOutBtn = new Button("יציאה", VaadinIcon.SIGN_OUT.create());
        clockOutBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR,
                ButtonVariant.LUMO_LARGE);
        clockOutBtn.addClickListener(e -> doPunchOut());

        HorizontalLayout btns = new HorizontalLayout(clockInBtn, clockOutBtn);
        btns.setSpacing(true);
        btns.setWidthFull();
        btns.setFlexGrow(1, clockInBtn);
        btns.setFlexGrow(1, clockOutBtn);

        card.add(statusBlock, clockNoteField, btns);
        add(card);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TODAY'S SESSIONS CARD
    // ══════════════════════════════════════════════════════════════════════════

    private void buildTodaySessionsCard() {
        Div card = card();
        card.getStyle().set("direction", "rtl");

        // Header
        Span title = sectionLabel("פעילויות היום");
        Button addEntry = new Button("+ כניסה חסרה", e -> addNewSessionRow());
        addEntry.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        HorizontalLayout cardHeader = new HorizontalLayout(title, addEntry);
        cardHeader.setWidthFull();
        cardHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        cardHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        cardHeader.getStyle().set("margin-bottom", "10px").set("direction", "rtl");

        // Grid — no Date column (all rows are today)
        sessionsGrid = new Grid<>(Attendance.class, false);
        sessionsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        sessionsGrid.getStyle().set("direction", "ltr");

        Binder<Attendance> binder = new Binder<>(Attendance.class);
        sessionsGrid.getEditor().setBinder(binder);
        sessionsGrid.getEditor().setBuffered(true);

        editorStartPicker = new TimePicker(); editorStartPicker.setWidthFull();
        editorEndPicker   = new TimePicker(); editorEndPicker.setWidthFull();
        TextField noteEd  = new TextField(); noteEd.setWidthFull();

        binder.forField(editorStartPicker).asRequired("שדה חובה")
                .bind(a -> a.getStartTime() != null
                                ? a.getStartTime().atZoneSameInstant(IL_ZONE).toLocalTime() : null,
                      (a, lt) -> {});
        binder.forField(editorEndPicker)
                .bind(a -> a.getEndTime() != null
                                ? a.getEndTime().atZoneSameInstant(IL_ZONE).toLocalTime() : null,
                      (a, lt) -> {});
        binder.forField(noteEd)
                .bind(a -> a.getNote() != null ? a.getNote() : "",
                      (a, v) -> a.setNote(v.isBlank() ? null : v));

        Button saveEd   = iconBtn(VaadinIcon.CHECK, ButtonVariant.LUMO_SUCCESS);
        Button cancelEd = iconBtn(VaadinIcon.CLOSE, ButtonVariant.LUMO_ERROR);
        saveEd.addClickListener(e -> sessionsGrid.getEditor().save());
        cancelEd.addClickListener(e -> {
            Attendance item = sessionsGrid.getEditor().getItem();
            sessionsGrid.getEditor().cancel();
            if (item != null && item.getId() == null && sessionsDataView != null) {
                sessionsDataView.removeItem(item);
                pendingNewRow = null;
            }
        });
        HorizontalLayout editorActions = new HorizontalLayout(saveEd, cancelEd);
        editorActions.setSpacing(false); editorActions.setPadding(false);

        sessionsGrid.addColumn(a -> a.getStartTime() != null
                ? a.getStartTime().atZoneSameInstant(IL_ZONE).format(TIME_FMT) : "—")
                .setHeader("כניסה").setWidth("80px").setFlexGrow(0)
                .setEditorComponent(editorStartPicker);

        sessionsGrid.addColumn(a -> a.getEndTime() != null
                ? a.getEndTime().atZoneSameInstant(IL_ZONE).format(TIME_FMT) : "—")
                .setHeader("יציאה").setWidth("80px").setFlexGrow(0)
                .setEditorComponent(editorEndPicker);

        sessionsGrid.addColumn(a -> {
            if (a.getDurationSeconds() == null) return "—";
            long h = a.getDurationSeconds() / 3600;
            long m = (a.getDurationSeconds() % 3600) / 60;
            return h + "h " + String.format("%02d", m) + "m";
        }).setHeader("משך").setWidth("80px").setFlexGrow(0);

        sessionsGrid.addColumn(a -> a.getNote() != null ? a.getNote() : "")
                .setHeader("הערה").setFlexGrow(1).setEditorComponent(noteEd);

        sessionsGrid.addComponentColumn(a -> statusBadge(a.getApprovalStatus()))
                .setHeader("סטטוס").setWidth("100px").setFlexGrow(0);

        sessionsGrid.addComponentColumn(a -> {
            Button edit = iconBtn(VaadinIcon.PENCIL, ButtonVariant.LUMO_TERTIARY);
            edit.addClickListener(e -> {
                if (sessionsGrid.getEditor().isOpen()) sessionsGrid.getEditor().cancel();
                sessionsGrid.getEditor().editItem(a);
            });
            return edit;
        }).setWidth("50px").setFlexGrow(0).setEditorComponent(editorActions);

        sessionsGrid.getEditor().addSaveListener(ev -> persistSessionEdit(ev.getItem()));
        sessionsGrid.setAllRowsVisible(true);
        sessionsGrid.setWidthFull();

        todayTotalSpan = new Span();
        todayTotalSpan.getStyle()
                .set("font-size", "13px").set("font-weight", "600").set("color", "#1565c0")
                .set("margin-top", "6px").set("display", "block").set("text-align", "right");

        card.add(cardHeader, sessionsGrid, todayTotalSpan);
        add(card);
    }

    private void addNewSessionRow() {
        if (sessionsGrid.getEditor().isOpen()) sessionsGrid.getEditor().cancel();
        if (sessionsDataView == null) return;
        pendingNewRow = new Attendance(currentUserId, OffsetDateTime.now(IL_ZONE), "EMPLOYEE_CORRECTION");
        sessionsDataView.addItem(pendingNewRow);
        sessionsGrid.getEditor().editItem(pendingNewRow);
        sessionsGrid.scrollToEnd();
    }

    private void persistSessionEdit(Attendance item) {
        LocalDate today = LocalDate.now(IL_ZONE);
        LocalTime start = editorStartPicker.getValue();
        LocalTime end   = editorEndPicker.getValue();

        if (start == null) {
            showError("שעת כניסה נדרשת.");
            return;
        }
        OffsetDateTime oStart = start.atDate(today).atZone(IL_ZONE).toOffsetDateTime();
        OffsetDateTime oEnd   = end != null ? end.atDate(today).atZone(IL_ZONE).toOffsetDateTime() : null;

        try {
            if (item.getId() == null) {
                if (oEnd == null) { showError("שעת יציאה נדרשת."); return; }
                attendanceService.createManualEntry(currentUserId, oStart, oEnd, item.getNote());
                pendingNewRow = null;
                refreshTodaySessions();
                Notification.show("הרשומה נשלחה לאישור מנהל.", 4000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                attendanceService.editSession(item.getId(), oStart, oEnd);
                refreshTodaySessions();
                Notification.show("הרשומה עודכנה.", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        } catch (Exception ex) {
            showError(ex.getMessage());
            refreshTodaySessions();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LEAVE / ABSENCE REPORTS CARD
    // ══════════════════════════════════════════════════════════════════════════

    private void buildLeaveReportsCard() {
        Div card = card();
        card.getStyle().set("direction", "rtl");

        Span title = sectionLabel("דיווחי חופשה / היעדרות");
        Button addBtn = new Button("+ הוסף דיווח",
                e -> addReportPanel.setVisible(!addReportPanel.isVisible()));
        addBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        HorizontalLayout hdr = new HorizontalLayout(title, addBtn);
        hdr.setWidthFull();
        hdr.setAlignItems(FlexComponent.Alignment.CENTER);
        hdr.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        hdr.getStyle().set("margin-bottom", "10px");

        // Add report panel
        addReportPanel = new VerticalLayout();
        addReportPanel.setPadding(true);
        addReportPanel.setSpacing(true);
        addReportPanel.setVisible(false);
        addReportPanel.getStyle()
                .set("background", "#f8f9fa").set("border-radius", "8px")
                .set("border", "1px solid #e0e0e0").set("margin-bottom", "12px")
                .set("direction", "rtl");

        reportTypeSelect = new ComboBox<>("סוג");
        reportTypeSelect.setItems(AttendanceReportType.values());
        reportTypeSelect.setItemLabelGenerator(TimeClockView::typeLabel);
        reportTypeSelect.setValue(AttendanceReportType.VACATION);
        reportTypeSelect.setWidth("200px");

        reportDatePicker = new com.vaadin.flow.component.datepicker.DatePicker("תאריך");
        reportDatePicker.setValue(LocalDate.now());
        reportDatePicker.setWidth("170px");

        reportEntryPicker = new TimePicker("כניסה"); reportEntryPicker.setWidth("130px");
        reportExitPicker  = new TimePicker("יציאה"); reportExitPicker.setWidth("130px");
        reportTimesRow = new HorizontalLayout(reportEntryPicker, reportExitPicker);
        reportTimesRow.setSpacing(true);
        reportTimesRow.setVisible(false);
        reportTypeSelect.addValueChangeListener(e ->
                reportTimesRow.setVisible(e.getValue() == AttendanceReportType.PRESENCE));

        reportNoteField = new TextField("הערה");
        reportNoteField.setWidth("260px");
        reportNoteField.setPlaceholder("סיבה (אופציונלי)…");

        Button submitBtn = new Button("שלח", VaadinIcon.CHECK.create(), e -> doSubmitReport());
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        Button cancelBtn = new Button("ביטול", e -> addReportPanel.setVisible(false));
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        HorizontalLayout formRow = new HorizontalLayout(
                reportTypeSelect, reportDatePicker, reportTimesRow, reportNoteField);
        formRow.setAlignItems(FlexComponent.Alignment.BASELINE);
        formRow.getStyle().set("flex-wrap", "wrap");
        formRow.setSpacing(true);

        addReportPanel.add(formRow, new HorizontalLayout(submitBtn, cancelBtn));

        // Reports grid
        reportsGrid = new Grid<>(AttendanceReportResponse.class, false);
        reportsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        reportsGrid.getStyle().set("direction", "ltr");

        reportsGrid.addColumn(r -> r.reportDate().format(DateTimeFormatter.ofPattern("dd MMM")))
                .setHeader("תאריך").setWidth("80px").setFlexGrow(0);
        reportsGrid.addComponentColumn(r -> typeBadge(r.reportType()))
                .setHeader("סוג").setWidth("140px").setFlexGrow(0);
        reportsGrid.addColumn(r -> r.durationFormatted() != null ? r.durationFormatted() : "יום מלא")
                .setHeader("משך").setWidth("80px").setFlexGrow(0);
        reportsGrid.addColumn(r -> r.note() != null ? r.note() : "")
                .setHeader("הערה").setFlexGrow(1);
        reportsGrid.addComponentColumn(r -> {
            Button del = iconBtn(VaadinIcon.TRASH, ButtonVariant.LUMO_ERROR);
            del.addClickListener(e -> {
                ConfirmDialog cd = new ConfirmDialog("מחיקת דיווח",
                        "למחוק את דיווח " + typeLabel(r.reportType()) + " ל-"
                                + r.reportDate().format(DateTimeFormatter.ofPattern("dd/MM")) + "?",
                        "מחק", ev -> {
                            reportService.deleteReport(r.id());
                            refreshLeaveReports();
                            Notification.show("הדיווח נמחק.", 2000, Notification.Position.BOTTOM_CENTER);
                        },
                        "ביטול", ev -> {});
                cd.setConfirmButtonTheme("error primary");
                cd.open();
            });
            return del;
        }).setWidth("50px").setFlexGrow(0);

        reportsGrid.setAllRowsVisible(true);
        reportsGrid.setWidthFull();

        card.add(hdr, addReportPanel, reportsGrid);
        add(card);
    }

    private void doSubmitReport() {
        AttendanceReportType type = reportTypeSelect.getValue();
        LocalDate date = reportDatePicker.getValue();
        if (type == null) { showError("סוג הדיווח נדרש."); return; }
        if (date == null) { showError("תאריך נדרש."); return; }

        LocalTime entry = null, exit = null;
        if (type == AttendanceReportType.PRESENCE) {
            entry = reportEntryPicker.getValue();
            exit  = reportExitPicker.getValue();
            if (entry == null || exit == null) { showError("שעות כניסה ויציאה נדרשות."); return; }
        }
        String note = reportNoteField.getValue().isBlank() ? null : reportNoteField.getValue();
        try {
            reportService.createReport(currentUserId,
                    new AttendanceReportRequest(date, entry, exit, note, type, false));
            addReportPanel.setVisible(false);
            reportNoteField.clear(); reportEntryPicker.clear(); reportExitPicker.clear();
            refreshLeaveReports();
            Notification.show(typeLabel(type) + " נוסף.", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HOLIDAYS SECTION
    // ══════════════════════════════════════════════════════════════════════════

    private void buildHolidaysSection() {
        Div card = card();
        card.getStyle().set("direction", "rtl");
        card.add(sectionLabel("חגים קרובים (90 יום הקרובים)"));
        holidayList = new VerticalLayout();
        holidayList.setSpacing(false);
        holidayList.setPadding(false);
        card.add(holidayList);
        add(card);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REFRESH
    // ══════════════════════════════════════════════════════════════════════════

    private void refreshAll() {
        refreshStatus();
        refreshTodaySessions();
        refreshLeaveReports();
        refreshHolidays();
    }

    private void refreshStatus() {
        attendanceService.findActiveSession(currentUserId).ifPresentOrElse(s -> {
            statusDot.getStyle().set("background", "#43a047");
            statusText.setText("מחוברת — פעילות");
            String since = s.getStartTime().atZoneSameInstant(IL_ZONE).format(TIME_FMT);
            sinceLabel.setText("שעת כניסה: " + since);
            clockInBtn.setEnabled(false);
            clockOutBtn.setEnabled(true);
        }, () -> {
            statusDot.getStyle().set("background", "#bdbdbd");
            statusText.setText("לא מחובר/ת");
            sinceLabel.setText("");
            clockInBtn.setEnabled(true);
            clockOutBtn.setEnabled(false);
        });
    }

    private void refreshTodaySessions() {
        if (sessionsGrid.getEditor().isOpen()) sessionsGrid.getEditor().cancel();
        pendingNewRow = null;

        LocalDate today = LocalDate.now(IL_ZONE);
        List<Attendance> todayRecords = attendanceService
                .getMonthlyRecords(currentUserId, today.getYear(), today.getMonthValue())
                .stream()
                .filter(a -> a.getStartTime().atZoneSameInstant(IL_ZONE)
                              .toLocalDate().equals(today))
                .collect(java.util.stream.Collectors.toList());

        sessionsDataView = sessionsGrid.setItems(new ArrayList<>(todayRecords));

        long totalSec = todayRecords.stream()
                .filter(a -> a.getDurationSeconds() != null)
                .filter(a -> a.getApprovalStatus() == null
                          || a.getApprovalStatus() == AttendanceApprovalStatus.APPROVED)
                .mapToLong(Attendance::getDurationSeconds).sum();
        if (totalSec > 0) {
            long h = totalSec / 3600, m = (totalSec % 3600) / 60;
            todayTotalSpan.setText(String.format("סה\"כ היום: %dh %02dm", h, m));
        } else {
            todayTotalSpan.setText("");
        }
    }

    private void refreshLeaveReports() {
        YearMonth now = YearMonth.now();
        List<AttendanceReportResponse> reps = reportService.getReportsForMonth(
                currentUserId, now.getYear(), now.getMonthValue());
        reportsGrid.setItems(reps);
    }

    private void refreshHolidays() {
        holidayList.removeAll();
        LocalDate today = LocalDate.now();
        List<Holiday> holidays = holidayRepository.findByDateBetween(today, today.plusDays(90))
                .stream().limit(8).toList();
        if (holidays.isEmpty()) {
            Span none = new Span("אין חגים בקרוב.");
            none.getStyle().set("font-size", "13px").set("color", "#999");
            holidayList.add(none);
            return;
        }
        for (Holiday h : holidays) {
            Span d = new Span(h.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            d.getStyle().set("min-width", "90px").set("color", "#888").set("font-size", "13px");
            Span n = new Span(h.getName());
            n.getStyle().set("flex", "1").set("font-weight", "500");
            Span c = new Span(h.getCreditHours().stripTrailingZeros().toPlainString() + "h");
            c.getStyle().set("color", "#2e7d32").set("font-size", "13px").set("font-weight", "700");
            Div row = new Div(d, n, c);
            row.getStyle().set("display", "flex").set("gap", "12px").set("align-items", "center")
                    .set("padding", "6px 4px").set("border-bottom", "1px solid #f0f0f0");
            holidayList.add(row);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CLOCK IN / OUT
    // ══════════════════════════════════════════════════════════════════════════

    private void doPunchIn() {
        try {
            String note = clockNoteField.getValue().isBlank() ? null : clockNoteField.getValue();
            attendanceService.punchIn(currentUserId, note, "MANUAL");
            clockNoteField.clear();
            refreshAll();
            Notification.show("כניסה נרשמה בהצלחה", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) { showError(e.getMessage()); }
    }

    private void doPunchOut() {
        try {
            attendanceService.punchOut(currentUserId);
            refreshAll();
            Notification.show("יציאה נרשמה בהצלחה", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) { showError(e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VISUAL HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private static Div card() {
        Div card = new Div();
        card.getStyle()
                .set("background", "#ffffff").set("border-radius", "12px")
                .set("padding", "18px 20px").set("box-shadow", "0 1px 4px rgba(0,0,0,.07)")
                .set("margin", "0 12px 14px").set("width", "calc(100% - 24px)");
        return card;
    }

    private static Span sectionLabel(String text) {
        Span s = new Span(text);
        s.getStyle().set("font-size", "12px").set("font-weight", "700")
                .set("text-transform", "uppercase").set("letter-spacing", "0.5px")
                .set("color", "#666");
        return s;
    }

    private static Button iconBtn(VaadinIcon icon, ButtonVariant... extra) {
        Button b = new Button(icon.create());
        b.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        b.addThemeVariants(extra);
        return b;
    }

    private static Span statusBadge(AttendanceApprovalStatus status) {
        if (status == null) return new Span();
        String label = switch (status) {
            case PENDING  -> "ממתין";
            case APPROVED -> "אושר";
            case REJECTED -> "נדחה";
        };
        String bg    = switch (status) {
            case PENDING  -> "#fff8e1"; case APPROVED -> "#e8f5e9"; case REJECTED -> "#ffebee";
        };
        String color = switch (status) {
            case PENDING  -> "#f57f17"; case APPROVED -> "#2e7d32"; case REJECTED -> "#c62828";
        };
        Span s = new Span(label);
        s.getStyle().set("background", bg).set("color", color)
                .set("padding", "2px 8px").set("border-radius", "10px")
                .set("font-size", "11px").set("font-weight", "600").set("white-space", "nowrap");
        return s;
    }

    static String typeLabel(AttendanceReportType t) {
        return switch (t) {
            case PRESENCE     -> "נוכחות";
            case VACATION     -> "חופשה";
            case SICK         -> "מחלה";
            case RESERVE_DUTY -> "מילואים";
            case HOLIDAY      -> "חג";
            case ABSENCE      -> "היעדרות";
        };
    }

    private static Span typeBadge(AttendanceReportType t) {
        String[] style = switch (t) {
            case PRESENCE     -> new String[]{"#e8f5e9", "#2e7d32"};
            case VACATION     -> new String[]{"#e3f2fd", "#1565c0"};
            case SICK         -> new String[]{"#fff3e0", "#e65100"};
            case RESERVE_DUTY -> new String[]{"#ede7f6", "#6a1b9a"};
            case HOLIDAY      -> new String[]{"#e0f7fa", "#00838f"};
            case ABSENCE      -> new String[]{"#ffebee", "#c62828"};
        };
        Span s = new Span(typeLabel(t));
        s.getStyle().set("background", style[0]).set("color", style[1])
                .set("padding", "2px 10px").set("border-radius", "10px")
                .set("font-size", "12px").set("font-weight", "600").set("white-space", "nowrap");
        return s;
    }

    private void showError(String msg) {
        Notification.show(msg, 4000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
