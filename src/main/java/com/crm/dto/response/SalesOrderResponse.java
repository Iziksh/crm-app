package com.crm.dto.response;

import com.crm.domain.entity.SalesOrder;
import com.crm.domain.enums.SalesOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SalesOrderResponse(
        Long id,
        String orderNumber,
        SalesOrderStatus status,
        LocalDate orderDate,
        LocalDate deliveryDate,
        BigDecimal totalAmount,
        String currency,
        Long accountId,
        String accountName,
        Long quoteId,
        String quoteNumber,
        List<SalesOrderLineItemResponse> lineItems,
        LocalDateTime createdAt
) {
    public static SalesOrderResponse from(SalesOrder so) {
        return new SalesOrderResponse(
                so.getId(),
                so.getOrderNumber(),
                so.getStatus(),
                so.getOrderDate(),
                so.getDeliveryDate(),
                so.getTotalAmount(),
                so.getCurrency(),
                so.getAccount() != null ? so.getAccount().getId() : null,
                so.getAccount() != null ? so.getAccount().getName() : null,
                so.getQuote() != null ? so.getQuote().getId() : null,
                so.getQuote() != null ? so.getQuote().getQuoteNumber() : null,
                so.getLineItems().stream().map(SalesOrderLineItemResponse::from).toList(),
                so.getCreatedAt()
        );
    }
}
