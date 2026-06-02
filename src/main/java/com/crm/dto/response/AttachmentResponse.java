package com.crm.dto.response;

import com.crm.domain.entity.Attachment;

import java.time.LocalDateTime;

public record AttachmentResponse(
        Long id,
        String filename,
        String contentType,
        long fileSize,
        String uploadedByName,
        LocalDateTime createdAt
) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(
                a.getId(), a.getFilename(), a.getContentType(), a.getFileSize(),
                a.getUploadedBy() != null ? a.getUploadedBy().getUsername() : "unknown",
                a.getCreatedAt()
        );
    }
}
