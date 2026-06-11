package com.crm.ui;

import com.crm.repository.UserRepository;
import com.crm.service.TranslationService;
import com.crm.timetracking.entity.Attendance;
import com.crm.timetracking.service.AttendanceService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "attendance-corrections", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class AttendanceCorrectionView extends VerticalLayout implements HasDynamicTitle {

    private static final ZoneId            IL_ZONE = ZoneId.of("Asia/Jerusalem");
    private static final DateTimeFormatter DT_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TranslationService i18n;
    private final AttendanceService attendanceService;
    private final UserRepository    userRepository;
    private final Long              currentManagerId;

    private Grid<Attendance> grid;

    public AttendanceCorrectionView(AttendanceService attendanceService,
                                    UserRepository userRepository,
                                    SecurityService securityService,
                                    TranslationService i18n) {
        this.attendanceService = attendanceService;
        this.userRepository    = userRepository;
        this.i18n = i18n;

        String username = securityService.getUsername();
        this.currentManagerId = userRepository.findByUsername(username)
                .map(u -> u.getId()).orElse(null);

        setSpacing(true);
        setPadding(true);

        add(new H2(i18n.translate("view.attendanceCorrection.title")));
        add(new Paragraph(i18n.translate("view.attendanceCorrection.description")));

        buildGrid();
        refresh();
    }

    @Override
    public String getPageTitle() {
        return i18n.translate("page.attendanceCorrections");
    }

    private void buildGrid() {
        grid = new Grid<>(Attendance.class, false);

        grid.addColumn(a -> resolveUsername(a.getUserId()))
                .setHeader(i18n.translate("view.attendanceCorrection.column.employee"))
                .setWidth("160px").setFlexGrow(0);

        grid.addColumn(a -> a.getStartTime().atZoneSameInstant(IL_ZONE).format(DT_FMT))
                .setHeader(i18n.translate("view.attendanceCorrection.column.entry"))
                .setWidth("155px").setFlexGrow(0);

        grid.addColumn(a -> a.getEndTime() != null
                        ? a.getEndTime().atZoneSameInstant(IL_ZONE).format(DT_FMT)
                        : i18n.translate("common.emDash"))
                .setHeader(i18n.translate("view.attendanceCorrection.column.exit"))
                .setWidth("155px").setFlexGrow(0);

        grid.addColumn(a -> {
            if (a.getDurationSeconds() == null) return i18n.translate("common.emDash");
            long h = a.getDurationSeconds() / 3600;
            long m = (a.getDurationSeconds() % 3600) / 60;
            return i18n.translate("common.duration.hoursMinutes", h, String.format("%02d", m));
        }).setHeader(i18n.translate("view.attendanceCorrection.column.duration"))
                .setWidth("100px").setFlexGrow(0);

        grid.addColumn(a -> a.getNote() != null ? a.getNote() : "")
                .setHeader(i18n.translate("view.attendanceCorrection.column.reason")).setFlexGrow(1);

        grid.addComponentColumn(a -> {
            Button approveBtn = new Button(i18n.translate("view.attendanceCorrection.approve"),
                    e -> doApprove(a));
            approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);

            Button rejectBtn = new Button(i18n.translate("view.attendanceCorrection.reject"),
                    e -> openRejectDialog(a));
            rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL,
                                       ButtonVariant.LUMO_TERTIARY);

            HorizontalLayout actions = new HorizontalLayout(approveBtn, rejectBtn);
            actions.setSpacing(true);
            return actions;
        }).setHeader(i18n.translate("view.attendanceCorrection.column.actions"))
                .setWidth("200px").setFlexGrow(0);

        grid.setWidthFull();
        grid.setHeight("400px");
        add(grid);
    }

    private void refresh() {
        List<Attendance> pending = attendanceService.getPendingApprovals();
        grid.setItems(pending);

        if (pending.isEmpty()) {
            Span empty = new Span(i18n.translate("view.attendanceCorrection.noPending"));
            empty.getStyle().set("color", "#888").set("font-style", "italic");
        }
    }

    private void doApprove(Attendance a) {
        ConfirmDialog confirm = new ConfirmDialog(
            i18n.translate("dialog.attendanceCorrection.approve.title"),
            i18n.translate("dialog.attendanceCorrection.approve.message",
                resolveUsername(a.getUserId()),
                a.getStartTime().atZoneSameInstant(IL_ZONE).format(DT_FMT)),
            i18n.translate("view.attendanceCorrection.approve"), ev -> {
                try {
                    attendanceService.approve(a.getId(), currentManagerId);
                    refresh();
                    Notification n = Notification.show(
                            i18n.translate("notification.attendanceCorrection.approved"),
                            3000, Notification.Position.BOTTOM_CENTER);
                    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            },
            i18n.translate("common.cancel"), ev -> {}
        );
        confirm.setConfirmButtonTheme("success primary");
        confirm.open();
    }

    private void openRejectDialog(Attendance a) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(i18n.translate("dialog.attendanceCorrection.reject.title",
                resolveUsername(a.getUserId())));
        dialog.setWidth("400px");

        TextArea reasonArea = new TextArea(i18n.translate("view.attendanceCorrection.rejectReason.label"));
        reasonArea.setPlaceholder(i18n.translate("view.attendanceCorrection.rejectReason.placeholder"));
        reasonArea.setWidthFull();
        reasonArea.setRequired(true);

        Button confirmBtn = new Button(i18n.translate("view.attendanceCorrection.reject"), ev -> {
            if (reasonArea.getValue().isBlank()) {
                reasonArea.setErrorMessage(i18n.translate("validation.rejectReasonRequired"));
                reasonArea.setInvalid(true);
                return;
            }
            try {
                attendanceService.reject(a.getId(), currentManagerId, reasonArea.getValue());
                dialog.close();
                refresh();
                Notification n = Notification.show(
                        i18n.translate("notification.attendanceCorrection.rejected"),
                        3000, Notification.Position.BOTTOM_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button(i18n.translate("common.cancel"), ev -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.add(new VerticalLayout(reasonArea) {{ setPadding(false); }});
        dialog.getFooter().add(confirmBtn, cancelBtn);
        dialog.open();
    }

    private String resolveUsername(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getUsername())
                .orElse(i18n.translate("view.attendanceCorrection.unknownUser", userId));
    }
}
