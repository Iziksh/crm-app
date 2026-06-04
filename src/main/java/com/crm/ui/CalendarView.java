package com.crm.ui;

import com.crm.domain.enums.ActivityPriority;
import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ActivityType;
import com.crm.dto.request.ActivityRequest;
import com.crm.dto.response.ActivityResponse;
import com.crm.service.ActivityService;
import com.crm.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "calendar", layout = MainLayout.class)
@PageTitle("Calendar | CRM")
@PermitAll
public class CalendarView extends VerticalLayout {

    private final ActivityService activityService;
    private final UserService userService;
    private final SecurityService securityService;

    private YearMonth currentMonth = YearMonth.now();
    private final H3 monthLabel = new H3();
    private final Div calendarGrid = new Div();

    public CalendarView(ActivityService activityService,
                        UserService userService,
                        SecurityService securityService) {
        this.activityService = activityService;
        this.userService = userService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        Button prevBtn = new Button(VaadinIcon.ANGLE_LEFT.create(), e -> {
            currentMonth = currentMonth.minusMonths(1);
            refresh();
        });
        prevBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button nextBtn = new Button(VaadinIcon.ANGLE_RIGHT.create(), e -> {
            currentMonth = currentMonth.plusMonths(1);
            refresh();
        });
        nextBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button newBtn = new Button("New Activity", VaadinIcon.PLUS.create(),
                e -> openActivityDialog(LocalDate.now()));
        newBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        StreamResource icalResource = new StreamResource("crm-calendar.ics", () ->
                new ByteArrayInputStream(activityService.buildIcal().getBytes(StandardCharsets.UTF_8)));
        Anchor icalAnchor = new Anchor(icalResource, "");
        icalAnchor.getElement().setAttribute("download", true);
        Button icalBtn = new Button("Download .ics", VaadinIcon.CALENDAR.create());
        icalBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        icalAnchor.add(icalBtn);

        monthLabel.getStyle().set("margin", "0 16px");

        HorizontalLayout nav = new HorizontalLayout(prevBtn, monthLabel, nextBtn, icalAnchor, newBtn);
        nav.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        nav.setWidthFull();
        nav.getStyle().set("margin-bottom", "12px");

        calendarGrid.setWidthFull();
        calendarGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(7, 1fr)")
                .set("gap", "1px")
                .set("background-color", "#cfd8dc")
                .set("border", "1px solid #cfd8dc")
                .set("border-radius", "6px")
                .set("overflow", "hidden");

        add(nav, calendarGrid);
        setFlexGrow(1, calendarGrid);
        refresh();
    }

    private void refresh() {
        monthLabel.setText(
                currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                        + " " + currentMonth.getYear());

        LocalDate first = currentMonth.atDay(1);
        LocalDate last = currentMonth.atEndOfMonth();

        List<ActivityResponse> activities = activityService.findForCalendar(first, last);
        Map<LocalDate, List<ActivityResponse>> byDate = activities.stream()
                .filter(a -> a.dueDate() != null)
                .collect(Collectors.groupingBy(ActivityResponse::dueDate));

        calendarGrid.removeAll();

        // Day-of-week headers
        for (String day : List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) {
            Div hdr = new Div(new Span(day));
            hdr.getStyle()
                    .set("background", "#1565c0")
                    .set("color", "#fff")
                    .set("text-align", "center")
                    .set("padding", "8px 4px")
                    .set("font-weight", "600")
                    .set("font-size", "13px");
            calendarGrid.add(hdr);
        }

        // Leading empty cells (Mon=1, so offset = dayOfWeek - 1)
        int offset = first.getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) {
            calendarGrid.add(emptyCell());
        }

        LocalDate today = LocalDate.now();
        for (int d = 1; d <= last.getDayOfMonth(); d++) {
            LocalDate date = currentMonth.atDay(d);
            calendarGrid.add(buildDayCell(date, byDate.getOrDefault(date, List.of()), today));
        }

        // Trailing empty cells
        int filled = offset + last.getDayOfMonth();
        int remainder = filled % 7;
        if (remainder != 0) {
            for (int i = 0; i < 7 - remainder; i++) {
                calendarGrid.add(emptyCell());
            }
        }
    }

    private Div emptyCell() {
        Div cell = new Div();
        cell.getStyle().set("background", "#f5f5f5").set("min-height", "90px");
        return cell;
    }

    private Div buildDayCell(LocalDate date, List<ActivityResponse> activities, LocalDate today) {
        Div cell = new Div();
        boolean isToday = date.equals(today);
        cell.getStyle()
                .set("background", isToday ? "#e8f0fe" : "#fff")
                .set("min-height", "90px")
                .set("padding", "4px 6px")
                .set("cursor", "pointer");

        Span dayNum = new Span(String.valueOf(date.getDayOfMonth()));
        dayNum.getStyle()
                .set("font-size", "12px")
                .set("font-weight", isToday ? "700" : "400")
                .set("color", isToday ? "#1565c0" : "#555")
                .set("display", "block")
                .set("margin-bottom", "3px");
        cell.add(dayNum);

        for (ActivityResponse a : activities) {
            String label = a.title().length() > 20 ? a.title().substring(0, 18) + "…" : a.title();
            Span chip = new Span(label);
            chip.getStyle()
                    .set("display", "block")
                    .set("font-size", "11px")
                    .set("padding", "2px 5px")
                    .set("border-radius", "3px")
                    .set("margin-bottom", "2px")
                    .set("color", "#fff")
                    .set("background", chipColor(a.type()))
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis")
                    .set("white-space", "nowrap")
                    .set("cursor", "pointer");
            chip.setTitle(a.title() + (a.assignedToName() != null ? " — " + a.assignedToName() : ""));
            chip.addClickListener(e -> {
                e.getSource().getElement().getComponent().ifPresent(c -> {}); // stop propagation workaround
                openDetailDialog(a);
            });
            cell.add(chip);
        }

        cell.addClickListener(e -> openActivityDialog(date));
        return cell;
    }

    private String chipColor(ActivityType type) {
        if (type == null) return "#78909c";
        return switch (type) {
            case MEETING    -> "#1565c0";
            case TASK       -> "#e65100";
            case SALES_VISIT -> "#2e7d32";
            case CALL       -> "#6a1b9a";
            case EMAIL      -> "#00838f";
            default         -> "#78909c";
        };
    }

    private void openDetailDialog(ActivityResponse a) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle(a.type() + " — " + a.title());
        dlg.setWidth("420px");

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        if (a.description() != null && !a.description().isBlank()) {
            Paragraph desc = new Paragraph(a.description());
            desc.getStyle().set("color", "#555").set("white-space", "pre-wrap");
            body.add(desc);
        }
        body.add(detailRow("Status",    a.status() != null ? a.status().name() : "—"));
        body.add(detailRow("Priority",  a.priority() != null ? a.priority().name() : "—"));
        body.add(detailRow("Due date",  a.dueDate() != null ? a.dueDate().toString() : "—"));
        body.add(detailRow("Assigned",  a.assignedToName() != null ? a.assignedToName() : "—"));
        if (a.accountName() != null) body.add(detailRow("Account", a.accountName()));
        if (a.contactName() != null) body.add(detailRow("Contact", a.contactName()));

        dlg.add(body);
        dlg.getFooter().add(new Button("Close", e -> dlg.close()));
        dlg.open();
    }

    private HorizontalLayout detailRow(String label, String value) {
        Span key = new Span(label + ":");
        key.getStyle().set("font-weight", "600").set("min-width", "90px").set("color", "#555");
        HorizontalLayout row = new HorizontalLayout(key, new Span(value));
        row.setSpacing(true);
        row.getStyle().set("padding", "2px 0");
        return row;
    }

    private void openActivityDialog(LocalDate prefilledDate) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("New Activity");
        dlg.setWidth("460px");

        ComboBox<ActivityType> typeField = new ComboBox<>("Type");
        typeField.setItems(ActivityType.values());
        typeField.setValue(ActivityType.MEETING);
        typeField.setWidthFull();

        TextField titleField = new TextField("Title");
        titleField.setWidthFull();
        titleField.setRequired(true);

        TextArea descField = new TextArea("Description");
        descField.setWidthFull();

        DatePicker dueDateField = new DatePicker("Due Date");
        dueDateField.setValue(prefilledDate);
        dueDateField.setWidthFull();

        ComboBox<String> assignedToField = new ComboBox<>("Assigned To");
        assignedToField.setItems(userService.findAll().stream().map(u -> u.username()).toList());
        assignedToField.setWidthFull();

        ComboBox<ActivityPriority> priorityField = new ComboBox<>("Priority");
        priorityField.setItems(ActivityPriority.values());
        priorityField.setValue(ActivityPriority.MEDIUM);
        priorityField.setWidthFull();

        VerticalLayout form = new VerticalLayout(typeField, titleField, descField,
                dueDateField, assignedToField, priorityField);
        form.setPadding(false);
        form.setSpacing(true);
        dlg.add(form);

        Button save = new Button("Create", e -> {
            if (titleField.getValue().isBlank()) {
                titleField.setInvalid(true);
                titleField.setErrorMessage("Title is required");
                return;
            }
            Long assignedToId = assignedToField.getValue() != null
                    ? userService.findAll().stream()
                        .filter(u -> u.username().equals(assignedToField.getValue()))
                        .findFirst().map(u -> u.id()).orElse(null)
                    : null;
            ActivityRequest req = new ActivityRequest(
                    titleField.getValue(),
                    descField.getValue(),
                    typeField.getValue(),
                    ActivityStatus.OPEN,
                    priorityField.getValue(),
                    dueDateField.getValue(),
                    assignedToId,
                    null,
                    null
            );
            try {
                activityService.create(req, securityService.getUsername());
                Notification.show("Activity created", 2000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dlg.close();
                refresh();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dlg.getFooter().add(new Button("Cancel", e -> dlg.close()), save);
        dlg.open();
    }
}
