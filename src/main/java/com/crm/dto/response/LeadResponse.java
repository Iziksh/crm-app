package com.crm.dto.response;

import com.crm.domain.entity.Lead;
import com.crm.domain.enums.LeadSource;
import com.crm.domain.enums.LeadStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeadResponse(
        Long id,
        String title,
        String firstName,
        String lastName,
        String email,
        String phone,
        String company,
        LeadStatus status,
        LeadSource source,
        BigDecimal estimatedValue,
        String currency,
        LocalDate closeDate,
        String assignedToName,
        Long accountId,
        String accountName,
        Long contactId,
        String contactName,
        String notes,
        LocalDateTime createdAt
) {
    public static LeadResponse from(Lead l) {
        return new LeadResponse(
                l.getId(),
                l.getTitle(),
                l.getFirstName(),
                l.getLastName(),
                l.getEmail(),
                l.getPhone(),
                l.getCompany(),
                l.getStatus(),
                l.getSource(),
                l.getEstimatedValue(),
                l.getCurrency(),
                l.getCloseDate(),
                l.getAssignedTo() != null ? l.getAssignedTo().getUsername() : null,
                l.getAccount() != null ? l.getAccount().getId() : null,
                l.getAccount() != null ? l.getAccount().getName() : null,
                l.getContact() != null ? l.getContact().getId() : null,
                l.getContact() != null ? (l.getContact().getFirstName() + " " + l.getContact().getLastName()) : null,
                l.getNotes(),
                l.getCreatedAt()
        );
    }
}
