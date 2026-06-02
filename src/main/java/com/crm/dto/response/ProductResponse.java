package com.crm.dto.response;

import com.crm.domain.entity.Product;
import com.crm.domain.enums.ProductCategory;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        ProductCategory category,
        BigDecimal unitPrice,
        String currency,
        boolean active
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getCategory(), p.getUnitPrice(), p.getCurrency(), p.isActive()
        );
    }
}
