package com.crm.service;

import com.crm.domain.entity.Attachment;
import com.crm.dto.response.AttachmentResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AttachmentRepository;
import com.crm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;

    public AttachmentService(AttachmentRepository attachmentRepository, UserRepository userRepository) {
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
    }

    public AttachmentResponse upload(MultipartFile file, String entityType, Long entityId,
                                     String uploadedByUsername) throws IOException {
        return uploadBytes(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file",
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getBytes(), entityType, entityId, uploadedByUsername);
    }

    public AttachmentResponse uploadBytes(String filename, String contentType, byte[] data,
                                          String entityType, Long entityId, String uploadedByUsername) {
        Attachment attachment = new Attachment();
        attachment.setFilename(filename);
        attachment.setContentType(contentType != null ? contentType : "application/octet-stream");
        attachment.setFileSize(data.length);
        attachment.setData(data);
        attachment.setEntityType(entityType);
        attachment.setEntityId(entityId);
        userRepository.findByUsername(uploadedByUsername).ifPresent(attachment::setUploadedBy);
        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> findByEntity(String entityType, Long entityId) {
        return attachmentRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .stream().map(AttachmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Attachment getForDownload(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", id));
    }

    public void delete(Long id) {
        attachmentRepository.delete(attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", id)));
    }
}
