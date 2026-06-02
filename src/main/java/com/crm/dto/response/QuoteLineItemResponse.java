package com.crm.dto.response;

import com.crm.domain.entity.QuoteLineItem;

import java.math.BigDecimal;

public record QuoteLineItemResponse(
        Long id,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPct,
        BigDecimal lineTotal,
        Integer sortOrder
) {
    public static QuoteLineItemResponse from(QuoteLineItem item) {
        return new QuoteLineItemResponse(
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
