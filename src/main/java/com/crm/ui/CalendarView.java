package com.crm.ui;

import com.crm.domain.enums.ActivityPriority;
import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ActivityType;
import com.crm.dto.request.ActivityRequest;
import com.crm.dto.response.ActivityResponse;
import com.crm.service.ActivityService;
import com.crm.service.TranslationService;
import com.crm.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.PermitAll;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "calendar", layout = MainLayout.class)
@PermitAll
public class CalendarView extends VerticalLayout implements HasDynamicTitle {

    private final TranslationService i18n;
    private final ActivityService activityService;
    private final UserService userService;
    private final SecurityService securityService;

    private YearMonth currentMonth = YearMonth.now();
    private final com.vaadin.flow.component.html.H3 monthLabel = new com.vaadin.flow.component.html.H3();
    private final Div calendarGrid = new Div();

    public CalendarView(TranslationService i18n, ActivityService activityService,
                        UserService userService,
                        SecurityService securityService) {
        this.i18n = i18n;
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

        Button newBtn = new Button(i18n.translate("view.calendar.button.new"), VaadinIcon.PLUS.create(),
                e -> openActivityDialog(LocalDate.now()));
        newBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        StreamResource icalResource = new StreamResource("crm-calendar.ics", () ->
                new ByteArrayInputStream(activityService.buildIcal().getBytes(StandardCharsets.UTF_8)));
        Anchor icalAnchor = new Anchor(icalResource, "");
        icalAnchor.getElement().setAttribute("download", true);
        Button icalBtn = new Button(i18n.translate("view.calendar.button.downloadIcs"), VaadinIcon.CALENDAR.create());
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

    @Override
    public String getPageTitle() {
        return i18n.translate("pageTitle.calendar");
    }

    private void refresh() {
        monthLabel.setText(
                currentMonth.getMonth().getDisplayName(TextStyle.FULL, i18n.getCurrentLocale())
                        + " " + currentMonth.getYear());

        LocalDate first = currentMonth.atDay(1);
        LocalDate last = currentMonth.atEndOfMonth();

        List<ActivityResponse> activities = activityService.findForCalendar(first, last);
        Map<LocalDate, List<ActivityResponse>> byDate = activities.stream()
                .filter(a -> a.dueDate() != null)
                .collect(Collectors.groupingBy(ActivityResponse::dueDate));

        calendarGrid.removeAll();

        for (int i = 1; i <= 7; i++) {
            DayOfWeek dow = DayOfWeek.of(i);
            String day = dow.getDisplayName(TextStyle.SHORT, i18n.getCurrentLocale());
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

        int offset = first.getDayOfWeek().getValue() - 1;
        for (int i = 0; i < offset; i++) {
            calendarGrid.add(emptyCell());
        }

        LocalDate today = LocalDate.now();
        for (int d = 1; d <= last.getDayOfMonth(); d++) {
            LocalDate date = currentMonth.atDay(d);
            calendarGrid.add(buildDayCell(date, byDate.getOrDefault(date, List.of()), today));
        }

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
            chip.setTitle(a.title() + (a.assignedToName() != null
                    ? " " + i18n.translate("common.emDash") + " " + a.assignedToName() : ""));
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
        dlg.setHeaderTitle(i18n.translate("view.calendar.dialog.detailHeader",
                i18n.translateEnum(a.type()), a.title()));
        dlg.setWidth("420px");

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        if (a.description() != null && !a.description().isBlank()) {
            Paragraph desc = new Paragraph(a.description());
            desc.getStyle().set("color", "#555").set("white-space", "pre-wrap");
            body.add(desc);
        }
        String emDash = i18n.translate("common.emDash");
        body.add(detailRow(i18n.translate("view.calendar.detail.status"),
                a.status() != null ? i18n.translateEnum(a.status()) : emDash));
        body.add(detailRow(i18n.translate("view.calendar.detail.priority"),
                a.priority() != null ? i18n.translateEnum(a.priority()) : emDash));
        body.add(detailRow(i18n.translate("view.calendar.detail.dueDate"),
                a.dueDate() != null ? a.dueDate().toString() : emDash));
        body.add(detailRow(i18n.translate("view.calendar.detail.assigned"),
                a.assignedToName() != null ? a.assignedToName() : emDash));
        if (a.accountName() != null) body.add(detailRow(i18n.translate("common.column.account"), a.accountName()));
        if (a.contactName() != null) body.add(detailRow(i18n.translate("common.column.contact"), a.contactName()));

        dlg.add(body);
        dlg.getFooter().add(new Button(i18n.translate("dialog.close"), e -> dlg.close()));
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
        dlg.setHeaderTitle(i18n.translate("view.calendar.dialog.new"));
        dlg.setWidth("460px");

        ComboBox<ActivityType> typeField = new ComboBox<>(i18n.translate("common.column.type"));
        typeField.setItems(ActivityType.values());
        typeField.setItemLabelGenerator(i18n::translateEnum);
        typeField.setValue(ActivityType.MEETING);
        typeField.setWidthFull();

        TextField titleField = new TextField(i18n.translate("view.activities.column.title"));
        titleField.setWidthFull();
        titleField.setRequired(true);

        TextArea descField = new TextArea(i18n.translate("view.activities.field.description"));
        descField.setWidthFull();

        DatePicker dueDateField = new DatePicker(i18n.translate("view.activities.column.dueDate"));
        dueDateField.setValue(prefilledDate);
        dueDateField.setWidthFull();

        ComboBox<String> assignedToField = new ComboBox<>(i18n.translate("view.activities.column.assignedTo"));
        assignedToField.setItems(userService.findAll().stream().map(u -> u.username()).toList());
        assignedToField.setWidthFull();

        ComboBox<ActivityPriority> priorityField = new ComboBox<>(i18n.translate("view.activities.column.priority"));
        priorityField.setItems(ActivityPriority.values());
        priorityField.setItemLabelGenerator(i18n::translateEnum);
        priorityField.setValue(ActivityPriority.MEDIUM);
        priorityField.setWidthFull();

        VerticalLayout form = new VerticalLayout(typeField, titleField, descField,
                dueDateField, assignedToField, priorityField);
        form.setPadding(false);
        form.setSpacing(true);
        dlg.add(form);

        Button save = new Button(i18n.translate("view.calendar.dialog.create"), e -> {
            if (titleField.getValue().isBlank()) {
                titleField.setInvalid(true);
                titleField.setErrorMessage(i18n.translate("view.activities.validation.titleRequired"));
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
                notify(i18n.translate("notification.activity.created"), false);
                dlg.close();
                refresh();
            } catch (Exception ex) {
                notify(i18n.translate("view.calendar.error", ex.getMessage()), true);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dlg.getFooter().add(new Button(i18n.translate("dialog.cancel"), e -> dlg.close()), save);
        dlg.open();
    }

    private void notify(String msg, boolean error) {
        Notification n = Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER);
        n.addThemeVariants(error ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
    }
}
