package com.crm.dto.response;

import com.crm.domain.entity.Contract;
import com.crm.domain.enums.ContractStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractResponse(
        Long id,
        String contractNumber,
        String title,
        ContractStatus status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalValue,
        String currency,
        Long accountId,
        String accountName,
        Long contactId,
        String contactName,
        Long salesOrderId,
        String orderNumber,
        LocalDateTime createdAt
) {
    public static ContractResponse from(Contract c) {
        return new ContractResponse(
                c.getId(),
                c.getContractNumber(),
                c.getTitle(),
                c.getStatus(),
                c.getStartDate(),
                c.getEndDate(),
                c.getTotalValue(),
                c.getCurrency(),
                c.getAccount() != null ? c.getAccount().getId() : null,
                c.getAccount() != null ? c.getAccount().getName() : null,
                c.getContact() != null ? c.getContact().getId() : null,
                c.getContact() != null ? (c.getContact().getFirstName() + " " + c.getContact().getLastName()) : null,
                c.getSalesOrder() != null ? c.getSalesOrder().getId() : null,
                c.getSalesOrder() != null ? c.getSalesOrder().getOrderNumber() : null,
                c.getCreatedAt()
        );
    }
}
