package com.crm.ui.attendance;

import com.crm.service.TranslationService;
import com.crm.timetracking.dto.AttendanceReportRequest;
import com.crm.timetracking.dto.AttendanceReportResponse;
import com.crm.timetracking.enums.AttendanceReportType;
import com.crm.timetracking.exception.AttendanceValidationException;
import com.crm.timetracking.service.AttendanceReportService;
import com.crm.timetracking.util.DurationCalculator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.timepicker.TimePicker;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

public class AttendanceReportEditor extends Dialog {

    private final AttendanceReportService service;
    private final TranslationService      i18n;
    private final AttendanceCalendarView  parent;
    private final Long                    userId;
    private final LocalDate               reportDate;
    private final AttendanceReportResponse editing;

    private final ComboBox<AttendanceReportType> typeCombo   = new ComboBox<>();
    private final TimePicker                     entryPicker = new TimePicker();
    private final TimePicker                     exitPicker  = new TimePicker();
    private final Span                           totalSpan   = new Span();
    private final TextArea                       notesField  = new TextArea();
    private final Checkbox                       equateBox   = new Checkbox();

    private final Button saveBtn   = new Button(VaadinIcon.CHECK.create());
    private final Button cancelBtn = new Button(VaadinIcon.CLOSE.create());
    private final Button deleteBtn = new Button(VaadinIcon.TRASH.create());

    public AttendanceReportEditor(AttendanceReportService service,
                                  TranslationService i18n,
                                  AttendanceCalendarView parent,
                                  Long userId, LocalDate reportDate,
                                  AttendanceReportResponse editing) {
        this.service     = service;
        this.i18n        = i18n;
        this.parent      = parent;
        this.userId      = userId;
        this.reportDate  = reportDate;
        this.editing     = editing;
        build();
        if (editing != null) populate();
    }

    private void build() {
        setHeaderTitle(editing == null
                ? i18n.translate("view.attendanceReportEditor.title.new", reportDate)
                : i18n.translate("view.attendanceReportEditor.title.edit", reportDate));
        setWidth("440px");

        typeCombo.setLabel(i18n.translate("view.attendanceReportEditor.field.reportType"));
        typeCombo.setItems(AttendanceReportType.values());
        typeCombo.setItemLabelGenerator(i18n::translateEnum);
        typeCombo.setValue(AttendanceReportType.PRESENCE);
        typeCombo.setWidthFull();
        typeCombo.addValueChangeListener(e -> onTypeChange(e.getValue()));

        entryPicker.setLabel(i18n.translate("view.attendanceReportEditor.field.entryTime"));
        exitPicker.setLabel(i18n.translate("view.attendanceReportEditor.field.exitTime"));
        entryPicker.setStep(Duration.ofMinutes(15));
        exitPicker.setStep(Duration.ofMinutes(15));
        entryPicker.addValueChangeListener(e -> recomputeTotal());
        exitPicker.addValueChangeListener(e -> recomputeTotal());

        totalSpan.setText(i18n.translate("view.attendanceReportEditor.total.empty"));
        totalSpan.getStyle().set("font-size", "1.3em").set("font-weight", "bold");
        HorizontalLayout totalRow = new HorizontalLayout(
                new Span(i18n.translate("view.attendanceReportEditor.total.label")), totalSpan);
        totalRow.setAlignItems(FlexComponent.Alignment.BASELINE);
        totalRow.setSpacing(true);

        notesField.setLabel(i18n.translate("view.attendanceReportEditor.field.notes"));
        notesField.setWidthFull();
        notesField.setMaxHeight("80px");

        equateBox.setLabel(i18n.translate("view.attendanceReportEditor.equateToStandard"));

        HorizontalLayout timeRow = new HorizontalLayout(entryPicker, exitPicker);
        timeRow.setSpacing(true);
        timeRow.setWidthFull();

        VerticalLayout form = new VerticalLayout(typeCombo, timeRow, totalRow, notesField, equateBox);
        form.setPadding(false);
        form.setSpacing(true);
        add(form);

        saveBtn.setText(i18n.translate("common.save"));
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> doSave());

        cancelBtn.setText(i18n.translate("common.cancel"));
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelBtn.addClickListener(e -> close());

        deleteBtn.setText(i18n.translate("common.delete"));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteBtn.setVisible(editing != null);
        deleteBtn.addClickListener(e -> doDelete());

        getFooter().add(new HorizontalLayout(saveBtn, cancelBtn, deleteBtn));
    }

    private void onTypeChange(AttendanceReportType type) {
        boolean needsClock = (type == AttendanceReportType.PRESENCE);
        entryPicker.setVisible(needsClock);
        exitPicker.setVisible(needsClock);
        if (!needsClock) {
            entryPicker.clear();
            exitPicker.clear();
            totalSpan.setText(i18n.translate("view.attendanceReportEditor.total.empty"));
        }
    }

    private void recomputeTotal() {
        LocalTime e = entryPicker.getValue();
        LocalTime x = exitPicker.getValue();
        totalSpan.setText(e != null && x != null
                ? DurationCalculator.formatMinutes(DurationCalculator.computeMinutes(e, x))
                : i18n.translate("view.attendanceReportEditor.total.empty"));
    }

    private void populate() {
        typeCombo.setValue(editing.reportType());
        onTypeChange(editing.reportType());
        if (editing.entryTime() != null) entryPicker.setValue(editing.entryTime());
        if (editing.exitTime()  != null) exitPicker.setValue(editing.exitTime());
        notesField.setValue(editing.note() != null ? editing.note() : "");
        equateBox.setValue(editing.equateToStandard());
        recomputeTotal();
    }

    private void doSave() {
        AttendanceReportType type = typeCombo.getValue();
        if (type == null) {
            showError(i18n.translate("view.attendanceReportEditor.error.typeRequired"));
            return;
        }
        AttendanceReportRequest req = new AttendanceReportRequest(
                reportDate,
                entryPicker.getValue(),
                exitPicker.getValue(),
                notesField.getValue().isBlank() ? null : notesField.getValue(),
                type,
                equateBox.getValue());
        try {
            if (editing == null) service.createReport(userId, req);
            else                 service.editReport(editing.id(), req);
            close();
            parent.refresh();
            Notification.show(i18n.translate("notification.attendanceReportEditor.saved"),
                    3000, Notification.Position.TOP_CENTER);
        } catch (AttendanceValidationException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void doDelete() {
        ConfirmDialog dlg = new ConfirmDialog(
                i18n.translate("dialog.attendanceReportEditor.delete.title"),
                i18n.translate("dialog.attendanceReportEditor.delete.message"),
                i18n.translate("common.delete"), ev -> {
                    service.deleteReport(editing.id());
                    close();
                    parent.refresh();
                    Notification.show(i18n.translate("notification.attendanceReportEditor.deleted"),
                            3000, Notification.Position.TOP_CENTER);
                },
                i18n.translate("common.cancel"), ev -> {});
        dlg.setConfirmButtonTheme("error primary");
        dlg.open();
    }

    private void showError(String msg) {
        Notification n = Notification.show(msg, 5000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
