package com.crm.ui;

import com.crm.repository.UserRepository;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "attendance-corrections", layout = MainLayout.class)
@PageTitle("Attendance Corrections | CRM")
@RolesAllowed("ADMIN")
public class AttendanceCorrectionView extends VerticalLayout {

    private static final ZoneId              IL_ZONE  = ZoneId.of("Asia/Jerusalem");
    private static final DateTimeFormatter   DT_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AttendanceService attendanceService;
    private final UserRepository    userRepository;
    private final Long              currentManagerId;

    private Grid<Attendance> grid;

    public AttendanceCorrectionView(AttendanceService attendanceService,
                                    UserRepository userRepository,
                                    SecurityService securityService) {
        this.attendanceService = attendanceService;
        this.userRepository    = userRepository;

        String username = securityService.getUsername();
        this.currentManagerId = userRepository.findByUsername(username)
                .map(u -> u.getId()).orElse(null);

        setSpacing(true);
        setPadding(true);

        add(new H2("Attendance Corrections — Pending Approvals"));
        add(new Paragraph(
            "Review manually-submitted attendance corrections from employees who forgot " +
            "to clock in or out. Approve to count the session toward their monthly hours; " +
            "reject to dismiss it."));

        buildGrid();
        refresh();
    }

    private void buildGrid() {
        grid = new Grid<>(Attendance.class, false);

        grid.addColumn(a -> resolveUsername(a.getUserId()))
                .setHeader("Employee").setWidth("160px").setFlexGrow(0);

        grid.addColumn(a -> a.getStartTime().atZoneSameInstant(IL_ZONE).format(DT_FMT))
                .setHeader("Entry").setWidth("155px").setFlexGrow(0);

        grid.addColumn(a -> a.getEndTime() != null
                        ? a.getEndTime().atZoneSameInstant(IL_ZONE).format(DT_FMT) : "—")
                .setHeader("Exit").setWidth("155px").setFlexGrow(0);

        grid.addColumn(a -> {
            if (a.getDurationSeconds() == null) return "—";
            long h = a.getDurationSeconds() / 3600;
            long m = (a.getDurationSeconds() % 3600) / 60;
            return String.format("%dh %02dm", h, m);
        }).setHeader("Duration").setWidth("100px").setFlexGrow(0);

        grid.addColumn(a -> a.getNote() != null ? a.getNote() : "")
                .setHeader("Reason").setFlexGrow(1);

        grid.addComponentColumn(a -> {
            Button approveBtn = new Button("Approve", e -> doApprove(a));
            approveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);

            Button rejectBtn = new Button("Reject", e -> openRejectDialog(a));
            rejectBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL,
                                       ButtonVariant.LUMO_TERTIARY);

            HorizontalLayout actions = new HorizontalLayout(approveBtn, rejectBtn);
            actions.setSpacing(true);
            return actions;
        }).setHeader("Actions").setWidth("200px").setFlexGrow(0);

        grid.setWidthFull();
        grid.setHeight("400px");
        add(grid);
    }

    private void refresh() {
        List<Attendance> pending = attendanceService.getPendingApprovals();
        grid.setItems(pending);

        if (pending.isEmpty()) {
            Span empty = new Span("No pending corrections.");
            empty.getStyle().set("color", "#888").set("font-style", "italic");
            // Show inline only when first loaded empty — grid handles empty state visually
        }
    }

    // ── APPROVE ───────────────────────────────────────────────────────────────

    private void doApprove(Attendance a) {
        ConfirmDialog confirm = new ConfirmDialog(
            "Approve correction",
            "Approve the correction for " + resolveUsername(a.getUserId()) + " on "
                + a.getStartTime().atZoneSameInstant(IL_ZONE).format(DT_FMT) + "?",
            "Approve", ev -> {
                try {
                    attendanceService.approve(a.getId(), currentManagerId);
                    refresh();
                    Notification n = Notification.show("Correction approved.",
                            3000, Notification.Position.BOTTOM_CENTER);
                    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            },
            "Cancel", ev -> {}
        );
        confirm.setConfirmButtonTheme("success primary");
        confirm.open();
    }

    // ── REJECT ────────────────────────────────────────────────────────────────

    private void openRejectDialog(Attendance a) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Reject correction — " + resolveUsername(a.getUserId()));
        dialog.setWidth("400px");

        TextArea reasonArea = new TextArea("Reason for rejection");
        reasonArea.setPlaceholder("e.g. Times not matching building access logs");
        reasonArea.setWidthFull();
        reasonArea.setRequired(true);

        Button confirmBtn = new Button("Reject", ev -> {
            if (reasonArea.getValue().isBlank()) {
                reasonArea.setErrorMessage("A reason is required.");
                reasonArea.setInvalid(true);
                return;
            }
            try {
                attendanceService.reject(a.getId(), currentManagerId, reasonArea.getValue());
                dialog.close();
                refresh();
                Notification n = Notification.show("Correction rejected.",
                        3000, Notification.Position.BOTTOM_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", ev -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.add(new VerticalLayout(reasonArea) {{ setPadding(false); }});
        dialog.getFooter().add(confirmBtn, cancelBtn);
        dialog.open();
    }

    private String resolveUsername(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getUsername()).orElse("user-" + userId);
    }
}