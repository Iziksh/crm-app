package com.crm.controller;

import com.crm.domain.entity.Attachment;
import com.crm.dto.response.AttachmentResponse;
import com.crm.service.AttachmentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<AttachmentResponse> upload(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload(file, entityType, entityId, userDetails.getUsername()));
    }

    @GetMapping("/entity")
    public ResponseEntity<List<AttachmentResponse>> listByEntity(
            @RequestParam String entityType, @RequestParam Long entityId) {
        return ResponseEntity.ok(attachmentService.findByEntity(entityType, entityId));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id) {
        Attachment a = attachmentService.getForDownload(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + a.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(a.getContentType()))
                .contentLength(a.getFileSize())
                .body(new ByteArrayResource(a.getData()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
