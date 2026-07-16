package com.crm.billing.dto;

import com.crm.billing.enums.AllocationStatus;

public record IssueResultResponse(
        Long documentId,
        String number,
        AllocationStatus allocationStatus,
        String allocationNumber,
        String message
) {}
