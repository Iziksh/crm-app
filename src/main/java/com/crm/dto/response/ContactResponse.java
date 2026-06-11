package com.crm.dto.response;

import com.crm.domain.entity.Contact;
import com.crm.domain.enums.ContactStatus;

import java.time.LocalDateTime;

public record ContactResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String jobTitle,
        String department,
        ContactStatus status,
        String notes,
        Long accountId,
        String accountName,
        String workspaceName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ContactResponse from(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getJobTitle(),
                contact.getDepartment(),
                contact.getStatus(),
                contact.getNotes(),
                contact.getAccount() != null ? contact.getAccount().getId() : null,
                contact.getAccount() != null ? contact.getAccount().getName() : null,
                contact.getWorkspace() != null ? contact.getWorkspace().getName() : null,
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}
