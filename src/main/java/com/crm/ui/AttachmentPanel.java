package com.crm.ui;

import com.crm.dto.response.AttachmentResponse;
import com.crm.service.AttachmentService;
import com.crm.service.TranslationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.util.List;

/** Reusable collapsible attachment panel for entity edit dialogs. */
public class AttachmentPanel extends Details {

    private final TranslationService i18n;
    private final AttachmentService attachmentService;
    private final String entityType;
    private Long entityId;
    private final String currentUsername;
    private final VerticalLayout listArea = new VerticalLayout();

    public AttachmentPanel(TranslationService i18n, AttachmentService attachmentService, String entityType,
                           Long entityId, String currentUsername) {
        super(i18n.translate("attachment.title"));
        this.i18n = i18n;
        this.attachmentService = attachmentService;
        this.entityType = entityType;
        this.entityId = entityId;
        this.currentUsername = currentUsername;

        listArea.setPadding(false);
        listArea.setSpacing(false);

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setMaxFiles(1);
        upload.setMaxFileSize(10 * 1024 * 1024);

        upload.addSucceededListener(event -> {
            if (entityId == null) {
                notify(i18n.translate("attachment.saveFirst.upload"), NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                byte[] bytes = buffer.getInputStream().readAllBytes();
                String filename = event.getFileName();
                String mimeType = event.getMIMEType();
                attachmentService.uploadBytes(filename, mimeType, bytes, entityType, entityId, currentUsername);
                refresh();
            } catch (Exception ex) {
                notify(i18n.translate("attachment.upload.failed", ex.getMessage()), NotificationVariant.LUMO_ERROR);
            }
        });

        VerticalLayout content = new VerticalLayout(listArea, upload);
        content.setPadding(false);
        content.setSpacing(true);
        setContent(content);
        refresh();
    }

    /** Call this after the entity is saved so uploads can be linked. */
    public void setEntityId(Long entityId) {
        this.entityId = entityId;
        refresh();
    }

    public void refresh() {
        listArea.removeAll();
        if (entityId == null) {
            listArea.add(new Span(i18n.translate("attachment.saveFirst.enable")));
            return;
        }
        List<AttachmentResponse> attachments = attachmentService.findByEntity(entityType, entityId);
        if (attachments.isEmpty()) {
            listArea.add(new Span(i18n.translate("attachment.empty")));
            return;
        }
        for (AttachmentResponse att : attachments) {
            HorizontalLayout row = new HorizontalLayout();
            row.setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
            row.setWidthFull();

            Div info = new Div();
            info.getStyle().set("flex", "1");
            Span name = new Span(att.filename());
            Span meta = new Span(i18n.translate("attachment.meta",
                    formatSize(att.fileSize()),
                    att.uploadedByName() != null ? att.uploadedByName() : "",
                    att.createdAt() != null ? att.createdAt().toLocalDate().toString() : ""));
            meta.getStyle().set("font-size", "0.8em").set("color", "var(--lumo-secondary-text-color)");
            info.add(name, meta);

            StreamResource downloadRes = new StreamResource(att.filename(), () -> {
                try {
                    com.crm.domain.entity.Attachment a = attachmentService.getForDownload(att.id());
                    return new ByteArrayInputStream(a.getData());
                } catch (Exception e) {
                    return new ByteArrayInputStream(new byte[0]);
                }
            });
            Anchor downloadLink = new Anchor(downloadRes, "");
            downloadLink.getElement().setAttribute("download", true);
            Button downloadBtn = new Button(VaadinIcon.DOWNLOAD.create());
            downloadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            downloadLink.add(downloadBtn);

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                attachmentService.delete(att.id());
                refresh();
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            row.add(info, downloadLink, deleteBtn);
            listArea.add(row);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return i18n.translate("attachment.size.bytes", bytes);
        if (bytes < 1024 * 1024) return i18n.translate("attachment.size.kb", bytes / 1024);
        return i18n.translate("attachment.size.mb", bytes / (1024.0 * 1024));
    }

    private void notify(String msg, NotificationVariant variant) {
        Notification.show(msg, 3000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(variant);
    }
}
