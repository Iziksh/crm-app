package com.crm.service;

import com.crm.domain.entity.Attachment;
import com.crm.dto.response.AttachmentResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AttachmentRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock AttachmentRepository attachmentRepository;
    @Mock UserRepository userRepository;

    @InjectMocks AttachmentService attachmentService;

    @Test
    void uploadBytes_savesAttachment_withCorrectFields() {
        byte[] data = "hello".getBytes();
        Attachment saved = new Attachment();
        saved.setFilename("report.pdf");
        saved.setContentType("application/pdf");
        saved.setFileSize(data.length);
        when(attachmentRepository.save(any())).thenReturn(saved);

        AttachmentResponse response = attachmentService.uploadBytes(
                "report.pdf", "application/pdf", data, "ACCOUNT", 1L, "admin");

        assertThat(response.filename()).isEqualTo("report.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");

        ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertThat(captor.getValue().getFileSize()).isEqualTo(data.length);
        assertThat(captor.getValue().getEntityType()).isEqualTo("ACCOUNT");
        assertThat(captor.getValue().getEntityId()).isEqualTo(1L);
    }

    @Test
    void uploadBytes_defaultsContentType_whenNull() {
        Attachment saved = new Attachment();
        saved.setFilename("file.bin");
        saved.setContentType("application/octet-stream");
        when(attachmentRepository.save(any())).thenReturn(saved);

        attachmentService.uploadBytes("file.bin", null, new byte[0], "LEAD", 2L, "admin");

        ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertThat(captor.getValue().getContentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void findByEntity_delegatesToRepository() {
        when(attachmentRepository.findByEntityTypeAndEntityId("ACCOUNT", 1L)).thenReturn(List.of());

        List<AttachmentResponse> result = attachmentService.findByEntity("ACCOUNT", 1L);

        assertThat(result).isEmpty();
        verify(attachmentRepository).findByEntityTypeAndEntityId("ACCOUNT", 1L);
    }

    @Test
    void getForDownload_returnsAttachment_whenFound() {
        Attachment attachment = new Attachment();
        attachment.setFilename("doc.pdf");
        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));

        Attachment result = attachmentService.getForDownload(1L);

        assertThat(result.getFilename()).isEqualTo("doc.pdf");
    }

    @Test
    void getForDownload_throwsNotFound_whenMissing() {
        when(attachmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attachmentService.getForDownload(99L));
    }

    @Test
    void delete_removesAttachment() {
        Attachment attachment = new Attachment();
        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));

        attachmentService.delete(1L);

        verify(attachmentRepository).delete(attachment);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(attachmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> attachmentService.delete(99L));
    }
}
