package com.crm.dto.response;

import com.crm.domain.entity.SalesOrderLineItem;

import java.math.BigDecimal;

public record SalesOrderLineItemResponse(
        Long id,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPct,
        BigDecimal lineTotal,
        Integer sortOrder
) {
    public static SalesOrderLineItemResponse from(SalesOrderLineItem item) {
        return new SalesOrderLineItemResponse(
                item.getId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getDiscountPct(),
                item.getLineTotal(),
                item.getSortOrder()
        );
    }
}
