package com.crm.billing.dto;

import com.crm.billing.entity.DocumentLineItem;

import java.math.BigDecimal;

public record LineItemResponse(
        Long id,
        String productOrService,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        Integer sortOrder
) {
    public static LineItemResponse from(DocumentLineItem item) {
        return new LineItemResponse(item.getId(), item.getProductOrService(), item.getDescription(),
                item.getQuantity(), item.getUnitPrice(), item.getLineTotal(), item.getSortOrder());
    }
}
