package com.crm.billing.service;

import com.crm.billing.dto.LineItemRequest;
import com.crm.billing.entity.DocumentLineItem;
import com.crm.billing.enums.DocumentOwnerType;
import com.crm.billing.repository.DocumentLineItemRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Shared line-item persistence for PaymentRequest and TaxDocument (the "shared line-item grid"
 * the prompt calls for) — both owners replace their full item list on every save. */
@Component
public class DocumentLineItemSupport {

    private final DocumentLineItemRepository lineItemRepository;

    public DocumentLineItemSupport(DocumentLineItemRepository lineItemRepository) {
        this.lineItemRepository = lineItemRepository;
    }

    public List<DocumentLineItem> replaceItems(DocumentOwnerType ownerType, Long ownerId, List<LineItemRequest> requests) {
        lineItemRepository.deleteByOwnerTypeAndOwnerId(ownerType, ownerId);
        List<DocumentLineItem> items = new ArrayList<>();
        int i = 0;
        for (LineItemRequest r : requests) {
            DocumentLineItem item = new DocumentLineItem();
            item.setOwnerType(ownerType);
            item.setOwnerId(ownerId);
            item.setProductOrService(r.productOrService());
            item.setDescription(r.description());
            item.setQuantity(r.quantity() != null ? r.quantity() : BigDecimal.ONE);
            item.setUnitPrice(r.unitPrice() != null ? r.unitPrice() : BigDecimal.ZERO);
            item.setSortOrder(r.sortOrder() != null ? r.sortOrder() : i);
            items.add(item);
            i++;
        }
        return lineItemRepository.saveAll(items);
    }

    public List<DocumentLineItem> find(DocumentOwnerType ownerType, Long ownerId) {
        return lineItemRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(ownerType, ownerId);
    }

    public BigDecimal sumLineTotals(List<DocumentLineItem> items) {
        return items.stream().map(DocumentLineItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Converts persisted line items back into request form, e.g. for recomputing totals or for
     * ConversionService building a TaxDocument draft from a PaymentRequest's existing items. */
    public List<LineItemRequest> toRequests(List<DocumentLineItem> items) {
        return items.stream()
                .map(i -> new LineItemRequest(null, i.getProductOrService(), i.getDescription(),
                        i.getQuantity(), i.getUnitPrice(), i.getSortOrder()))
                .toList();
    }
}
