package com.crm.dto.response;

import com.crm.domain.entity.Opportunity;
import com.crm.domain.enums.OpportunityStage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OpportunityResponse(
        Long id,
        String name,
        OpportunityStage stage,
        BigDecimal amount,
        String currency,
        Integer probability,
        LocalDate closeDate,
        String assignedToName,
        Long accountId,
        String accountName,
        Long contactId,
        String contactName,
        BigDecimal weightedAmount,
        LocalDateTime createdAt
) {
    public static OpportunityResponse from(Opportunity o) {
        BigDecimal weighted = (o.getAmount() != null && o.getProbability() != null)
                ? o.getAmount().multiply(BigDecimal.valueOf(o.getProbability() / 100.0))
                : null;
        return new OpportunityResponse(
                o.getId(),
                o.getName(),
                o.getStage(),
                o.getAmount(),
                o.getCurrency(),
                o.getProbability(),
                o.getCloseDate(),
                o.getAssignedTo() != null ? o.getAssignedTo().getUsername() : null,
                o.getAccount() != null ? o.getAccount().getId() : null,
                o.getAccount() != null ? o.getAccount().getName() : null,
                o.getContact() != null ? o.getContact().getId() : null,
                o.getContact() != null ? (o.getContact().getFirstName() + " " + o.getContact().getLastName()) : null,
                weighted,
                o.getCreatedAt()
        );
    }
}
